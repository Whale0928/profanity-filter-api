import { useEffect, useMemo, useState } from "react";

const API_BASE_URL = "http://localhost:8080";
const LOGIN_RESULT_EMPTY = "아직 로그인 응답이 없습니다.";

type LoginProvider = "github" | "google";

type OAuthLoginResult = Record<string, string>;

export function LoginPage() {
  const [loginResult, setLoginResult] = useState<OAuthLoginResult | null>(() => parseLoginResult());

  useEffect(() => {
    function syncLoginResult() {
      setLoginResult(parseLoginResult());
    }

    window.addEventListener("hashchange", syncLoginResult);
    return () => window.removeEventListener("hashchange", syncLoginResult);
  }, []);

  const formattedResult = useMemo(() => {
    if (!loginResult) {
      return LOGIN_RESULT_EMPTY;
    }

    return JSON.stringify(loginResult, null, 2);
  }, [loginResult]);

  function startOAuthLogin(provider: LoginProvider) {
    window.location.assign(`${API_BASE_URL}/oauth2/authorization/${provider}`);
  }

  return (
    <section className="login-page" aria-labelledby="login-title">
      <div className="login-panel">
        <div className="login-actions">
          <div>
            <p className="login-kicker">OAuth 로그인</p>
            <h1 id="login-title">소셜 계정으로 로그인</h1>
          </div>

          <div className="login-provider-list" aria-label="소셜 로그인 제공자">
            <button className="login-provider-button" onClick={() => startOAuthLogin("github")} type="button">
              <GitHubIcon />
              <span>GitHub</span>
            </button>
            <button className="login-provider-button" onClick={() => startOAuthLogin("google")} type="button">
              <GoogleIcon />
              <span>Google</span>
            </button>
          </div>
        </div>

        <aside className="login-result-panel" aria-live="polite">
          <span>최종 응답</span>
          <pre data-empty={loginResult ? "false" : "true"}>{formattedResult}</pre>
        </aside>
      </div>
    </section>
  );
}

function parseLoginResult(): OAuthLoginResult | null {
  const hash = window.location.hash.replace(/^#/, "");

  if (!hash) {
    return null;
  }

  const params = new URLSearchParams(hash);
  const result = Object.fromEntries(params.entries());

  return Object.keys(result).length > 0 ? result : null;
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
