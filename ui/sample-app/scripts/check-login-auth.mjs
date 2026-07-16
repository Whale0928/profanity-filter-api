import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const appRoot = resolve(import.meta.dirname, "..");
const loginPage = readFileSync(resolve(appRoot, "src", "features", "login", "LoginPage.tsx"), "utf8");
const packageJson = JSON.parse(readFileSync(resolve(appRoot, "package.json"), "utf8"));

assert.match(loginPage, /VITE_API_BASE_URL/, "Login API base URL must support a Vite environment override.");
assert.match(loginPage, /DEFAULT_API_BASE_URL = "http:\/\/localhost:8080"/, "Local login development must keep the existing API default.");

assert.match(loginPage, /params\.getAll\("code"\)/, "OAuth callback must consume only the one-time code fragment.");
assert.match(loginPage, /window\.history\.replaceState/, "OAuth callback code must be removed from browser history immediately.");
assert.match(loginPage, /\/api\/v1\/auth\/exchange/, "Login callback code must be exchanged through the auth API.");
assert.match(loginPage, /body: JSON\.stringify\(\{ code \}\)/, "Exchange must send the callback code in a JSON request body.");

assert.match(loginPage, /\/api\/v1\/auth\/csrf/, "Session restoration must obtain a CSRF token first.");
assert.match(loginPage, /\/api\/v1\/auth\/refresh/, "Session restoration must rotate the refresh cookie through the refresh API.");
assert.match(loginPage, /refreshFlightRef\.current/, "Refresh requests must share one in-flight promise.");
assert.match(loginPage, /if \(refreshFlightRef\.current\)/, "Refresh must return the existing in-flight request before starting another rotation.");
assert.match(loginPage, /credentials: "include"/, "Every auth request must include the HttpOnly refresh cookie managed by the browser.");

assert.match(loginPage, /useState<string \| null>\(null\)/, "The access token must live only in React state.");
assert.match(loginPage, /\/api\/v1\/auth\/me/, "The issued access token must be verified through the current-user endpoint.");
assert.match(loginPage, /Authorization: `\$\{tokenType\} \$\{accessToken\}`/, "Current-user requests must authenticate with the in-memory access token.");

assert.doesNotMatch(loginPage, /\blocalStorage\b/, "Login credentials must never be stored in localStorage.");
assert.doesNotMatch(loginPage, /\bsessionStorage\b/, "Login credentials must never be stored in sessionStorage.");
assert.doesNotMatch(loginPage, /document\.cookie/, "JavaScript must not read or write the refresh cookie.");
assert.doesNotMatch(loginPage, /console\.(?:log|info|debug|warn|error)/, "Login credentials and provider responses must not be logged.");
assert.doesNotMatch(loginPage, /<pre\b/, "The login page must not render a raw API or provider response.");
assert.doesNotMatch(loginPage, /loginResult|formattedResult|OAuthLoginResult/, "The old raw callback result renderer must stay removed.");
assert.match(loginPage, /authView\.user\.displayName/, "Authenticated UI must render only the normalized user view.");
assert.match(loginPage, /email: string;/, "Authenticated user email must be required by the UI contract.");
assert.match(loginPage, /email: readRequiredString\(record, "email"\)/, "Login responses must include a non-empty email.");
assert.doesNotMatch(loginPage, /공개된 이메일 없음/, "The login UI must not keep a nullable email fallback.");
assert.match(loginPage, /toSafeAvatarUrl\(authView\.user\.avatarUrl\)/, "Nullable avatar URLs must be normalized before rendering.");

assert.equal(packageJson.scripts["test:login"], "node scripts/check-login-auth.mjs", "Package scripts must expose the login invariant check.");
assert.match(packageJson.scripts.test, /test:login/, "The default npm test command must include the login invariant check.");

console.log("Login authentication invariants verified.");
