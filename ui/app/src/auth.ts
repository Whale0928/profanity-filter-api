export type LoginUser = {
  avatarUrl: string | null;
  displayName: string;
  email: string;
  id: string;
};

type LoginTokenData = {
  accessToken: string;
  expiresIn: number;
  tokenType: string;
  user: LoginUser;
};

type ApiResponse<T> = {
  data: T | null;
  status?: {
    description?: string;
  };
};

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "https://api.kr-filter.com").replace(/\/$/, "");

async function readData<T>(response: Response): Promise<T> {
  const body = await response.json() as ApiResponse<T>;
  if (!response.ok || !body.data) throw new Error(body.status?.description ?? "로그인 요청을 완료하지 못했습니다.");
  return body.data;
}

async function verifyUser(accessToken: string) {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/me`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return readData<LoginUser>(response);
}

async function completeLogin(response: Response) {
  const token = await readData<LoginTokenData>(response);
  const user = await verifyUser(token.accessToken);
  return { accessToken: token.accessToken, user };
}

export async function exchangeLoginCode(code: string) {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/exchange`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ code }),
  });
  return completeLogin(response);
}

export async function restoreLoginSession() {
  const csrfResponse = await fetch(`${API_BASE_URL}/api/v1/auth/csrf`, { credentials: "include" });
  const csrf = await readData<{ headerName: string; token: string }>(csrfResponse);
  const refreshResponse = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
    method: "POST",
    credentials: "include",
    headers: { [csrf.headerName]: csrf.token },
  });
  return completeLogin(refreshResponse);
}

export function startSocialLogin(provider: "github" | "google") {
  window.location.assign(`${API_BASE_URL}/oauth2/authorization/${provider}`);
}
