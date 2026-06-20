import { lazy, Suspense, useEffect, useRef, useState } from "react";

type PagePath = "/" | `/docs${string}`;

const DocsPage = lazy(() => import("./DocsPage"));

const storyBlocks = [
  {
    id: "block-2-1",
    label: "입력",
    intro: "텍스트를 보냅니다",
    title: "한국어 문장을 그대로 전달",
    body: "댓글, 채팅, 게시글처럼 사용자가 입력한 문장을 API 요청 본문에 담아 보냅니다.",
    visualLabel: "request",
    visualTitle: "POST /api/v1/filter",
    visualLines: ["text", "mode", "X-API-KEY"],
  },
  {
    id: "block-2-2",
    label: "검출",
    intro: "비속어를 찾습니다",
    title: "Aho-Corasick 기반 빠른 매칭",
    body: "등록된 비속어 사전을 기준으로 문장 안의 금칙어를 찾아 위치와 길이를 함께 계산합니다.",
    visualLabel: "detect",
    visualTitle: "욕설 → match",
    visualLines: ["word", "startIndex", "endIndex"],
  },
  {
    id: "block-2-3",
    label: "결과",
    intro: "원하는 방식으로 받습니다",
    title: "검출, 전체 목록, 마스킹",
    body: "QUICK, NORMAL, FILTER 모드로 첫 검출 여부부터 마스킹된 문장까지 필요한 응답만 선택합니다.",
    visualLabel: "response",
    visualTitle: "FILTER",
    visualLines: ["QUICK", "NORMAL", "FILTER"],
  },
  {
    id: "block-2-4",
    label: "연동",
    intro: "서비스 앞단에 붙입니다",
    title: "입력 저장 전에 한 번 호출",
    body: "게시글 저장, 댓글 등록, 채팅 전송 같은 사용자 입력 흐름 앞에 필터링 단계를 추가합니다.",
    visualLabel: "service",
    visualTitle: "before save",
    visualLines: ["comment", "chat", "post"],
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
  const [activeStory, setActiveStory] = useState(storyBlocks[0].id);
  const itemRefs = useRef<Array<HTMLElement | null>>([]);

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        const visibleEntry = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];

        if (visibleEntry instanceof IntersectionObserverEntry) {
          const storyId = visibleEntry.target.getAttribute("data-story");
          if (storyId) {
            setActiveStory(storyId);
          }
        }
      },
      {
        root: null,
        rootMargin: "-22% 0px -22% 0px",
        threshold: [0.35, 0.5, 0.7],
      },
    );

    itemRefs.current.forEach((item) => {
      if (item) {
        observer.observe(item);
      }
    });

    return () => observer.disconnect();
  }, []);

  return (
    <section className="story-section" id="story" aria-labelledby="story-title">
      <div className="story-copy">
        <p className="section-kicker">02</p>
        <h2 id="story-title">한국어 문장을 API로 필터링합니다</h2>
        <div className="story-markers" aria-label="흐름 단계">
          {storyBlocks.map((block, index) => (
            <span
              aria-current={block.id === activeStory ? "step" : undefined}
              key={block.id}
            >
              {String(index + 1).padStart(2, "0")} {block.label}
            </span>
          ))}
        </div>
      </div>
      <div className="story-track">
        {storyBlocks.map((block, index) => (
          <article
            className="story-block"
            data-active={block.id === activeStory}
            data-story={block.id}
            key={block.id}
            ref={(node) => {
              itemRefs.current[index] = node;
            }}
          >
            <p className="story-intro">{block.intro}</p>
            <div className="story-panel">
              <div>
                <span>{block.label}</span>
                <h3>{block.title}</h3>
                <p>{block.body}</p>
              </div>
              <div className="story-visual" aria-hidden="true">
                <span>{block.visualLabel}</span>
                <strong>{block.visualTitle}</strong>
                <div>
                  {block.visualLines.map((line) => (
                    <i key={line}>{line}</i>
                  ))}
                </div>
              </div>
            </div>
          </article>
        ))}
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
