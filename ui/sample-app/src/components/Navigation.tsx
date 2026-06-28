import type { PagePath } from "../constants/pagePath";

type NavigationProps = {
  onNavigate: (path: PagePath) => void;
  pagePath: PagePath;
};

export function Navigation({ onNavigate, pagePath }: NavigationProps) {
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
          aria-current={pagePath === "/register" ? "page" : undefined}
          href="/register"
          onClick={(event) => {
            event.preventDefault();
            onNavigate("/register");
          }}
        >
          시작
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
