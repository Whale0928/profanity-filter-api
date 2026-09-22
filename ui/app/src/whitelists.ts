import { API_BASE_URL, readData } from "./auth";

/** 서버가 받는 상한입니다. 서버가 최종 검증하고, 화면은 미리 알려 주는 용도로만 씁니다. */
export const MAX_GROUPS = 10;
export const MAX_WORDS = 200;
export const MAX_WORD_LENGTH = 80;
export const MAX_NAME_LENGTH = 60;
export const MAX_GROUPS_PER_REQUEST = 5;

export type WhitelistView = {
  createdAt: string;
  id: string;
  name: string;
  updatedAt: string;
  wordCount: number;
  words: string[];
};

export type WhitelistInput = { name: string; words: string[] };

async function dashboardRequest<T>(accessToken: string, path = "", init?: RequestInit) {
  const response = await fetch(`${API_BASE_URL}/api/v1/dashboard/whitelists${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...init?.headers,
    },
  });
  return readData<T>(response);
}

export function listWhitelists(accessToken: string) {
  return dashboardRequest<WhitelistView[]>(accessToken);
}

export function createWhitelist(accessToken: string, input: WhitelistInput) {
  return dashboardRequest<WhitelistView>(accessToken, "", { body: JSON.stringify(input), method: "POST" });
}

export function updateWhitelist(accessToken: string, id: string, input: WhitelistInput) {
  return dashboardRequest<WhitelistView>(accessToken, `/${id}`, { body: JSON.stringify(input), method: "PUT" });
}

export function deleteWhitelist(accessToken: string, id: string) {
  return dashboardRequest<{ id: string }>(accessToken, `/${id}`, { method: "DELETE" });
}

/**
 * 서버와 같은 기준으로 단어를 비교하는 키입니다. 한글, 영문, 공백만 남기고 소문자로 맞춥니다.
 * 화면에서 같은 단어를 두 번 넣지 않게 하고, 등록해도 효과가 없는 단어를 미리 알려 주는 데 씁니다.
 */
export function comparisonKey(word: string) {
  return word.replace(/[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z\s]/g, "").trim().toLowerCase();
}

export function formatDate(value: string) {
  return value.slice(0, 10).replace(/-/g, ".");
}

/** 필터 요청 예시 본문입니다. */
export function requestExample(ids: string[]) {
  return JSON.stringify({ text: "오늘 보스 죽여버리자", mode: "FILTER", whitelistIds: ids }, null, 2);
}
