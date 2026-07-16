import { API_BASE_URL, readData } from "./auth";

export type ApiKeyStatus = "ACTIVE" | "EXPIRED";

export type ApiKeyView = {
  email: string;
  expiredAt: string | null;
  id: string;
  issuedAt: string;
  issuerInfo: string;
  keyHint: string;
  name: string;
  note: string | null;
  permissions: string[];
  requestCount: number;
  status: ApiKeyStatus;
};

export type IssuedApiKey = {
  apiKey: string;
  key: ApiKeyView;
};

export type CreateApiKeyInput = {
  issuerInfo: string;
  name: string;
  note: string;
};

async function dashboardRequest<T>(accessToken: string, path = "", init?: RequestInit) {
  const response = await fetch(`${API_BASE_URL}/api/v1/dashboard/keys${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...init?.headers,
    },
  });
  return readData<T>(response);
}

export function listApiKeys(accessToken: string) {
  return dashboardRequest<ApiKeyView[]>(accessToken);
}

export function issueApiKey(accessToken: string, input: CreateApiKeyInput) {
  return dashboardRequest<IssuedApiKey>(accessToken, "", {
    body: JSON.stringify(input),
    method: "POST",
  });
}

export function reissueApiKey(accessToken: string, apiKeyId: string) {
  return dashboardRequest<IssuedApiKey>(accessToken, `/${apiKeyId}/reissue`, {
    method: "POST",
  });
}

export function expireApiKey(accessToken: string, apiKeyId: string) {
  return dashboardRequest<ApiKeyView>(accessToken, `/${apiKeyId}`, { method: "DELETE" });
}
