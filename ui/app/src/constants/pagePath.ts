export type PagePath = "/" | "/register" | "/docs" | `/docs/${string}`;

export const HOME_PATH: PagePath = "/";
export const DOCS_PATH: PagePath = "/docs";
export const REGISTER_PATH: PagePath = "/register";

export function getCurrentPagePath(pathname: string): PagePath {
  if (pathname === DOCS_PATH || pathname.startsWith(`${DOCS_PATH}/`)) {
    return pathname as PagePath;
  }

  if (pathname === REGISTER_PATH) {
    return REGISTER_PATH;
  }

  return HOME_PATH;
}
