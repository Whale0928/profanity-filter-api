export type PagePath = "/" | `/docs${string}`;

export const HOME_PATH: PagePath = "/";
export const DOCS_PREFIX = "/docs";

export function getCurrentPagePath(pathname: string): PagePath {
  return pathname.startsWith(DOCS_PREFIX) ? (pathname as PagePath) : HOME_PATH;
}
