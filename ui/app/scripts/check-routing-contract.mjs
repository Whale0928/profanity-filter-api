import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const appRoot = resolve(import.meta.dirname, "..");

const routes = readFileSync(resolve(appRoot, "src", "app", "routes.tsx"), "utf8");
const app = readFileSync(resolve(appRoot, "src", "App.tsx"), "utf8");
const navigation = readFileSync(resolve(appRoot, "src", "components", "Navigation.tsx"), "utf8");
const pagePath = readFileSync(resolve(appRoot, "src", "constants", "pagePath.ts"), "utf8");

assert.match(routes, /export const APP_ROUTES/, "Routes must be centralized in APP_ROUTES.");
assert.match(routes, /showInNavigation:\s*true/, "Navigation visibility must be route metadata.");
assert.match(routes, /matchPath/, "Each route must expose a matchPath contract.");
assert.match(routes, /pathname === DOCS_PATH \|\| pathname\.startsWith\(`\$\{DOCS_PATH\}\/`\)/, "Docs route must not match docs-admin style paths.");
assert.match(routes, /getRouteForPathname/, "Route selection must be centralized.");
assert.match(routes, /getNavigationItems/, "Navigation item selection must be centralized.");

assert.match(app, /currentRoute\.render/, "App must render through route registry.");
assert.doesNotMatch(app, /pagePath\.startsWith\("\/docs"\)/, "App must not hard-code docs prefix matching.");
assert.doesNotMatch(app, /pagePath === "\/register"/, "App must not hard-code register routing.");

assert.match(navigation, /getNavigationItems/, "Navigation must read menu items from route registry.");
assert.doesNotMatch(navigation, /href="\/register"[\s\S]*href="\/docs"/, "Navigation must not hard-code menu anchors.");

assert.match(pagePath, /DOCS_PATH/, "Page path constants must expose DOCS_PATH.");
assert.doesNotMatch(pagePath, /pathname\.startsWith\(DOCS_PREFIX\)/, "Page path parsing must not use broad docs prefix matching.");
