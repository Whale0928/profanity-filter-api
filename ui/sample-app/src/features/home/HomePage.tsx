import { useState } from "react";
import type { PagePath } from "../../constants/pagePath";
import { IDENTITY_OPTIONS, SCENARIO_STEPS } from "../../constants/landingContent";

type HomePageProps = {
  onNavigate: (path: PagePath) => void;
};

export function HomePage({ onNavigate }: HomePageProps) {
  return (
    <>
      <Hero onNavigate={onNavigate} />
      <StoryBlocks />
      <StartGuide />
      <FooterCta onNavigate={onNavigate} />
    </>
  );
}

function Hero({ onNavigate }: HomePageProps) {
  return (
    <section className="landing-hero" id="top">
      <div className="hero-grid" aria-hidden="true" />
      <p className="hero-kicker">profanity-filter application</p>
      <h1>말조심하세욧</h1>
      <p className="hero-description">한국어 비속어 필터 API.</p>
      <div className="hero-actions">
        <a
          className="primary-link"
          href="/register"
          onClick={(event) => {
            event.preventDefault();
            onNavigate("/register");
          }}
        >
          시작하기
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
  const [activeIdentityId, setActiveIdentityId] = useState<typeof IDENTITY_OPTIONS[number]["id"]>(IDENTITY_OPTIONS[0].id);
  const activeIdentity =
    IDENTITY_OPTIONS.find((option) => option.id === activeIdentityId) ?? IDENTITY_OPTIONS[0];

  return (
    <section className="story-section" id="story" aria-labelledby="story-title">
      <div className="story-copy">
        <p className="section-kicker">02</p>
        <h2 id="story-title">한국어 문장을 API로 필터링합니다</h2>
        <div className="identity-tabs" role="tablist" aria-label="2번 블록 메시지 타입">
          {IDENTITY_OPTIONS.map((option) => (
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
          {SCENARIO_STEPS.map((step) => (
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

function FooterCta({ onNavigate }: HomePageProps) {
  return (
    <section className="footer-cta">
      <div className="footer-actions">
        <button className="primary-link" onClick={() => onNavigate("/register")} type="button">
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
