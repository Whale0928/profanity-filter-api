import { lazy, Suspense, useEffect, useState } from "react";

type PagePath = "/" | `/docs${string}`;

const DocsPage = lazy(() => import("./DocsPage"));

const identityOptions = [
  {
    id: "free-practical",
    tabLabel: "무료/실용",
    label: "작은 서비스",
    intro: "비용 때문에 입력 안전망을 미루지 않게",
    title: "작은 서비스도 한국어 입력을 안전하게 다룰 수 있게",
    body: "포트폴리오, 학습, 비영리 서비스가 댓글과 게시글 저장 전에 기본 비속어 필터링을 붙일 수 있도록 만든 무료 REST API입니다.",
    points: ["무료로 시작", "키 발급 후 호출", "현실적인 첫 안전망"],
    proofItems: ["키 발급 후 바로 호출", "문서 보고 붙이기", "상업용은 KISO 권장"],
    visualLabel: "추천 대상",
    visualTitle: "무료로 시작하는 입력 안전망",
    visualLines: ["학습 서비스", "비영리 프로젝트", "사이드 프로젝트"],
  },
  {
    id: "korean-focused",
    tabLabel: "한국어 중심",
    label: "한국어 필터",
    intro: "영어권 moderation이 놓치는 한국어 입력을 기준으로",
    title: "한국어 비속어 사전을 빠르게 검색합니다",
    body: "Aho-Corasick Trie로 등록된 비속어 목록을 한 번에 매칭하고, 초성/한글/영문이 섞인 사용자 입력에서도 검출 위치를 계산합니다.",
    points: ["빠른 사전 검색", "검출부터 마스킹까지", "단어 목록 동기화"],
    proofItems: ["첫 단어만 빠르게 확인", "전체 검출", "마스킹 문장 반환"],
    visualLabel: "검출 기준",
    visualTitle: "한국어 문장을 기준으로",
    visualLines: ["초성 입력", "한글 문장", "영문 혼합"],
  },
  {
    id: "developer-integration",
    tabLabel: "개발자 연동",
    label: "REST API",
    intro: "서비스 흐름을 크게 바꾸지 않고",
    title: "저장 전에 API 한 번으로 붙입니다",
    body: "댓글, 채팅, 게시글 저장 직전에 필터 API를 호출하고, 검출 결과와 마스킹된 문장, 응답 코드를 확인하면 됩니다.",
    points: ["문장 전달", "동기 또는 콜백 처리", "문서 기반 연동"],
    proofItems: ["필터 API 호출", "문장 전달", "응답 코드 확인", "키로 인증"],
    visualLabel: "연동 흐름",
    visualTitle: "저장 전에 한 번 확인",
    visualLines: ["입력 받기", "필터 호출", "결과 반영"],
  },
];

const scenarioSteps = [
  {
    id: "scenario-1",
    label: "01",
    title: "사용자가 댓글을 입력",
    body: "서비스는 저장 전에 필터 API를 호출합니다.",
  },
  {
    id: "scenario-2",
    label: "02",
    title: "FILTER 모드로 마스킹",
    body: "비속어 위치를 찾고 노출 가능한 문장으로 바꿉니다.",
  },
  {
    id: "scenario-3",
    label: "03",
    title: "정리된 문장을 저장",
    body: "응답 결과를 댓글, 채팅, 게시글 흐름에 그대로 반영합니다.",
  },
];

function getPagePath(): PagePath {
  return window.location.pathname.startsWith("/docs")
    ? (window.location.pathname as PagePath)
    : "/";
}

export default function App() {
  const [pagePath, setPagePath] = useState<PagePath>(getPagePath);

  useEffect(() => {
    window.history.scrollRestoration = "manual";
    const handlePopState = () => setPagePath(getPagePath());
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, [pagePath]);

  const navigate = (path: PagePath) => {
    if (window.location.pathname !== path) {
      window.history.pushState({}, "", path);
    }
    setPagePath(path);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <main>
      <Navigation onNavigate={navigate} pagePath={pagePath} />
      {pagePath.startsWith("/docs") ? (
        <Suspense fallback={<DocsFallback />}>
          <DocsPage currentPath={pagePath} onNavigate={navigate} />
        </Suspense>
      ) : (
        <HomePage onNavigate={navigate} />
      )}
    </main>
  );
}

function Navigation({
  onNavigate,
  pagePath,
}: {
  onNavigate: (path: PagePath) => void;
  pagePath: PagePath;
}) {
  return (
    <header className="site-nav">
      <a
        className="brand"
        href="/"
        onClick={(event) => {
          event.preventDefault();
          onNavigate("/");
        }}
      >
        말조심하세욧
      </a>
      <nav aria-label="primary">
        <a
          aria-current={pagePath === "/" ? "page" : undefined}
          href="/"
          onClick={(event) => {
            event.preventDefault();
            onNavigate("/");
          }}
        >
          홈
        </a>
        <a
          aria-current={pagePath.startsWith("/docs") ? "page" : undefined}
          href="/docs"
          onClick={(event) => {
            event.preventDefault();
            onNavigate("/docs");
          }}
        >
          문서
        </a>
      </nav>
    </header>
  );
}

function HomePage({ onNavigate }: { onNavigate: (path: PagePath) => void }) {
  return (
    <>
      <Hero onNavigate={onNavigate} />
      <StoryBlocks />
      <StartGuide />
      <FooterCta onNavigate={onNavigate} />
    </>
  );
}

function Hero({ onNavigate }: { onNavigate: (path: PagePath) => void }) {
  return (
    <section className="landing-hero" id="top">
      <div className="hero-grid" aria-hidden="true" />
      <p className="hero-kicker">profanity-filter application</p>
      <h1>말조심하세욧</h1>
      <p className="hero-description">한국어 비속어 필터 API.</p>
      <div className="hero-actions">
        <a className="primary-link" href="#story">
          흐름 보기
        </a>
        <a
          className="secondary-link"
          href="/docs"
          onClick={(event) => {
            event.preventDefault();
            onNavigate("/docs");
          }}
        >
          문서 보기
        </a>
      </div>
    </section>
  );
}

function StoryBlocks() {
  const [activeIdentityId, setActiveIdentityId] = useState(identityOptions[0].id);
  const activeIdentity =
    identityOptions.find((option) => option.id === activeIdentityId) ?? identityOptions[0];

  return (
    <section className="story-section" id="story" aria-labelledby="story-title">
      <div className="story-copy">
        <p className="section-kicker">02</p>
        <h2 id="story-title">한국어 문장을 API로 필터링합니다</h2>
        <div className="identity-tabs" role="tablist" aria-label="2번 블록 메시지 타입">
          {identityOptions.map((option) => (
            <button
              aria-controls="identity-panel"
              aria-selected={option.id === activeIdentity.id}
              id={`${option.id}-tab`}
              key={option.id}
              onClick={() => setActiveIdentityId(option.id)}
              role="tab"
              type="button"
            >
              {option.tabLabel}
            </button>
          ))}
        </div>
      </div>
      <div className="story-track">
        <article
          aria-labelledby={`${activeIdentity.id}-tab`}
          className="story-block"
          data-active="true"
          id="identity-panel"
          role="tabpanel"
        >
          <p className="story-intro">{activeIdentity.intro}</p>
          <div className="story-panel">
            <div>
              <span>{activeIdentity.label}</span>
              <h3>{activeIdentity.title}</h3>
              <p>{activeIdentity.body}</p>
              <div className="identity-points">
                {activeIdentity.points.map((point) => (
                  <b key={point}>{point}</b>
                ))}
              </div>
            </div>
            <div className="story-visual" aria-hidden="true">
              <span>{activeIdentity.visualLabel}</span>
              <strong>{activeIdentity.visualTitle}</strong>
              <div>
                {activeIdentity.visualLines.map((line) => (
                  <i key={line}>{line}</i>
                ))}
              </div>
            </div>
            <div className="identity-proof-row" aria-label="선택한 메시지 근거">
              {activeIdentity.proofItems.map((item) => (
                <code key={item}>{item}</code>
              ))}
            </div>
          </div>
        </article>
      </div>
    </section>
  );
}

function StartGuide() {
  return (
    <section className="start-section" id="scenario" aria-labelledby="start-title">
      <div className="section-heading">
        <p className="section-kicker">03</p>
        <h2 id="start-title">사용 시나리오</h2>
        <p>
          댓글, 채팅, 게시글 입력 전에 API를 호출해 비속어를 검출하거나 마스킹합니다.
        </p>
      </div>
      <div className="scenario-layout">
        <div className="scenario-video" aria-label="필터링 처리 흐름">
          <div className="scenario-screen">
            <div className="comment-card">
              <span>comment</span>
              <p>이 댓글은 욕설을 포함합니다.</p>
            </div>
            <div className="api-card">
              <span>POST /api/v1/filter</span>
              <strong>FILTER</strong>
            </div>
            <div className="result-card">
              <span>response</span>
              <p>이 댓글은 **을 포함합니다.</p>
            </div>
          </div>
          <div className="scenario-timeline" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
        </div>
        <div className="scenario-steps">
          {scenarioSteps.map((step) => (
            <article className="step-card" key={step.id}>
              <span>{step.label}</span>
              <h3>{step.title}</h3>
              <p>{step.body}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function DocsFallback() {
  return (
    <section className="docs-loading" aria-label="문서 로딩" role="status">
      문서 로딩 중
    </section>
  );
}

function FooterCta({ onNavigate }: { onNavigate: (path: PagePath) => void }) {
  return (
    <section className="footer-cta">
      <div className="footer-actions">
        <button className="primary-link" type="button">
          신청하기
        </button>
        <button
          className="secondary-link"
          onClick={() => onNavigate("/docs")}
          type="button"
        >
          문서 보기
        </button>
      </div>
    </section>
  );
}
