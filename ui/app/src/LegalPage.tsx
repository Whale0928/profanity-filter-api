import { legalReviewNotice, privacySections, termsSections } from "./legalContent";

export default function LegalPage({ kind }: { kind: "privacy" | "terms" }) {
  const privacy = kind === "privacy";
  const sections = privacy ? privacySections : termsSections;
  return (
    <article className="legal-page page-width">
      <header><p className="eyebrow">서비스 정책 · 검토 초안</p><h1>{privacy ? "개인정보 처리방침" : "이용약관"}</h1><p className="legal-intro">{privacy ? "서비스에서 처리하는 정보와 이용자의 권리를 안내합니다." : "서비스 이용에 필요한 기준과 서로의 책임을 안내합니다."}</p></header>
      <p className="legal-review" role="note">{legalReviewNotice}</p>
      <nav className="legal-toc" aria-label="문서 목차">{sections.map((section, index) => <a href={`#legal-${index + 1}`} key={section.title}>{section.title}</a>)}</nav>
      {sections.map((section, index) => <section id={`legal-${index + 1}`} key={section.title}><h2>{section.title}</h2>{section.paragraphs.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}</section>)}
    </article>
  );
}
