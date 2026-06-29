import { useCallback, useEffect, useRef, useState } from "react";

export type ClipboardStatus = "idle" | "copied" | "failed";

export function useClipboard(resetAfterMs = 1400) {
  const [status, setStatus] = useState<ClipboardStatus>("idle");
  const resetTimer = useRef<number | null>(null);

  const clearResetTimer = useCallback(() => {
    if (resetTimer.current !== null) {
      window.clearTimeout(resetTimer.current);
      resetTimer.current = null;
    }
  }, []);

  const reset = useCallback(() => {
    clearResetTimer();
    setStatus("idle");
  }, [clearResetTimer]);

  const copy = useCallback(
    async (value: string) => {
      clearResetTimer();

      try {
        if (!navigator.clipboard) {
          throw new Error("Clipboard API is not available.");
        }

        await navigator.clipboard.writeText(value);
        setStatus("copied");
        resetTimer.current = window.setTimeout(() => setStatus("idle"), resetAfterMs);
        return true;
      } catch {
        setStatus("failed");
        resetTimer.current = window.setTimeout(() => setStatus("idle"), resetAfterMs);
        return false;
      }
    },
    [clearResetTimer, resetAfterMs],
  );

  useEffect(() => () => clearResetTimer(), [clearResetTimer]);

  return {
    copy,
    reset,
    status,
  };
}
