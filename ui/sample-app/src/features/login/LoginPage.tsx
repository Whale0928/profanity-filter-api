import { useCallback, useLayoutEffect, useRef, useState } from "react";

const DEFAULT_API_BASE_URL = "http://localhost:8080";
const API_BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL);

type LoginProvider = "github" | "google";

type LoginUser = {
  id: string;
  displayName: string;
  email: string;
  avatarUrl: string | null;
};

type AuthSession = {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: LoginUser;
};

type AuthView =
  | { state: "checking"; message: string }
  | { state: "anonymous"; message: string }
  | { state: "error"; message: string }
  | { state: "authenticated"; user: LoginUser; expiresIn: number };

type LoginCallback =
  | { state: "none" }
  | { state: "invalid" }
  | { state: "ready"; code: string };

type CsrfToken = {
  token: string;
  headerName: string;
};

class AuthRequestError extends Error {
  constructor(readonly status: number) {
    super("Authentication request failed");
    this.name = "AuthRequestError";
  }
}

export function LoginPage() {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [authView, setAuthView] = useState<AuthView>({
    state: "checking",
    message: "로그인 상태를 확인하고 있습니다.",
  });
  const callbackRef = useRef<LoginCallback | null>(null);
  const initializationStartedRef = useRef(false);
  const refreshFlightRef = useRef<Promise<AuthSession> | null>(null);

  if (callbackRef.current === null) {
    callbackRef.current = readLoginCallback();
  }

  const verifySession = useCallback(async (session: AuthSession) => {
    setAccessToken(session.accessToken);
    setAuthView({ state: "checking", message: "로그인 정보를 확인하고 있습니다." });

    try {
      const user = await fetchCurrentUser(session.accessToken, session.tokenType);
      setAuthView({ state: "authenticated", user, expiresIn: session.expiresIn });
    } catch {
      setAccessToken(null);
      setAuthView({
        state: "error",
        message: "로그인 정보를 확인하지 못했습니다. 다시 로그인해 주세요.",
      });
    }
  }, []);

  const refreshSession = useCallback(() => {
    if (refreshFlightRef.current) {
      return refreshFlightRef.current;
    }

    const refreshFlight = requestRefreshSession().finally(() => {
      if (refreshFlightRef.current === refreshFlight) {
        refreshFlightRef.current = null;
      }
    });

    refreshFlightRef.current = refreshFlight;
    return refreshFlight;
  }, []);

  const restoreSession = useCallback(async () => {
    setAuthView({ state: "checking", message: "로그인 상태를 복구하고 있습니다." });

    try {
      const session = await refreshSession();
      await verifySession(session);
    } catch (error) {
      setAccessToken(null);
      setAuthView(toRefreshFailureView(error));
    }
  }, [refreshSession, verifySession]);

  useLayoutEffect(() => {
    if (initializationStartedRef.current) {
      return;
    }

    initializationStartedRef.current = true;
    const callback = callbackRef.current ?? { state: "none" };
    callbackRef.current = { state: "none" };
    removeLoginFragment();

    if (callback.state === "invalid") {
      setAuthView({
        state: "error",
        message: "로그인 응답을 확인할 수 없습니다. 소셜 로그인을 다시 시작해 주세요.",
      });
      return;
    }

    if (callback.state === "ready") {
      setAuthView({ state: "checking", message: "소셜 로그인을 완료하고 있습니다." });
      void exchangeLoginCode(callback.code)
        .then(verifySession)
        .catch(() => {
          setAccessToken(null);
          setAuthView({
            state: "error",
            message: "로그인을 완료하지 못했습니다. 소셜 로그인을 다시 시작해 주세요.",
          });
        });
      return;
    }

    void restoreSession();
  }, [restoreSession, verifySession]);

  function startOAuthLogin(provider: LoginProvider) {
    window.location.assign(`${API_BASE_URL}/oauth2/authorization/${provider}`);
  }

  async function checkCurrentSession() {
    if (!accessToken) {
      await restoreSession();
      return;
    }

    const expiresIn = authView.state === "authenticated" ? authView.expiresIn : 0;
    setAuthView({ state: "checking", message: "로그인 상태를 확인하고 있습니다." });
    try {
      const user = await fetchCurrentUser(accessToken, "Bearer");
      setAuthView({ state: "authenticated", user, expiresIn });
    } catch {
      setAccessToken(null);
      setAuthView({
        state: "anonymous",
        message: "로그인 세션이 만료되었습니다. 다시 로그인해 주세요.",
      });
    }
  }

  const isChecking = authView.state === "checking";

  return (
    <section className="login-page" aria-labelledby="login-title">
      <div className="login-panel">
        <div className="login-actions">
          <div>
            <p className="login-kicker">OAuth 로그인</p>
            <h1 id="login-title">소셜 계정으로 로그인</h1>
          </div>

          <div className="login-provider-list" aria-label="소셜 로그인 제공자">
            <button
              className="login-provider-button"
              disabled={isChecking}
              onClick={() => startOAuthLogin("github")}
              type="button"
            >
              <GitHubIcon />
              <span>GitHub</span>
            </button>
            <button
              className="login-provider-button"
              disabled={isChecking}
              onClick={() => startOAuthLogin("google")}
              type="button"
            >
              <GoogleIcon />
              <span>Google</span>
            </button>
          </div>
        </div>

        <LoginStatusPanel authView={authView} onCheckSession={() => void checkCurrentSession()} />
      </div>
    </section>
  );
}

function LoginStatusPanel({ authView, onCheckSession }: { authView: AuthView; onCheckSession: () => void }) {
  if (authView.state === "authenticated") {
    const avatarUrl = toSafeAvatarUrl(authView.user.avatarUrl);
    const expiresInMinutes = Math.max(1, Math.ceil(authView.expiresIn / 60));

    return (
      <aside className="login-result-panel" aria-live="polite">
        <span>로그인됨</span>
        <div className="login-user-card">
          {avatarUrl ? (
            <img
              alt=""
              className="login-user-avatar"
              height="64"
              referrerPolicy="no-referrer"
              src={avatarUrl}
              width="64"
            />
          ) : (
            <div aria-hidden="true" className="login-user-avatar login-user-avatar-fallback">
              {authView.user.displayName.slice(0, 1).toUpperCase()}
            </div>
          )}
          <div>
            <p className="login-user-name">{authView.user.displayName}</p>
            <p className="login-user-email">{authView.user.email}</p>
          </div>
          <p className="login-session-note">
            현재 브라우저에서 로그인 상태를 유지합니다. 약 {expiresInMinutes}분 후 자동 갱신이 필요합니다.
          </p>
          <button className="login-session-button" onClick={onCheckSession} type="button">
            로그인 상태 확인
          </button>
        </div>
      </aside>
    );
  }

  return (
    <aside className="login-result-panel" aria-live="polite">
      <span>{authView.state === "checking" ? "확인 중" : "로그인 상태"}</span>
      <div className="login-status-message" data-state={authView.state} role="status">
        <p>{authView.message}</p>
        {authView.state !== "checking" ? (
          <button className="login-session-button" onClick={onCheckSession} type="button">
            세션 다시 확인
          </button>
        ) : null}
      </div>
    </aside>
  );
}

function readLoginCallback(): LoginCallback {
  const hash = window.location.hash.replace(/^#/, "");
  if (!hash) {
    return { state: "none" };
  }

  const params = new URLSearchParams(hash);
  const codes = params.getAll("code");
  const hasOnlyCode = Array.from(params.keys()).every((key) => key === "code");

  if (!hasOnlyCode || codes.length !== 1 || codes[0].length === 0 || codes[0].length > 4096) {
    return { state: "invalid" };
  }

  return { state: "ready", code: codes[0] };
}

function removeLoginFragment() {
  if (!window.location.hash) {
    return;
  }

  const cleanUrl = `${window.location.pathname}${window.location.search}`;
  window.history.replaceState(window.history.state, "", cleanUrl);
}

async function exchangeLoginCode(code: string): Promise<AuthSession> {
  const data = await requestApiData("/api/v1/auth/exchange", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ code }),
  });

  return readAuthSession(data);
}

async function requestRefreshSession(): Promise<AuthSession> {
  const csrfData = await requestApiData("/api/v1/auth/csrf", { method: "GET" });
  const csrfToken = readCsrfToken(csrfData);
  const data = await requestApiData("/api/v1/auth/refresh", {
    method: "POST",
    headers: { [csrfToken.headerName]: csrfToken.token },
  });

  return readAuthSession(data);
}

async function fetchCurrentUser(accessToken: string, tokenType: "Bearer"): Promise<LoginUser> {
  const data = await requestApiData("/api/v1/auth/me", {
    method: "GET",
    headers: { Authorization: `${tokenType} ${accessToken}` },
  });

  const record = toRecord(data);
  return readLoginUser("user" in record ? record.user : record);
}

async function requestApiData(path: string, init: RequestInit): Promise<unknown> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
  });

  if (!response.ok) {
    throw new AuthRequestError(response.status);
  }

  const payload: unknown = await response.json();
  const envelope = toRecord(payload);
  if (!("data" in envelope)) {
    throw new AuthRequestError(response.status);
  }

  return envelope.data;
}

function readAuthSession(value: unknown): AuthSession {
  const record = toRecord(value);
  const accessToken = readRequiredString(record, "accessToken");
  const tokenType = readRequiredString(record, "tokenType");
  const expiresIn = record.expiresIn;

  if (
    tokenType.toLowerCase() !== "bearer" ||
    typeof expiresIn !== "number" ||
    !Number.isFinite(expiresIn) ||
    expiresIn <= 0
  ) {
    throw new TypeError("Invalid authentication session");
  }

  return {
    accessToken,
    tokenType: "Bearer",
    expiresIn,
    user: readLoginUser(record.user),
  };
}

function readLoginUser(value: unknown): LoginUser {
  const record = toRecord(value);
  const id = record.id;

  if ((typeof id !== "string" && typeof id !== "number") || String(id).length === 0) {
    throw new TypeError("Invalid login user");
  }

  return {
    id: String(id),
    displayName: readRequiredString(record, "displayName"),
    email: readRequiredString(record, "email"),
    avatarUrl: readNullableString(record, "avatarUrl"),
  };
}

function readCsrfToken(value: unknown): CsrfToken {
  const record = toRecord(value);
  const headerName = readRequiredString(record, "headerName");

  if (!/^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/.test(headerName)) {
    throw new TypeError("Invalid CSRF header name");
  }

  return {
    token: readRequiredString(record, "token"),
    headerName,
  };
}

function readRequiredString(record: Record<string, unknown>, key: string): string {
  const value = record[key];
  if (typeof value !== "string" || value.length === 0) {
    throw new TypeError(`Invalid ${key}`);
  }
  return value;
}

function readNullableString(record: Record<string, unknown>, key: string): string | null {
  const value = record[key];
  if (value === null || value === undefined) {
    return null;
  }
  if (typeof value !== "string") {
    throw new TypeError(`Invalid ${key}`);
  }
  return value;
}

function toRecord(value: unknown): Record<string, unknown> {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new TypeError("Invalid API response");
  }
  return value as Record<string, unknown>;
}

function toRefreshFailureView(error: unknown): AuthView {
  if (error instanceof AuthRequestError && (error.status === 401 || error.status === 403)) {
    return {
      state: "anonymous",
      message: "로그인이 필요합니다. GitHub 또는 Google 계정으로 시작해 주세요.",
    };
  }

  return {
    state: "error",
    message: "로그인 상태를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.",
  };
}

function toSafeAvatarUrl(value: string | null): string | null {
  if (!value) {
    return null;
  }

  try {
    const url = new URL(value);
    return url.protocol === "https:" || url.protocol === "http:" ? url.toString() : null;
  } catch {
    return null;
  }
}

function resolveApiBaseUrl(value: string | undefined): string {
  const configuredUrl = value?.trim().replace(/\/+$/, "");
  return configuredUrl || DEFAULT_API_BASE_URL;
}

function GitHubIcon() {
  return (
    <svg aria-hidden="true" className="login-provider-icon" focusable="false" viewBox="0 0 24 24">
      <path
        d="M12 2C6.48 2 2 6.58 2 12.26c0 4.53 2.87 8.38 6.84 9.74.5.1.68-.22.68-.49 0-.24-.01-1.04-.01-1.89-2.51.47-3.16-.63-3.36-1.21-.11-.3-.6-1.21-1.03-1.45-.35-.19-.85-.66-.01-.67.79-.01 1.35.74 1.54 1.05.9 1.55 2.34 1.11 2.91.85.09-.67.35-1.11.64-1.37-2.22-.26-4.55-1.14-4.55-5.05 0-1.11.39-2.03 1.03-2.75-.1-.26-.45-1.3.1-2.71 0 0 .84-.28 2.75 1.05A9.2 9.2 0 0 1 12 7.02c.85 0 1.71.12 2.51.34 1.91-1.33 2.75-1.05 2.75-1.05.55 1.41.2 2.45.1 2.71.64.72 1.03 1.63 1.03 2.75 0 3.92-2.34 4.79-4.57 5.05.36.32.68.94.68 1.91 0 1.38-.01 2.49-.01 2.83 0 .27.18.59.69.49A10.13 10.13 0 0 0 22 12.26C22 6.58 17.52 2 12 2Z"
        fill="currentColor"
      />
    </svg>
  );
}

function GoogleIcon() {
  return (
    <svg aria-hidden="true" className="login-provider-icon" focusable="false" viewBox="0 0 24 24">
      <path
        d="M21.56 12.25c0-.74-.07-1.45-.19-2.14H12v4.05h5.36a4.58 4.58 0 0 1-1.99 3.01v2.5h3.22c1.89-1.74 2.97-4.3 2.97-7.42Z"
        fill="#4285F4"
      />
      <path
        d="M12 22c2.7 0 4.96-.89 6.61-2.42l-3.22-2.5c-.89.6-2.03.95-3.39.95-2.6 0-4.8-1.76-5.59-4.12H3.08v2.58A10 10 0 0 0 12 22Z"
        fill="#34A853"
      />
      <path
        d="M6.41 13.91a6 6 0 0 1 0-3.82V7.51H3.08a10 10 0 0 0 0 8.98l3.33-2.58Z"
        fill="#FBBC05"
      />
      <path
        d="M12 5.97c1.47 0 2.79.51 3.83 1.5l2.85-2.85C16.96 3.02 14.7 2 12 2a10 10 0 0 0-8.92 5.51l3.33 2.58C7.2 7.73 9.4 5.97 12 5.97Z"
        fill="#EA4335"
      />
    </svg>
  );
}
