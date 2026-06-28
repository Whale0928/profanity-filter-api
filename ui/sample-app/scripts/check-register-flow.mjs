import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const appRoot = resolve(import.meta.dirname, "..");

const app = readFileSync(resolve(appRoot, "src", "App.tsx"), "utf8");
const pagePath = readFileSync(resolve(appRoot, "src", "constants", "pagePath.ts"), "utf8");
const homePage = readFileSync(resolve(appRoot, "src", "features", "home", "HomePage.tsx"), "utf8");
const registerPage = readFileSync(resolve(appRoot, "src", "features", "register", "RegisterPage.tsx"), "utf8");
const styles = readFileSync(resolve(appRoot, "src", "styles.css"), "utf8");

assert.match(pagePath, /REGISTER_PATH(?:\s*:\s*PagePath)?\s*=\s*"\/register"/, "Register route must be defined as a first-class page path.");
assert.match(app, /<RegisterPage\s+onNavigate=\{navigate\}\s*\/>/, "App must render the register page for the register route.");
assert.match(homePage, /onNavigate\("\/register"\)/, "Home apply CTA must navigate to the register page.");

assert.match(registerPage, /function createLocalApiKey/, "Register page must create an API key locally until the API is connected.");
assert.match(registerPage, /crypto\.getRandomValues/, "Local key issuance must use browser cryptographic randomness.");
assert.match(registerPage, /type="submit"/, "Register page must expose a form submit flow.");
assert.match(registerPage, /인증번호 발송/, "Register page must require an email verification send step.");
assert.match(registerPage, /인증번호 확인/, "Register page must require an email verification confirm step.");
assert.match(registerPage, /emailVerified/, "Register page must track verified email state before issuing a key.");
assert.match(registerPage, /canIssue/, "Register page must gate key issuance on email verification.");
assert.doesNotMatch(registerPage, /const canIssue = Boolean\([^)]*formState\.email\.trim\(\)[^)]*\)/s, "Email input alone must not be enough to issue a key.");
assert.doesNotMatch(registerPage, /\bfetch\s*\(/, "Register page must not send real API requests yet.");
assert.doesNotMatch(registerPage, /\bXMLHttpRequest\b/, "Register page must not use XMLHttpRequest.");
assert.doesNotMatch(registerPage, /https:\/\/api\.kr-filter\.com\/api\/v1\/clients\/register/, "Register page must not hard-code the production register API.");
assert.doesNotMatch(registerPage, /mock|모의/i, "Register page must not present the key issuance flow as fake UI.");
assert.doesNotMatch(registerPage, /API Key|POST \/api\/v1\/clients\/register|ClientsRegistResponse|이름, 이메일, 발급자 정보를 입력해 키를 발급합니다/, "Register page must not show implementation or document-copy text.");
assert.doesNotMatch(registerPage, /responsePreview|<pre>/, "Register page must not show raw response JSON.");
assert.match(registerPage, /navigator\.clipboard\.writeText/, "Issued keys must be copyable.");

assert.match(styles, /\.register-page\b/, "Register page shell must be styled.");
assert.match(styles, /\.register-form\b/, "Register form must be styled.");
assert.match(styles, /\.register-result\b/, "Register result must be styled.");
