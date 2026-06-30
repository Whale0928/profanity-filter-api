import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

const appRoot = resolve(import.meta.dirname, "..");

const navigationHookPath = resolve(appRoot, "src", "hooks", "useAppNavigation.ts");
const clipboardHookPath = resolve(appRoot, "src", "hooks", "useClipboard.ts");

assert.ok(existsSync(navigationHookPath), "useAppNavigation hook must exist.");
assert.ok(existsSync(clipboardHookPath), "useClipboard hook must exist.");

const navigationHook = readFileSync(navigationHookPath, "utf8");
const clipboardHook = readFileSync(clipboardHookPath, "utf8");
const app = readFileSync(resolve(appRoot, "src", "App.tsx"), "utf8");
const docsPage = readFileSync(resolve(appRoot, "src", "DocsPage.tsx"), "utf8");
const registerPage = readFileSync(resolve(appRoot, "src", "features", "register", "RegisterPage.tsx"), "utf8");

assert.match(navigationHook, /export function useAppNavigation/, "Navigation hook must export useAppNavigation.");
assert.match(navigationHook, /addEventListener\("popstate"/, "Navigation hook must own popstate synchronization.");
assert.match(navigationHook, /scrollRestoration\s*=\s*"manual"/, "Navigation hook must own scroll restoration.");
assert.match(navigationHook, /getRouteForPathname/, "Navigation hook must use route registry.");
assert.match(navigationHook, /navigate/, "Navigation hook must return a navigate command.");

assert.match(clipboardHook, /export function useClipboard/, "Clipboard hook must export useClipboard.");
assert.match(clipboardHook, /ClipboardStatus\s*=\s*"idle" \| "copied" \| "failed"/, "Clipboard state must include failed status.");
assert.match(clipboardHook, /try\s*{[\s\S]*writeText[\s\S]*}\s*catch/s, "Clipboard writes must handle failures.");

assert.match(app, /useAppNavigation/, "App must use navigation hook.");
assert.doesNotMatch(app, /addEventListener\("popstate"/, "App must not own popstate directly.");

assert.match(docsPage, /useClipboard/, "Docs page must use clipboard hook.");
assert.match(registerPage, /useClipboard/, "Register page must use clipboard hook.");
assert.doesNotMatch(docsPage, /navigator\.clipboard\.writeText/, "Docs page must not call clipboard directly.");
assert.doesNotMatch(registerPage, /navigator\.clipboard\.writeText/, "Register page must not call clipboard directly.");
