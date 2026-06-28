export type PagePath = "/" | "/register" | `/docs${string}`;

export const HOME_PATH: PagePath = "/";
export const DOCS_PREFIX = "/docs";
export const REGISTER_PATH: PagePath = "/register";

export function getCurrentPagePath(pathname: string): PagePath {
  if (pathname.startsWith(DOCS_PREFIX)) {
    return pathname as PagePath;
  }

  if (pathname === REGISTER_PATH) {
    return REGISTER_PATH;
  }

  return HOME_PATH;
}
