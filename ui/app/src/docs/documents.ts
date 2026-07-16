import { useEffect, useState } from "react";

import type { OpenApiDocument, RemoteDocumentState } from "./types";

export const OVERVIEW_URL = "https://api.kr-filter.com/overview.md";
export const OPENAPI_URL = "https://api.kr-filter.com/openapi.json";

type CacheEntry<T> = {
  error?: unknown;
  promise: Promise<T>;
  status: "pending" | "fulfilled" | "rejected";
  value?: T;
};

const cache = new Map<string, CacheEntry<unknown>>();

function loadCached<T>(key: string, loader: () => Promise<T>) {
  const cached = cache.get(key) as CacheEntry<T> | undefined;
  if (cached) return cached;

  const entry = { status: "pending" } as CacheEntry<T>;
  entry.promise = loader()
    .then((value) => {
      entry.status = "fulfilled";
      entry.value = value;
      return value;
    })
    .catch((error: unknown) => {
      entry.status = "rejected";
      entry.error = error;
      throw error;
    });
  cache.set(key, entry as CacheEntry<unknown>);
  return entry;
}

async function fetchText(url: string) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.text();
}

async function fetchJson(url: string) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json() as Promise<OpenApiDocument>;
}

function overviewEntry() {
  return loadCached(`text:${OVERVIEW_URL}`, () => fetchText(OVERVIEW_URL));
}

function openApiEntry() {
  return loadCached(`json:${OPENAPI_URL}`, () => fetchJson(OPENAPI_URL));
}

export function preloadDocsDocuments() {
  return Promise.allSettled([overviewEntry().promise, openApiEntry().promise]);
}

function initialState<T>(entry: CacheEntry<T>): RemoteDocumentState<T> {
  if (entry.status === "fulfilled") return { data: entry.value as T, error: "", loading: false };
  if (entry.status === "rejected") return { data: null, error: "문서를 불러오지 못했습니다.", loading: false };
  return { data: null, error: "", loading: true };
}

function useCachedDocument<T>(entryFactory: () => CacheEntry<T>) {
  const [state, setState] = useState<RemoteDocumentState<T>>(() => initialState(entryFactory()));

  useEffect(() => {
    let active = true;
    const entry = entryFactory();
    entry.promise
      .then((data) => active && setState({ data, error: "", loading: false }))
      .catch(() => active && setState({ data: null, error: "문서를 불러오지 못했습니다.", loading: false }));
    return () => { active = false; };
  }, [entryFactory]);

  return state;
}

export function useOverviewDocument() {
  return useCachedDocument(overviewEntry);
}

export function useOpenApiDocument() {
  return useCachedDocument(openApiEntry);
}

void preloadDocsDocuments();
