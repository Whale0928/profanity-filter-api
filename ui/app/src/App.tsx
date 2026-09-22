import {
  ArrowRight,
  CaretDown,
  Copy,
  GithubLogo,
  Key, ListChecks,
  List,
  Moon,
  SignOut,
  ShieldCheck,
  Sun,
  UserCircle,
  X,
} from "@phosphor-icons/react";
import { lazy, Suspense, useEffect, useMemo, useRef, useState, type MouseEvent, type ReactNode } from "react";

import { exchangeLoginCode, logoutSession, restoreLoginSession, startSocialLogin, type LoginUser } from "./auth";
import ApiKeysPage from "./ApiKeysPage";
import WhitelistsPage from "./WhitelistsPage";
import DocsPage from "./docs/DocsPage";
import FilterExample from "./FilterExample";
const AdminPage = lazy(() => import("./AdminPage"));
const NewsPage = lazy(() => import("./NewsPage"));
const LegalPage = lazy(() => import("./LegalPage"));
const DashboardInquiries = lazy(() => import("./DashboardInquiries"));

type Theme = "light" | "dark";
type RoutePath = "/" | "/admin" | "/news" | "/docs" | "/login" | "/app" | "/app/credentials" | "/app/account" | "/app/keys" | "/app/whitelists" | "/privacy" | "/terms";
type AuthStatus = "checking" | "anonymous" | "exchanging" | "authenticated" | "failed";

const ROUTES: RoutePath[] = ["/", "/admin", "/news", "/docs", "/login", "/app", "/app/credentials", "/app/account", "/app/keys", "/app/whitelists", "/privacy", "/terms"];

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

function readPreviewParam(): "visitor" | "user" | "admin" | null {
  if (!import.meta.env.DEV) return null;
  const role = new URLSearchParams(window.location.search).get("preview");
  return role === "admin" || role === "user" || role === "visitor" ? role : null;
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
        title: `${path === "/admin" ? "관리자" : path === "/news" ? "소식" : path === "/privacy" ? "개인정보 처리방침" : path === "/terms" ? "이용약관" : path === "/login" ? "로그인" : "개발자 포털"} | 말조심하세욧`,
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
  const initialAuth = useRef<{ exchanging: boolean; session: ReturnType<typeof exchangeLoginCode> } | null>(null);
  const [signingOut, setSigningOut] = useState(false);
  const [logoutError, setLogoutError] = useState("");
  const [mobileOpen, setMobileOpen] = useState(false);
  const authenticated = authStatus === "authenticated";
  const [previewRole, setPreviewRole] = useState<"visitor" | "user" | "admin">(() => readPreviewParam() ?? "visitor");
  const [previewActive, setPreviewActive] = useState(() => import.meta.env.DEV && readPreviewParam() !== null);
  const previewing = previewActive && (path === "/news" || path === "/admin");
  const shownAuthenticated = previewing ? previewRole !== "visitor" : authenticated;
  const shownUser = previewing ? (previewRole === "visitor" ? null : { id: "ui-preview", displayName: previewRole === "admin" ? "관리자" : "일반 회원", email: "preview@example.test", avatarUrl: null, admin: previewRole === "admin" }) : loginUser;
  const shownAdmin = shownAuthenticated && shownUser?.admin === true;
  function changePreview(role: "visitor" | "user" | "admin") {
    setPreviewRole(role);
    setPreviewActive(true);
    const params = new URLSearchParams(window.location.search);
    params.set("preview", role);
    window.history.replaceState({}, "", `${path}?${params}`);
  }
  function exitPreview() {
    setPreviewActive(false);
    const params = new URLSearchParams(window.location.search);
    params.delete("preview");
    window.history.replaceState({}, "", `${path}${params.size ? `?${params}` : ""}`);
  }

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    window.localStorage.setItem("pf-theme", theme);
  }, [theme]);

  useEffect(() => {
    updatePageMetadata(path);
  }, [path]);

  useEffect(() => {
    // 유휴 시간에 라우트 청크를 미리 받아 둔다. 전환 순간에 내려받으면 화면이 비었다가 채워진다.
    const prefetch = () => {
      void import("./NewsPage");
      void import("./LegalPage");
    };
    const idle = window.requestIdleCallback?.(prefetch, { timeout: 3000});
    const timer = idle === undefined ? window.setTimeout(prefetch, 1200) : 0;
    return () => {
      if (idle !== undefined) window.cancelIdleCallback?.(idle);
      else window.clearTimeout(timer);
    };
  }, []);

  useEffect(() => {
    const onPopState = () => setPath(currentPath());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  useEffect(() => {
    let cancelled = false;
    // StrictMode가 effect를 다시 구독해도 일회용 코드 교환과 refresh는 한 번만 수행한다.
    if (!initialAuth.current) {
      const code = window.location.pathname === "/login"
        ? new URLSearchParams(window.location.hash.replace(/^#/, "")).get("code")
        : null;
      if (code) window.history.replaceState({}, "", "/login");
      initialAuth.current = {
        exchanging: Boolean(code),
        session: code ? exchangeLoginCode(code) : restoreLoginSession(),
      };
    }
    const task = initialAuth.current;
    if (task.exchanging) setAuthStatus("exchanging");

    async function authenticate() {
      try {
        const session = await task.session;
        if (cancelled) return;
        setAccessToken(session.accessToken);
        setLoginUser(session.user);
        setAuthStatus("authenticated");
      } catch (error) {
        if (cancelled) return;
        setAccessToken(null);
        setLoginUser(null);
        setAuthStatus(task.exchanging ? "failed" : "anonymous");
        setAuthError(task.exchanging && error instanceof Error ? error.message : "");
      }
    }

    void authenticate();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (path === "/app" || path === "/app/credentials") navigate("/app/keys");
    if ((authStatus === "anonymous" || authStatus === "failed") && (path === "/app/account" || path === "/app/keys" || path === "/app/whitelists")) navigate("/login");
    if (authenticated && path === "/login") navigate("/");
  }, [authStatus, authenticated, path]);

  function navigate(next: RoutePath) {
    const nextUrl = previewActive && (next === "/news" || next === "/admin") ? `${next}?preview=${previewRole}` : next;
    if (window.location.pathname + window.location.search !== nextUrl) window.history.pushState({}, "", nextUrl);
    if (next === path) window.dispatchEvent(new PopStateEvent("popstate"));
    setPath(next);
    setMobileOpen(false);
    window.scrollTo({ top: 0, behavior: "auto" });
  }

  async function signOut() {
    if (signingOut) return;
    setSigningOut(true);
    setLogoutError("");
    try {
      await logoutSession();
      setAccessToken(null);
      setLoginUser(null);
      setAuthStatus("anonymous");
      navigate("/login");
    } catch {
      setLogoutError("로그아웃하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setSigningOut(false);
    }
  }

  const previewControls = previewing ? <div className="prototype-bar"><div><span className="prototype-dot" /><strong>UI 목업</strong><span>예시 데이터 · 실제 서비스에 반영되지 않습니다.</span></div><div className="prototype-roles" role="group" aria-label="목업 사용자 상태">{([ ["visitor", "방문자"], ["user", "일반 회원"], ["admin", "관리자"] ] as const).map(([role, label]) => <button type="button" key={role} aria-pressed={previewRole === role} onClick={() => changePreview(role)}>{label}</button>)}<button type="button" onClick={exitPreview}>미리보기 종료</button></div></div> : null;

  const page = useMemo(() => {
    switch (path) {
      case "/news":
        return <NewsPage onManage={shownAdmin ? () => navigate("/admin") : undefined} preview={previewing} />;
      case "/admin":
        return <AdminPage accessToken={accessToken} allowed={shownAdmin} currentUserId={loginUser?.id ?? null} onPublic={() => navigate("/news")} preview={previewing} />;
      case "/docs":
        return <DocsPage theme={theme} />;
      case "/privacy":
      case "/terms":
        return <LegalPage kind={path === "/privacy" ? "privacy" : "terms"} />;
      case "/login":
        return <LoginPage error={authError} status={authStatus} />;
      case "/app/account":
        return <AccountPage accessToken={accessToken} user={loginUser} />;
      case "/app/keys":
        return accessToken && loginUser ? <ApiKeysPage accessToken={accessToken} /> : null;
      case "/app/whitelists":
        return accessToken && loginUser ? <WhitelistsPage accessToken={accessToken} /> : null;
      default:
        return (
          <OverviewPage
            authenticated={authenticated}
            loginUser={loginUser}
            onNavigate={navigate}
          />
        );
    }
  }, [accessToken, authError, authenticated, authStatus, loginUser, path, theme, shownAdmin, previewRole, previewing]);

  return (
    <div className="app-shell">
      <GlobalHeader
        authenticated={shownAuthenticated}
        loginUser={shownUser}
        mobileOpen={mobileOpen}
        onMenu={() => setMobileOpen((open) => !open)}
        onNavigate={navigate}
        onSignOut={() => { if (previewing) changePreview("visitor"); else void signOut(); }}
        signingOut={signingOut}
        onTheme={() => setTheme(theme === "dark" ? "light" : "dark")}
        path={path}
        theme={theme}
      />
      {previewControls}
      <main id="main-content">{logoutError ? <p className="session-error page-width" role="alert">{logoutError}</p> : null}<Suspense fallback={<div className="route-loading page-width" role="status"><span aria-hidden="true" /><p>불러오고 있습니다.</p></div>}>{page}</Suspense></main>
      <footer className="site-footer page-width">
        <div><strong>말조심하세욧</strong><span>한국어를 위한 필터 API</span></div>
        <nav aria-label="서비스 정책">
          <InternalLink onNavigate={navigate} to="/privacy">개인정보 처리방침</InternalLink>
          <InternalLink onNavigate={navigate} to="/terms">이용약관</InternalLink>
        </nav>
      </footer>
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
  signingOut: boolean;
  onTheme: () => void;
  path: RoutePath;
  theme: Theme;
};

function BrandMark() {
  return (
    <svg aria-hidden="true" className="brand-mark" height="34" viewBox="0 0 64 64" width="34">
      <rect className="mark-box" fill="#17211d" height="48" rx="12" width="56" x="4" y="8" />
      <rect className="mark-bar" fill="#f2f2eb" height="8" rx="4" width="32" x="12" y="16" />
      <rect className="mark-accent" fill="#63cf88" height="8" rx="4" width="24" x="12" y="28" />
      <rect className="mark-bar" fill="#f2f2eb" height="8" rx="4" width="40" x="12" y="40" />
    </svg>
  );
}

function GlobalHeader({ authenticated, loginUser, mobileOpen, onMenu, onNavigate, onSignOut, signingOut, onTheme, path, theme }: NavigationProps) {
  const [accountOpen, setAccountOpen] = useState(false);
  const go = (next: RoutePath) => {
    setAccountOpen(false);
    onNavigate(next);
  };
  return (
    <header className="global-header">
      <InternalLink className="brand" onNavigate={onNavigate} to="/">
        <BrandMark />
        <span className="brand-copy">
          <strong>말조심하세욧</strong>
          <span>한국어 욕설 필터 API</span>
        </span>
      </InternalLink>
      <button aria-expanded={mobileOpen} aria-label="메뉴 열기" className="mobile-menu" onClick={onMenu} type="button">
        {mobileOpen ? <X size={22} /> : <List size={22} />}
      </button>
      <div className={mobileOpen ? "global-actions open" : "global-actions"}>
        <nav aria-label="공개 메뉴">
          <NavLink active={path === "/"} label="소개" onNavigate={onNavigate} to="/" />
          <NavLink active={path === "/news"} label="소식" onNavigate={onNavigate} to="/news" />
          <NavLink active={path === "/docs"} label="API 문서" onNavigate={onNavigate} to="/docs" />
          {authenticated && loginUser?.admin ? <NavLink active={path === "/admin"} label="관리자" onNavigate={onNavigate} to="/admin" /> : null}
        </nav>
        <a
          aria-label="GitHub 저장소 열기"
          className="github-link"
          href="https://github.com/Whale0928/profanity-filter-api"
          rel="noopener noreferrer"
          target="_blank"
        >
          <GithubLogo aria-hidden="true" size={20} weight="fill" />
        </a>
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
                <button onClick={() => go("/app/whitelists")} role="menuitem" type="button"><ListChecks size={17} />허용 단어 그룹</button>
                {loginUser?.admin ? <button onClick={() => go("/admin")} role="menuitem" type="button"><ShieldCheck size={17} />관리자 페이지</button> : null}
                <button onClick={() => go("/app/account")} role="menuitem" type="button"><UserCircle size={17} />내 계정</button>
                <button className="sign-out" disabled={signingOut} onClick={onSignOut} role="menuitem" type="button"><SignOut size={17} />{signingOut ? "로그아웃 중" : "로그아웃"}</button>
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
  return (
    <div className="overview-page">
      <section className="intro-page page-width">
        <div className="intro-copy">
          <p className="eyebrow">Korean profanity filter API</p>
          <h1>한국어 욕설·비속어<br />필터 API</h1>
          <p className="lead">검출부터 마스킹까지.<br />서비스에 맞는 방식으로 한국어 문장을 처리하세요.</p>
          <div className="intro-actions">
            <InternalLink className="primary-action" onNavigate={onNavigate} to={authenticated ? "/app/keys" : "/login"}>{authenticated ? "API Key 관리" : "API Key 발급받기"}<ArrowRight size={18} /></InternalLink>
            <InternalLink className="text-action" onNavigate={onNavigate} to="/docs">API 문서 보기</InternalLink>
          </div>
          <p className="intro-caption">Google · GitHub 로그인으로 시작할 수 있습니다.</p>
        </div>
        <FilterExample />
      </section>
      <section className="quickstart page-width" id="credentials">
        <div className="quickstart-copy">
          <p className="eyebrow">첫 API 요청</p>
          <h2>{authenticated ? `${loginUser?.displayName || "개발자"}님, 연동을 시작하세요.` : "API Key 하나로 시작하세요."}</h2>
          <ol className="setup-steps">
            <li><span>01</span><div><h3>API Key 발급</h3><p>로그인한 뒤 용도에 맞는 키를 만드세요.</p></div></li>
            <li><span>02</span><div><h3>요청 헤더에 키 추가</h3><p>발급받은 키를 서버의 환경 변수에 보관하세요.</p></div></li>
            <li><span>03</span><div><h3>모드를 선택하고 호출</h3><p>검출 결과 또는 마스킹한 문장을 받으세요.</p></div></li>
          </ol>
          <p className="future-inline">OAuth2 Client Credentials는 추후 제공 예정입니다.</p>
        </div>
        <RequestExample />
      </section>
    </div>
  );
}

function RequestExample() {
  const [copyStatus, setCopyStatus] = useState("");
  const code = [
    "curl https://api.kr-filter.com/api/v1/filter \\",
    '  -H "Content-Type: application/json" \\',
    '  -H "x-api-key: $API_KEY" \\',
    `  -d '{"text":"안녕하세요","mode":"FILTER"}'`,
  ].join("\n");
  async function copy() {
    try {
      await navigator.clipboard.writeText(code);
      setCopyStatus("요청 예시를 복사했습니다.");
    } catch {
      setCopyStatus("복사하지 못했습니다. 예시를 직접 선택해 복사하세요.");
    }
  }
  return (
    <div className="request-example">
      <div className="example-heading"><span><b>POST</b> /api/v1/filter</span><button aria-label="요청 예시 복사" onClick={() => void copy()} type="button"><Copy size={17} />복사</button></div>
      <pre><code>{code}</code></pre>
      <p className="request-hint"><code>API_KEY</code> 환경 변수를 설정한 터미널에서 실행하세요.</p>
      <p aria-live="polite" className="example-copy-status">{copyStatus}</p>
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
        <p className="lead">로그인하고 API Key를 발급받으세요.<br />발급한 키는 계정에서 관리할 수 있습니다.</p>
      </div>
      <div className="provider-list">
        <button disabled={pending} onClick={() => startSocialLogin("github")} type="button"><GithubLogo size={24} weight="fill" />GitHub로 계속</button>
        <button disabled={pending} onClick={() => startSocialLogin("google")} type="button"><span className="google-mark">G</span>Google로 계속</button>
        <p role={error ? "alert" : "status"}>{error || (pending ? "로그인 상태를 확인하고 있습니다." : "선택한 계정의 로그인 화면으로 이동합니다.")}</p>
        <p className="login-policies">이용 전 <a href="/terms">이용약관</a>과 <a href="/privacy">개인정보 처리방침</a>을 확인해 주세요.</p>
      </div>
    </section>
  );
}

function AccountPage({ accessToken, user }: { accessToken: string | null; user: LoginUser | null }) {
  return (
    <section className="account-page page-width">
      <header className="page-heading"><h1>내 계정</h1><p>SSO에서 확인한 기본 계정 정보입니다.</p></header>
      <div className="account-profile">
        <UserCircle size={64} weight="thin" />
        <dl><div><dt>표시 이름</dt><dd>{user?.displayName ?? "-"}</dd></div><div><dt>Primary email</dt><dd>{user?.email ?? "-"}</dd></div><div><dt>로그인 상태</dt><dd><span className="status-dot" />활성</dd></div></dl>
      </div>
      {accessToken ? <DashboardInquiries accessToken={accessToken} /> : null}
    </section>
  );
}
