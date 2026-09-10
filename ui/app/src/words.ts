import { adminRequest, buildQuery, type AdminPageData } from "./adminClient";

export type WordUsed = "Y" | "N";
export type WordSource = "BASE" | "ADMIN" | "REQUEST" | "UNKNOWN";
export const WORD_SOURCE_LABELS: Record<WordSource, string> = {
  ADMIN: "관리자 등록",
  BASE: "기본 사전",
  REQUEST: "사용자 요청",
  UNKNOWN: "출처 불명",
};

export type WordView = {
  createdAt: string | null;
  id: number;
  isUsed: WordUsed;
  source: WordSource;
  updatedAt: string | null;
  word: string;
};

export function listWords(accessToken: string, query: string, isUsed: WordUsed | "", page: number, signal?: AbortSignal) {
  const params = buildQuery({ isUsed: isUsed || undefined, page, query: query || undefined });
  return adminRequest<AdminPageData<WordView>>(accessToken, `/api/v1/admin/words?${params}`, { signal });
}

export function createWord(accessToken: string, word: string) {
  return adminRequest<WordView>(accessToken, "/api/v1/admin/words", { body: JSON.stringify({ word }), method: "POST" });
}

export function updateWord(accessToken: string, id: number, word: string, isUsed: WordUsed) {
  return adminRequest<WordView>(accessToken, `/api/v1/admin/words/${id}`, {
    body: JSON.stringify({ isUsed, word }),
    method: "PUT",
  });
}
