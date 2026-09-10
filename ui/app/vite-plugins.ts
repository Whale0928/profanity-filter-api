import { readFileSync } from "node:fs";
import { createRequire } from "node:module";

import type { Plugin } from "vite";

const FONT_PACKAGE = "@fontsource-variable/noto-sans-kr";

/**
 * 첫 화면에 반드시 나오는 글자들이다. 헤더, 내비게이션, 버튼, 소개 문구에서 뽑았다.
 * 이 글자를 담은 서브셋만 미리 받아 두면 첫 페인트부터 제 글꼴로 그려진다.
 * 문구를 크게 바꾸면 이 문자열도 함께 갱신해야 미리 받는 대상이 어긋나지 않는다.
 */
const SHELL_TEXT =
  "말조심하세욧 한국어 욕설·비속어 필터 API 소개 소식 문서 로그인 로그아웃 " +
  "검출 마스킹 필터링 요청 발급받기 보기 개요 명세 이용약관 개인정보 처리방침 " +
  "관리자 계정 다크 라이트 모드 전환 인증 키 목록 재발급 만료 문의 응답 오류 " +
  "한국어 문장의 욕설과 비속어를 검출하고 필요한 방식으로 확인하거나 마스킹하는 API입니다";

function parseRanges(spec: string): Array<[number, number]> {
  return spec.split(",").map((part) => {
    const [from, to] = part.trim().replace(/^u\+/i, "").split("-");
    const start = Number.parseInt(from, 16);
    return [start, to ? Number.parseInt(to, 16) : start];
  });
}

/** SHELL_TEXT 를 그리는 데 필요한 서브셋 이름을 구한다. */
function shellSubsets(): Set<string> {
  const require = createRequire(import.meta.url);
  const unicode = JSON.parse(readFileSync(require.resolve(`${FONT_PACKAGE}/unicode.json`), "utf8")) as Record<string, string>;
  const table = Object.entries(unicode).map(([subset, spec]) => [subset.replace(/^\[|\]$/g, ""), parseRanges(spec)] as const);
  const needed = new Set<string>();
  for (const character of SHELL_TEXT) {
    const code = character.codePointAt(0);
    if (code === undefined) continue;
    // 같은 글자를 여러 서브셋이 담고 있으면 CSS 에서 나중에 선언된 쪽이 이긴다.
    let winner: string | null = null;
    for (const [subset, ranges] of table) {
      if (ranges.some(([low, high]) => code >= low && code <= high)) winner = subset;
    }
    if (winner) needed.add(winner);
  }
  return needed;
}

/**
 * fontsource 는 한글을 서브셋 여러 개로 쪼개 내보내고 모두 font-display: swap 이다.
 * 서브셋이 제각기 도착할 때마다 본문이 다시 그려져 화면이 깜빡이므로 optional 로 바꾼다.
 * optional 은 폰트가 제때 오지 않으면 그 방문에서는 대체 글꼴을 그대로 쓰고 교체하지 않는다.
 * 아래 preloadShellFont 와 짝을 이룬다. 미리 받아 두어야 optional 이 대체 글꼴로 굳지 않는다.
 */
export function fontDisplayOptional(): Plugin {
  return {
    name: "font-display-optional",
    enforce: "pre",
    transform(code, id) {
      if (!id.split("?")[0].endsWith(".css") || !id.includes("@fontsource")) return null;
      if (!code.includes("font-display: swap")) return null;
      return { code: code.replaceAll("font-display: swap", "font-display: optional"), map: null };
    },
  };
}

/**
 * 폰트는 스타일시트를 해석한 뒤에야 요청되기 때문에 첫 페인트에 늦는다.
 * 첫 화면에 필요한 서브셋만 preload 로 앞당긴다. 어차피 받던 파일이라 전송량은 늘지 않는다.
 */
export function preloadShellFont(): Plugin {
  let needed: Set<string> | null = null;
  return {
    name: "preload-shell-font",
    transformIndexHtml: {
      order: "post",
      handler(html, context) {
        needed ??= shellSubsets();
        const hrefs: string[] = [];
        if (context.bundle) {
          for (const name of Object.keys(context.bundle)) {
            const match = /noto-sans-kr-(.+?)-wght-normal-[^.]+\.woff2$/.exec(name);
            if (match && needed.has(match[1])) hrefs.push(`/${name}`);
          }
        } else {
          for (const subset of needed) {
            hrefs.push(`/node_modules/${FONT_PACKAGE}/files/noto-sans-kr-${subset}-wght-normal.woff2`);
          }
        }
        return {
          html,
          tags: hrefs.sort().map((href) => ({
            tag: "link",
            attrs: { rel: "preload", as: "font", type: "font/woff2", href, crossorigin: "" },
            injectTo: "head-prepend" as const,
          })),
        };
      },
    },
  };
}
