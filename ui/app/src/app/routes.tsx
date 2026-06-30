import type { ReactNode } from "react";

import { DOCS_PATH, HOME_PATH, REGISTER_PATH, type PagePath } from "../constants/pagePath";
import { HomePage } from "../features/home/HomePage";
import { RegisterPage } from "../features/register/RegisterPage";

type Navigate = (path: PagePath) => void;

type RouteRenderContext = {
  docsElement: ReactNode;
  navigate: Navigate;
};

export type AppRoute = {
  id: "home" | "register" | "docs";
  label: string;
  matchPath: (pathname: string) => boolean;
  path: PagePath;
  render: (context: RouteRenderContext) => ReactNode;
  showInNavigation: boolean;
};

export const APP_ROUTES: AppRoute[] = [
  {
    id: "home",
    label: "홈",
    matchPath: (pathname) => pathname === HOME_PATH,
    path: HOME_PATH,
    render: ({ navigate }) => <HomePage onNavigate={navigate} />,
    showInNavigation: true,
  },
  {
    id: "register",
    label: "시작",
    matchPath: (pathname) => pathname === REGISTER_PATH,
    path: REGISTER_PATH,
    render: ({ navigate }) => <RegisterPage onNavigate={navigate} />,
    showInNavigation: true,
  },
  {
    id: "docs",
    label: "문서",
    matchPath: (pathname) => pathname === DOCS_PATH || pathname.startsWith(`${DOCS_PATH}/`),
    path: DOCS_PATH,
    render: ({ docsElement }) => docsElement,
    showInNavigation: true,
  },
];

export const HOME_ROUTE = APP_ROUTES[0];

export function getRouteForPathname(pathname: string) {
  return APP_ROUTES.find((route) => route.matchPath(pathname)) ?? HOME_ROUTE;
}

export function getNavigationItems() {
  return APP_ROUTES.filter((route) => route.showInNavigation);
}
