import {
  ArrowRight,
  BookOpen,
  Check,
  Copy,
  GithubLogo,
  Key,
  List,
  LockKey,
  Moon,
  ShieldCheck,
  Sun,
  UserCircle,
  X,
} from "@phosphor-icons/react";
import { useEffect, useMemo, useState } from "react";

import DocsPage from "./docs/DocsPage";

type Theme = "light" | "dark";
type RoutePath = "/" | "/docs" | "/login" | "/app" | "/app/credentials" | "/app/account";
type CredentialKind = "api-key" | "oauth";

const ROUTES: RoutePath[] = ["/", "/docs", "/login", "/app", "/app/credentials", "/app/account"];

function currentPath(): RoutePath {
  const pathname = window.location.pathname;
  return ROUTES.includes(pathname as RoutePath) ? (pathname as RoutePath) : "/";
}

function preferredTheme(): Theme {
  const saved = window.localStorage.getItem("pf-theme");
  if (saved === "light" || saved === "dark") return saved;
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export default function App() {
  const [path, setPath] = useState<RoutePath>(currentPath);
  const [theme, setTheme] = useState<Theme>(preferredTheme);
  const [authenticated, setAuthenticated] = useState(() => window.sessionStorage.getItem("pf-demo-auth") !== "false");
  const [mobileOpen, setMobileOpen] = useState(false);
  const [credentialDialog, setCredentialDialog] = useState<CredentialKind | null>(null);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    window.localStorage.setItem("pf-theme", theme);
  }, [theme]);

  useEffect(() => {
    const onPopState = () => setPath(currentPath());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  useEffect(() => {
    if (!authenticated && path.startsWith("/app")) navigate("/login");
    if (authenticated && path === "/login") navigate("/app");
  }, [authenticated, path]);

  function navigate(next: RoutePath) {
    if (window.location.pathname !== next) window.history.pushState({}, "", next);
    setPath(next);
    setMobileOpen(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function signIn() {
    window.sessionStorage.setItem("pf-demo-auth", "true");
    setAuthenticated(true);
    navigate("/app");
  }

  function signOut() {
    window.sessionStorage.setItem("pf-demo-auth", "false");
    setAuthenticated(false);
    navigate("/");
  }

  const page = useMemo(() => {
    switch (path) {
      case "/docs":
        return <DocsPage theme={theme} />;
      case "/login":
        return <LoginPage onSignIn={signIn} />;
      case "/app":
        return <StartPage onNavigate={navigate} />;
      case "/app/credentials":
        return <CredentialsPage onCreate={setCredentialDialog} />;
      case "/app/account":
        return <AccountPage onSignOut={signOut} />;
      default:
        return <IntroPage onNavigate={navigate} />;
    }
  }, [path]);

  return (
    <div className="app-shell">
      <GlobalHeader
        authenticated={authenticated}
        mobileOpen={mobileOpen}
        onMenu={() => setMobileOpen((open) => !open)}
        onNavigate={navigate}
        onTheme={() => setTheme(theme === "dark" ? "light" : "dark")}
        path={path}
        theme={theme}
      />
      {authenticated ? <AppNavigation onNavigate={navigate} path={path} /> : null}
      <main>{page}</main>
      {credentialDialog ? (
        <CredentialDialog kind={credentialDialog} onClose={() => setCredentialDialog(null)} />
      ) : null}
    </div>
  );
}

type NavigationProps = {
  authenticated: boolean;
  mobileOpen: boolean;
  onMenu: () => void;
  onNavigate: (path: RoutePath) => void;
  onTheme: () => void;
  path: RoutePath;
  theme: Theme;
};

function GlobalHeader({ authenticated, mobileOpen, onMenu, onNavigate, onTheme, path, theme }: NavigationProps) {
  return (
    <header className="global-header">
      <button className="brand" onClick={() => onNavigate("/")} type="button">
        <strong>말조심하세욧</strong>
        <span>한국어 욕설 필터 API</span>
      </button>
      <button aria-expanded={mobileOpen} aria-label="메뉴 열기" className="mobile-menu" onClick={onMenu} type="button">
        {mobileOpen ? <X size={22} /> : <List size={22} />}
      </button>
      <div className={mobileOpen ? "global-actions open" : "global-actions"}>
        <nav aria-label="공개 메뉴">
          <NavButton active={path === "/"} label="소개" onClick={() => onNavigate("/")} />
          <NavButton active={path === "/docs"} label="API 문서" onClick={() => onNavigate("/docs")} />
        </nav>
        <button aria-label={`${theme === "dark" ? "라이트" : "다크"} 모드로 전환`} className="theme-toggle" onClick={onTheme} type="button">
          {theme === "dark" ? <Moon size={17} weight="fill" /> : <Sun size={18} weight="fill" />}
          <i aria-hidden="true" />
        </button>
        {authenticated ? (
          <button className="identity" onClick={() => onNavigate("/app/account")} type="button">
            <span className="avatar">김</span>
            <span>김개발</span>
          </button>
        ) : (
          <button className="login-link" onClick={() => onNavigate("/login")} type="button">로그인</button>
        )}
      </div>
    </header>
  );
}

function AppNavigation({ onNavigate, path }: { onNavigate: (path: RoutePath) => void; path: RoutePath }) {
  return (
    <nav aria-label="로그인 메뉴" className="app-navigation">
      <NavButton active={path === "/app"} label="시작" onClick={() => onNavigate("/app")} />
      <NavButton active={path === "/app/credentials"} label="자격 증명" onClick={() => onNavigate("/app/credentials")} />
      <NavButton active={path === "/app/account"} label="내 계정" onClick={() => onNavigate("/app/account")} />
    </nav>
  );
}

function NavButton({ active, label, onClick }: { active: boolean; label: string; onClick: () => void }) {
  return <button aria-current={active ? "page" : undefined} onClick={onClick} type="button">{label}</button>;
}

function IntroPage({ onNavigate }: { onNavigate: (path: RoutePath) => void }) {
  return (
    <section className="intro-page page-width">
      <p className="eyebrow">Korean profanity filter API</p>
      <h1>사용자 입력을<br />더 안전하게 다룹니다.</h1>
      <p className="lead">한국어 문장의 비속어를 검출하고 필요한 방식으로 마스킹하는 API입니다.</p>
      <div className="intro-actions">
        <button className="primary-action" onClick={() => onNavigate("/login")} type="button">로그인하여 시작 <ArrowRight size={18} /></button>
        <button className="text-action" onClick={() => onNavigate("/docs")} type="button">API 문서 보기</button>
      </div>
      <div className="auth-summary">
        <p>로그인 후 선택할 수 있습니다</p>
        <div><Key size={24} /><span><b>API Key</b>빠르고 단순한 연동</span></div>
        <div><LockKey size={24} /><span><b>OAuth2 Client Credentials</b>운영·서버 간 연동 권장</span></div>
      </div>
    </section>
  );
}

function LoginPage({ onSignIn }: { onSignIn: () => void }) {
  return (
    <section className="login-page page-width">
      <div>
        <p className="eyebrow">Sign in</p>
        <h1>계정으로 시작하세요.</h1>
        <p className="lead">자격 증명은 Google 또는 GitHub로 로그인한 사용자만 만들고 관리할 수 있습니다.</p>
      </div>
      <div className="provider-list">
        <button onClick={onSignIn} type="button"><GithubLogo size={24} weight="fill" />GitHub로 계속</button>
        <button onClick={onSignIn} type="button"><span className="google-mark">G</span>Google로 계속</button>
        <p>프로토타입에서는 실제 OAuth 요청을 보내지 않습니다.</p>
      </div>
    </section>
  );
}

function StartPage({ onNavigate }: { onNavigate: (path: RoutePath) => void }) {
  return (
    <section className="start-page page-width">
      <p className="eyebrow">시작</p>
      <h1>반갑습니다, 김개발님.</h1>
      <p className="lead">연동 환경에 맞는 자격 증명을 선택하고 API 문서에서 요청 방식을 확인하세요.</p>
      <div className="next-actions">
        <button onClick={() => onNavigate("/app/credentials")} type="button"><Key size={22} /><span><b>자격 증명 선택</b>API Key와 OAuth2 방식을 비교합니다.</span><ArrowRight size={19} /></button>
        <button onClick={() => onNavigate("/docs")} type="button"><BookOpen size={22} /><span><b>API 문서 보기</b>인증과 요청 형식을 확인합니다.</span><ArrowRight size={19} /></button>
      </div>
    </section>
  );
}

function CredentialsPage({ onCreate }: { onCreate: (kind: CredentialKind) => void }) {
  return (
    <section className="credentials-page page-width">
      <header className="page-heading">
        <h1>자격 증명</h1>
        <p>모든 연동은 Google 또는 GitHub 계정으로 로그인해야 사용할 수 있습니다.</p>
      </header>
      <div className="credential-grid">
        <CredentialMethod
          action="API Key 만들기"
          code={'curl -X POST https://api.kr-filter.com/api/v1/filter \\\n  -H "Content-Type: application/json" \\\n  -H "x-api-key: $API_KEY"'}
          description="발급된 키를 요청 헤더에 포함하는 가장 단순한 인증 방식입니다. 이 화면에는 키 원문을 표시하지 않습니다."
          icon={<Key size={28} />}
          kind="api-key"
          points={["로그인 후 API Key 발급", "x-api-key 헤더로 API 호출", "재발급하면 이전 키는 즉시 무효화"]}
          requestLabel="API 요청 예시"
          subtitle="빠르고 단순한 연동"
          title="API Key"
          onCreate={onCreate}
        />
        <CredentialMethod
          action="OAuth 클라이언트 만들기"
          code={'curl -X POST https://api.kr-filter.com/oauth2/token \\\n  -u "$CLIENT_ID:$CLIENT_SECRET" \\\n  -d "grant_type=client_credentials"'}
          description="클라이언트 자격 증명으로 access token을 발급받고, 그 token을 API 요청에 사용하는 서버 간 인증 흐름입니다."
          icon={<LockKey size={28} />}
          kind="oauth"
          points={["클라이언트 자격 증명으로 token 요청", "발급된 token을 Bearer 헤더로 전달", "만료 시 token을 다시 발급"]}
          recommended
          requestLabel="Token 요청 예시"
          subtitle="운영·서버 간 연동 권장"
          title="OAuth2 Client Credentials"
          onCreate={onCreate}
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
    <article className="credential-method">
      <div className="method-title"><span className="method-icon">{props.icon}</span><h2>{props.title}</h2>{props.recommended ? <em>운영 권장</em> : null}</div>
      <p className="method-subtitle">{props.subtitle}</p>
      <p className="method-description">{props.description}</p>
      <label>{props.requestLabel}</label>
      <button aria-label={`${props.requestLabel} 복사`} className="code-example" onClick={copyExample} type="button"><code>{props.code}</code>{copied ? <Check size={19} /> : <Copy size={19} />}</button>
      <ul>{props.points.map((point) => <li key={point}><Check size={18} />{point}</li>)}</ul>
      <button className="create-action" onClick={() => props.onCreate(props.kind)} type="button">{props.action}<ArrowRight size={19} /></button>
    </article>
  );
}

function AccountPage({ onSignOut }: { onSignOut: () => void }) {
  return (
    <section className="account-page page-width">
      <header className="page-heading"><h1>내 계정</h1><p>SSO에서 확인한 기본 계정 정보입니다.</p></header>
      <div className="account-profile">
        <UserCircle size={64} weight="thin" />
        <dl><div><dt>표시 이름</dt><dd>김개발</dd></div><div><dt>Primary email</dt><dd>dev.kim@example.com</dd></div><div><dt>로그인 상태</dt><dd><span className="status-dot" />활성</dd></div></dl>
      </div>
      <button className="secondary-action" onClick={onSignOut} type="button">프로토타입 세션 종료</button>
    </section>
  );
}

function CredentialDialog({ kind, onClose }: { kind: CredentialKind; onClose: () => void }) {
  const name = kind === "api-key" ? "API Key" : "OAuth2 Client Credentials";
  return (
    <div className="dialog-backdrop" onMouseDown={onClose} role="presentation">
      <section aria-labelledby="dialog-title" aria-modal="true" className="dialog" onMouseDown={(event) => event.stopPropagation()} role="dialog">
        <button aria-label="닫기" className="dialog-close" onClick={onClose} type="button"><X size={20} /></button>
        <span className="dialog-icon">{kind === "api-key" ? <Key size={30} /> : <LockKey size={30} />}</span>
        <h2 id="dialog-title">{name} 만들기</h2>
        <p>이 화면은 발급 흐름을 확인하는 프로토타입입니다. API가 연결되기 전에는 자격 증명을 생성하거나 전송하지 않습니다.</p>
        <button className="primary-action" onClick={onClose} type="button">확인</button>
      </section>
    </div>
  );
}
