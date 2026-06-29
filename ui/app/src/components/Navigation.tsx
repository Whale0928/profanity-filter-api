import type { PagePath } from "../constants/pagePath";
import { getNavigationItems } from "../app/routes";

type NavigationProps = {
  onNavigate: (path: PagePath) => void;
  pagePath: PagePath;
};

export function Navigation({ onNavigate, pagePath }: NavigationProps) {
  const navigationItems = getNavigationItems();

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
        {navigationItems.map((item) => (
          <a
            aria-current={item.matchPath(pagePath) ? "page" : undefined}
            href={item.path}
            key={item.id}
            onClick={(event) => {
              event.preventDefault();
              onNavigate(item.path);
            }}
          >
            {item.label}
          </a>
        ))}
      </nav>
    </header>
  );
}
