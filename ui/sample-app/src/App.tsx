import { lazy, Suspense, useEffect, useRef, useState } from "react";

type PagePath = "/" | `/docs${string}`;

const DocsPage = lazy(() => import("./DocsPage"));

const storyBlocks = [
  {
    id: "block-2-1",
    label: "2-1",
    intro: "2번 블럭",
    title: "2번 블럭",
    body: "2번 블럭",
    visualLabel: "2-1",
    visualTitle: "2번 블럭",
    visualLines: ["2-1", "2-2", "2-3"],
  },
  {
    id: "block-2-2",
    label: "2-2",
    intro: "2번 블럭",
    title: "2번 블럭",
    body: "2번 블럭",
    visualLabel: "2-2",
    visualTitle: "2번 블럭",
    visualLines: ["2-1", "2-2", "2-3"],
  },
  {
    id: "block-2-3",
    label: "2-3",
    intro: "2번 블럭",
    title: "2번 블럭",
    body: "2번 블럭",
    visualLabel: "2-3",
    visualTitle: "2번 블럭",
    visualLines: ["2-1", "2-2", "2-3"],
  },
  {
    id: "block-2-4",
    label: "2-4",
    intro: "2번 블럭",
    title: "2번 블럭",
    body: "2번 블럭",
    visualLabel: "2-4",
    visualTitle: "2번 블럭",
    visualLines: ["2-1", "2-2", "2-3"],
  },
];

const startSteps = [
  {
    id: "block-3-1",
    title: "3번 블럭",
    body: "3번 블럭",
  },
  {
    id: "block-3-2",
    title: "3번 블럭",
    body: "3번 블럭",
  },
  {
    id: "block-3-3",
    title: "3번 블럭",
    body: "3번 블럭",
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
        <h2 id="story-title">2번 블럭</h2>
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
    <section className="start-section" aria-labelledby="start-title">
      <div className="section-heading">
        <p className="section-kicker">03</p>
        <h2 id="start-title">3번 블럭</h2>
      </div>
      <div className="step-grid">
        {startSteps.map((step, index) => (
          <article className="step-card" key={step.id}>
            <span>{String(index + 1).padStart(2, "0")}</span>
            <h3>{step.title}</h3>
            <p>{step.body}</p>
          </article>
        ))}
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
