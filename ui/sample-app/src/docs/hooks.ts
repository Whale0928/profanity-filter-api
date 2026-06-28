import { useEffect, useState } from "react";

import type { MarkdownState } from "./types";

export function useMarkdownDocument(url: string, fallback: string): MarkdownState {
  const [state, setState] = useState<MarkdownState>({
    content: fallback,
    source: "fallback",
    url: "",
  });

  useEffect(() => {
    let active = true;

    async function loadMarkdown() {
      try {
        const response = await fetch(`${url}?v=${Date.now()}`);
        if (response.ok) {
          const content = await response.text();
          if (active && content.trim()) {
            setState({ content, source: "local", url });
            return;
          }
        }
      } catch {
        // local 문서 fetch 실패 시 fallback을 유지해 페이지를 계속 표시한다.
      }
      if (active) {
        setState({ content: fallback, source: "fallback", url: "" });
      }
    }

    loadMarkdown();
    return () => {
      active = false;
    };
  }, [fallback, url]);

  return state;
}
