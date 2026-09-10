import { API_BASE_URL, readData } from "./auth";
import { adminRequest, buildQuery } from "./adminClient";

export const NEWS_CATEGORIES = {
  NOTICE: "공지",
  CHANGELOG: "변경 내역",
  MAINTENANCE: "점검 안내",
  ISSUE: "이슈",
} as const;
export type NewsCategory = keyof typeof NEWS_CATEGORIES;
export type NewsStatus = "DRAFT" | "PUBLISHED";
export type NewsSummary = {
  id: number;
  category: NewsCategory;
  title: string;
  status: NewsStatus;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
};
export type NewsPost = NewsSummary & { content: string };
export type NewsPageData = { items: NewsSummary[]; page: number; hasNext: boolean };
export type AdminNewsPageData = { items: NewsPost[]; page: number; hasNext: boolean };
export type NewsInput = { category: NewsCategory; content: string; status: NewsStatus; title: string };

export async function listNews(category: NewsCategory | "", query: string, page: number, signal?: AbortSignal) {
  const params = buildQuery({ category: category || undefined, page, query: query || undefined });
  return readData<NewsPageData>(await fetch(`${API_BASE_URL}/api/v1/news?${params}`, { signal }));
}

export async function getNews(id: number | string, signal?: AbortSignal) {
  return readData<NewsPost>(await fetch(`${API_BASE_URL}/api/v1/news/${encodeURIComponent(id)}`, { signal }));
}

export function listAdminNews(
  accessToken: string,
  status: NewsStatus | "",
  category: NewsCategory | "",
  query: string,
  page: number,
  signal?: AbortSignal,
) {
  const params = buildQuery({ category: category || undefined, page, query: query || undefined, status: status || undefined });
  return adminRequest<AdminNewsPageData>(accessToken, `/api/v1/admin/news?${params}`, { signal });
}

export function createNews(accessToken: string, input: NewsInput) {
  return adminRequest<NewsPost>(accessToken, "/api/v1/admin/news", { body: JSON.stringify(input), method: "POST" });
}

export function updateNews(accessToken: string, id: number | string, input: NewsInput) {
  return adminRequest<NewsPost>(accessToken, `/api/v1/admin/news/${encodeURIComponent(id)}`, { body: JSON.stringify(input), method: "PUT" });
}

export function deleteNews(accessToken: string, id: number | string) {
  return adminRequest<boolean>(accessToken, `/api/v1/admin/news/${encodeURIComponent(id)}`, { method: "DELETE" });
}
