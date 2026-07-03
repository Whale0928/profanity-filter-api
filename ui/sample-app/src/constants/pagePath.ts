export type PagePath = "/" | "/login" | "/register" | `/docs${string}`;

export const HOME_PATH: PagePath = "/";
export const DOCS_PREFIX = "/docs";
export const LOGIN_PATH: PagePath = "/login";
export const REGISTER_PATH: PagePath = "/register";

export function getCurrentPagePath(pathname: string): PagePath {
  if (pathname.startsWith(DOCS_PREFIX)) {
    return pathname as PagePath;
  }

  if (pathname === LOGIN_PATH) {
    return LOGIN_PATH;
  }

  if (pathname === REGISTER_PATH) {
    return REGISTER_PATH;
  }

  return HOME_PATH;
}
