import {
  ArrowRight,
  BookOpen,
  CaretDown,
  Check,
  Copy,
  GithubLogo,
  Key,
  List,
  LockKey,
  Moon,
  ShieldCheck,
  SignOut,
  Sun,
  UserCircle,
  X,
} from "@phosphor-icons/react";
import { useEffect, useMemo, useState, type MouseEvent, type ReactNode } from "react";

import { exchangeLoginCode, restoreLoginSession, startSocialLogin, type LoginUser } from "./auth";
import ApiKeysPage from "./ApiKeysPage";
import DocsPage from "./docs/DocsPage";

type Theme = "light" | "dark";
type RoutePath = "/" | "/docs" | "/login" | "/app" | "/app/credentials" | "/app/account" | "/app/keys";
type CredentialKind = "api-key" | "oauth";
type AuthStatus = "checking" | "anonymous" | "exchanging" | "authenticated" | "failed";

const ROUTES: RoutePath[] = ["/", "/docs", "/login", "/app", "/app/credentials", "/app/account", "/app/keys"];

const PUBLIC_PAGE_METADATA = {
  "/": {
    title: "한국어 욕설·비속어 필터 API | 말조심하세욧",
    description: "한국어 문장의 욕설과 비속어를 검출하고 QUICK, NORMAL, FILTER 모드로 확인하거나 마스킹하는 REST API입니다.",
    canonical: "https://developers.kr-filter.com/",
  },
  "/docs": {
    title: "한국어 텍스트 처리 API 문서 | 말조심하세욧",
    description: "한국어 욕설·비속어 검출 API의 인증, 요청 형식, 처리 모드, 오류 모델과 OpenAPI 명세를 확인하세요.",
    canonical: "https://developers.kr-filter.com/docs",
  },
} as const;

function currentPath(): RoutePath {
  const pathname = window.location.pathname;
  return ROUTES.includes(pathname as RoutePath) ? (pathname as RoutePath) : "/";
}

function preferredTheme(): Theme {
  const saved = window.localStorage.getItem("pf-theme");
  if (saved === "light" || saved === "dark") return saved;
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function updatePageMetadata(path: RoutePath) {
  const isPublicPage = path === "/" || path === "/docs";
  const metadata = isPublicPage
    ? PUBLIC_PAGE_METADATA[path]
    : {
        title: path === "/login" ? "로그인 | 말조심하세욧" : "개발자 포털 | 말조심하세욧",
        description: "말조심하세욧 개발자 포털입니다.",
        canonical: `https://developers.kr-filter.com${path}`,
      };

  document.title = metadata.title;
  document.querySelector<HTMLMetaElement>('meta[name="description"]')?.setAttribute("content", metadata.description);
  document.querySelector<HTMLMetaElement>('meta[name="robots"]')?.setAttribute("content", isPublicPage ? "index, follow" : "noindex, nofollow");
  document.querySelector<HTMLLinkElement>('link[rel="canonical"]')?.setAttribute("href", metadata.canonical);
  document.querySelector<HTMLMetaElement>('meta[property="og:title"]')?.setAttribute("content", metadata.title);
  document.querySelector<HTMLMetaElement>('meta[property="og:description"]')?.setAttribute("content", metadata.description);
  document.querySelector<HTMLMetaElement>('meta[property="og:url"]')?.setAttribute("content", metadata.canonical);
  document.querySelector<HTMLMetaElement>('meta[name="twitter:title"]')?.setAttribute("content", metadata.title);
  document.querySelector<HTMLMetaElement>('meta[name="twitter:description"]')?.setAttribute("content", metadata.description);
}

export default function App() {
  const [path, setPath] = useState<RoutePath>(currentPath);
  const [theme, setTheme] = useState<Theme>(preferredTheme);
  const [authStatus, setAuthStatus] = useState<AuthStatus>("checking");
  const [loginUser, setLoginUser] = useState<LoginUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [authError, setAuthError] = useState("");
  const [mobileOpen, setMobileOpen] = useState(false);
  const authenticated = authStatus === "authenticated";

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    window.localStorage.setItem("pf-theme", theme);
  }, [theme]);

  useEffect(() => {
    updatePageMetadata(path);
  }, [path]);

  useEffect(() => {
    const onPopState = () => setPath(currentPath());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  useEffect(() => {
    let cancelled = false;
    const code = window.location.pathname === "/login"
      ? new URLSearchParams(window.location.hash.replace(/^#/, "")).get("code")
      : null;

    async function authenticate() {
      try {
        if (code) {
          setAuthStatus("exchanging");
          window.history.replaceState({}, "", "/login");
        }
        const session = code ? await exchangeLoginCode(code) : await restoreLoginSession();
        if (cancelled) return;
        setAccessToken(session.accessToken);
        setLoginUser(session.user);
        setAuthStatus("authenticated");
      } catch (error) {
        if (cancelled) return;
        setAccessToken(null);
        setLoginUser(null);
        setAuthStatus(code ? "failed" : "anonymous");
        setAuthError(code && error instanceof Error ? error.message : "");
      }
    }

    void authenticate();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (path === "/app" || path === "/app/credentials") navigate("/app/keys");
    if ((authStatus === "anonymous" || authStatus === "failed") && (path === "/app/account" || path === "/app/keys")) navigate("/login");
    if (authenticated && path === "/login") navigate("/");
  }, [authStatus, authenticated, path]);

  function navigate(next: RoutePath) {
    if (window.location.pathname !== next) window.history.pushState({}, "", next);
    setPath(next);
    setMobileOpen(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function signOut() {
    setAccessToken(null);
    setLoginUser(null);
    setAuthStatus("anonymous");
    navigate("/login");
  }

  const page = useMemo(() => {
    switch (path) {
      case "/docs":
        return <DocsPage theme={theme} />;
      case "/login":
        return <LoginPage error={authError} status={authStatus} />;
      case "/app/account":
        return <AccountPage user={loginUser} />;
      case "/app/keys":
        return accessToken && loginUser ? <ApiKeysPage accessToken={accessToken} /> : null;
      default:
        return (
          <OverviewPage
            authenticated={authenticated}
            loginUser={loginUser}
            onNavigate={navigate}
          />
        );
    }
  }, [accessToken, authError, authenticated, authStatus, loginUser, path]);

  return (
    <div className="app-shell">
      <GlobalHeader
        authenticated={authenticated}
        loginUser={loginUser}
        mobileOpen={mobileOpen}
        onMenu={() => setMobileOpen((open) => !open)}
        onNavigate={navigate}
        onSignOut={signOut}
        onTheme={() => setTheme(theme === "dark" ? "light" : "dark")}
        path={path}
        theme={theme}
      />
      <main id="main-content">{page}</main>
    </div>
  );
}

type NavigationProps = {
  authenticated: boolean;
  loginUser: LoginUser | null;
  mobileOpen: boolean;
  onMenu: () => void;
  onNavigate: (path: RoutePath) => void;
  onSignOut: () => void;
  onTheme: () => void;
  path: RoutePath;
  theme: Theme;
};

function GlobalHeader({ authenticated, loginUser, mobileOpen, onMenu, onNavigate, onSignOut, onTheme, path, theme }: NavigationProps) {
  const [accountOpen, setAccountOpen] = useState(false);
  const go = (next: RoutePath) => {
    setAccountOpen(false);
    onNavigate(next);
  };
  return (
    <header className="global-header">
      <InternalLink className="brand" onNavigate={onNavigate} to="/">
        <strong>말조심하세욧</strong>
        <span>한국어 욕설 필터 API</span>
      </InternalLink>
      <button aria-expanded={mobileOpen} aria-label="메뉴 열기" className="mobile-menu" onClick={onMenu} type="button">
        {mobileOpen ? <X size={22} /> : <List size={22} />}
      </button>
      <div className={mobileOpen ? "global-actions open" : "global-actions"}>
        <nav aria-label="공개 메뉴">
          <NavLink active={path === "/"} label="소개" onNavigate={onNavigate} to="/" />
          <NavLink active={path === "/docs"} label="API 문서" onNavigate={onNavigate} to="/docs" />
        </nav>
        <button aria-label={`${theme === "dark" ? "라이트" : "다크"} 모드로 전환`} className="theme-toggle" onClick={onTheme} type="button">
          {theme === "dark" ? <Moon size={17} weight="fill" /> : <Sun size={18} weight="fill" />}
          <i aria-hidden="true" />
        </button>
        {authenticated ? (
          <div className="identity-menu">
            <button aria-expanded={accountOpen} aria-haspopup="menu" className="identity" onClick={() => setAccountOpen((open) => !open)} type="button">
              <span className="avatar">{loginUser?.displayName.trim().slice(0, 1) || "나"}</span>
              <span>{loginUser?.displayName || "내 계정"}</span>
              <CaretDown aria-hidden="true" size={14} />
            </button>
            {accountOpen ? (
              <div aria-label="사용자 메뉴" className="identity-popover" role="menu">
                <button onClick={() => go("/app/keys")} role="menuitem" type="button"><Key size={17} />API Key 관리</button>
                <button onClick={() => go("/app/account")} role="menuitem" type="button"><UserCircle size={17} />내 계정</button>
                <button className="sign-out" onClick={() => { setAccountOpen(false); onSignOut(); }} role="menuitem" type="button"><SignOut size={17} />로그아웃</button>
              </div>
            ) : null}
          </div>
        ) : (
          <InternalLink className="login-link" onNavigate={onNavigate} to="/login">로그인</InternalLink>
        )}
      </div>
    </header>
  );
}

function InternalLink({
  children,
  className,
  current,
  onNavigate,
  to,
}: {
  children: ReactNode;
  className?: string;
  current?: boolean;
  onNavigate: (path: RoutePath) => void;
  to: RoutePath;
}) {
  const navigateInternally = (event: MouseEvent<HTMLAnchorElement>) => {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    onNavigate(to);
  };

  return <a aria-current={current ? "page" : undefined} className={className} href={to} onClick={navigateInternally}>{children}</a>;
}

function NavLink({ active, label, onNavigate, to }: { active: boolean; label: string; onNavigate: (path: RoutePath) => void; to: RoutePath }) {
  return <InternalLink current={active} onNavigate={onNavigate} to={to}>{label}</InternalLink>;
}

function OverviewPage({
  authenticated,
  loginUser,
  onNavigate,
}: {
  authenticated: boolean;
  loginUser: LoginUser | null;
  onNavigate: (path: RoutePath) => void;
}) {
  const showCredentials = () => document.getElementById("credentials")?.scrollIntoView({ behavior: "smooth" });

  return (
    <div className="overview-page">
      <section className="intro-page page-width">
        <p className="eyebrow">Korean profanity filter API</p>
        <h1>한국어 욕설·비속어<br />필터 API</h1>
        <p className="lead">한국어 문장의 욕설과 비속어를 검출하고 필요한 방식으로 확인하거나 마스킹하는 API입니다.</p>
        <div className="intro-actions">
          {authenticated ? (
            <button className="primary-action" onClick={showCredentials} type="button">자격 증명 선택 <ArrowRight size={18} /></button>
          ) : (
            <button className="primary-action" onClick={() => onNavigate("/login")} type="button">로그인하여 시작 <ArrowRight size={18} /></button>
          )}
          <InternalLink className="text-action" onNavigate={onNavigate} to="/docs">API 문서 보기</InternalLink>
        </div>
        <div className="auth-summary">
          <p>{authenticated ? "API Key로 바로 연동할 수 있습니다" : "로그인 후 선택할 수 있습니다"}</p>
          <div><Key size={24} /><span><b>API Key</b>빠르고 단순한 연동</span></div>
          <div aria-disabled="true" className="auth-summary-future">
            <LockKey size={24} />
            <span><b>OAuth2 Client Credentials</b>운영·서버 간 연동 예정</span>
            <small>추후 제공</small>
          </div>
        </div>
      </section>

      <section className="overview-start page-width">
        <div>
          <p className="eyebrow">시작하기</p>
          <h2>{authenticated ? `반갑습니다, ${loginUser?.displayName || "개발자"}님.` : "계정으로 연동을 시작하세요."}</h2>
          <p className="lead">
            {authenticated
              ? "연동 환경에 맞는 자격 증명을 선택하고 API 문서에서 요청 방식을 확인하세요."
              : "Google 또는 GitHub 계정으로 로그인한 뒤 자격 증명을 만들고 관리할 수 있습니다."}
          </p>
        </div>
        <div className="next-actions">
          <button onClick={authenticated ? showCredentials : () => onNavigate("/login")} type="button"><Key size={22} /><span><b>자격 증명 선택</b>API Key와 OAuth2 방식을 비교합니다.</span><ArrowRight size={19} /></button>
          <InternalLink onNavigate={onNavigate} to="/docs"><BookOpen size={22} /><span><b>API 문서 보기</b>인증과 요청 형식을 확인합니다.</span><ArrowRight size={19} /></InternalLink>
        </div>
      </section>

      <CredentialsSection
        authenticated={authenticated}
        onCreate={(kind) => { if (kind === "api-key") onNavigate("/app/keys"); }}
        onLogin={() => onNavigate("/login")}
      />
    </div>
  );
}

function LoginPage({ error, status }: { error: string; status: AuthStatus }) {
  const pending = status === "checking" || status === "exchanging";
  return (
    <section className="login-page page-width">
      <div>
        <p className="eyebrow">Sign in</p>
        <h1>계정으로 시작하세요.</h1>
        <p className="lead">자격 증명은 Google 또는 GitHub로 로그인한 사용자만 만들고 관리할 수 있습니다.</p>
      </div>
      <div className="provider-list">
        <button disabled={pending} onClick={() => startSocialLogin("github")} type="button"><GithubLogo size={24} weight="fill" />GitHub로 계속</button>
        <button disabled={pending} onClick={() => startSocialLogin("google")} type="button"><span className="google-mark">G</span>Google로 계속</button>
        <p role={error ? "alert" : "status"}>{error || (pending ? "로그인 상태를 확인하고 있습니다." : "선택한 계정의 로그인 화면으로 이동합니다.")}</p>
      </div>
    </section>
  );
}

function CredentialsSection({
  authenticated,
  onCreate,
  onLogin,
}: {
  authenticated: boolean;
  onCreate: (kind: CredentialKind) => void;
  onLogin: () => void;
}) {
  const createCredential = (kind: CredentialKind) => authenticated ? onCreate(kind) : onLogin();

  return (
    <section className="credentials-page page-width" id="credentials">
      <header className="page-heading">
        <h1>자격 증명</h1>
        <p>{authenticated ? "API Key를 만들거나 다음 인증 방식을 미리 확인하세요." : "Google 또는 GitHub 계정으로 로그인한 뒤 만들 수 있습니다."}</p>
      </header>
      <div className="credential-grid">
        <CredentialMethod
          action={authenticated ? "API Key 만들기" : "로그인 후 API Key 만들기"}
          code={'curl -X POST https://api.kr-filter.com/api/v1/filter \\\n  -H "Content-Type: application/json" \\\n  -H "x-api-key: $API_KEY"'}
          description="발급된 키를 요청 헤더에 포함하는 가장 단순한 인증 방식입니다. 이 화면에는 키 원문을 표시하지 않습니다."
          icon={<Key size={28} />}
          kind="api-key"
          points={["로그인 후 API Key 발급", "x-api-key 헤더로 API 호출", "재발급하면 이전 키는 즉시 무효화"]}
          requestLabel="API 요청 예시"
          subtitle="빠르고 단순한 연동"
          title="API Key"
          onCreate={createCredential}
        />
        <CredentialMethod
          action="OAuth 클라이언트 만들기"
          code={'curl -X POST https://api.kr-filter.com/oauth2/token \\\n  -u "$CLIENT_ID:$CLIENT_SECRET" \\\n  -d "grant_type=client_credentials"'}
          description="클라이언트 생성 직후 Client Secret을 한 번만 확인하고, 이후 access token을 발급받는 서버 간 인증 흐름입니다."
          disabled
          icon={<LockKey size={28} />}
          kind="oauth"
          points={["발급 완료 화면에서 Client Secret 최초 1회 확인", "Client ID와 Secret으로 token 요청", "만료 시 token을 다시 발급"]}
          recommended
          requestLabel="Token 요청 예시"
          subtitle="운영·서버 간 연동 권장"
          title="OAuth2 Client Credentials"
          onCreate={createCredential}
        />
      </div>
      <div className="security-note"><ShieldCheck size={28} /><b>보안 안내</b><span>이 페이지는 요청 형식만 안내합니다. 실제 API Key, Client Secret, access token 원문은 화면에 다시 노출하지 않습니다.</span></div>
    </section>
  );
}

type CredentialMethodProps = {
  action: string;
  code: string;
  description: string;
  disabled?: boolean;
  icon: React.ReactNode;
  kind: CredentialKind;
  onCreate: (kind: CredentialKind) => void;
  points: string[];
  recommended?: boolean;
  requestLabel: string;
  subtitle: string;
  title: string;
};

function CredentialMethod(props: CredentialMethodProps) {
  const [copied, setCopied] = useState(false);
  async function copyExample() {
    await navigator.clipboard?.writeText(props.code);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1200);
  }
  return (
    <article aria-disabled={props.disabled || undefined} className={props.disabled ? "credential-method credential-method-future" : "credential-method"}>
      {props.disabled ? (
        <div className="future-sign" role="note">
          <span>추후 업데이트</span>
          <strong>OAuth2 Client Credentials 준비 중</strong>
          <small>구성과 사용 흐름을 미리 확인할 수 있습니다.</small>
        </div>
      ) : null}
      <div className="method-title"><span className="method-icon">{props.icon}</span><h2>{props.title}</h2>{props.recommended ? <em>운영 권장</em> : null}</div>
      <p className="method-subtitle">{props.subtitle}</p>
      <p className="method-description">{props.description}</p>
      <label>{props.requestLabel}</label>
      <button aria-label={`${props.requestLabel} 복사`} className="code-example" disabled={props.disabled} onClick={copyExample} type="button"><code>{props.code}</code>{copied ? <Check size={19} /> : <Copy size={19} />}</button>
      <ul>{props.points.map((point) => <li key={point}><Check size={18} />{point}</li>)}</ul>
      <button className="create-action" disabled={props.disabled} onClick={() => props.onCreate(props.kind)} type="button">{props.action}<ArrowRight size={19} /></button>
    </article>
  );
}

function AccountPage({ user }: { user: LoginUser | null }) {
  return (
    <section className="account-page page-width">
      <header className="page-heading"><h1>내 계정</h1><p>SSO에서 확인한 기본 계정 정보입니다.</p></header>
      <div className="account-profile">
        <UserCircle size={64} weight="thin" />
        <dl><div><dt>표시 이름</dt><dd>{user?.displayName ?? "-"}</dd></div><div><dt>Primary email</dt><dd>{user?.email ?? "-"}</dd></div><div><dt>로그인 상태</dt><dd><span className="status-dot" />활성</dd></div></dl>
      </div>
    </section>
  );
}
