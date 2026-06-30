import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

const uiRoot = resolve(import.meta.dirname, "..", "..");
const appRoot = resolve(import.meta.dirname, "..");

assert.equal(appRoot.endsWith(`${resolve(uiRoot, "app")}`), true, "Runtime app directory must be ui/app.");
assert.equal(
  existsSync(resolve(uiRoot, ["sample", "app"].join("-"))),
  false,
  "Runtime app directory must not use the old temporary name.",
);

const requiredFiles = [
  "policies/engineering.md",
  "policies/design-system.md",
  "skills/profanity-ui/SKILL.md",
];

for (const filePath of requiredFiles) {
  assert.ok(existsSync(resolve(uiRoot, filePath)), `${filePath} must live under ui/.`);
}

const uiReadme = readFileSync(resolve(uiRoot, "README.md"), "utf8");
const appReadme = readFileSync(resolve(appRoot, "README.md"), "utf8");
const engineeringPolicy = readFileSync(resolve(uiRoot, "policies", "engineering.md"), "utf8");
const designPolicy = readFileSync(resolve(uiRoot, "policies", "design-system.md"), "utf8");
const skill = readFileSync(resolve(uiRoot, "skills", "profanity-ui", "SKILL.md"), "utf8");

for (const [name, content] of [
  ["ui/README.md", uiReadme],
  ["app/README.md", appReadme],
]) {
  assert.doesNotMatch(content, new RegExp(["샘", "플"].join("")), `${name} must describe the product UI, not a temporary app.`);
  assert.doesNotMatch(content, /실험|데모|demo/i, `${name} must avoid prototype/demo framing.`);
}

assert.match(engineeringPolicy, /YAGNI/, "Engineering policy must explicitly define YAGNI.");
assert.match(engineeringPolicy, /OOP/, "Engineering policy must explicitly define OOP boundaries.");
assert.match(engineeringPolicy, /Tell, Don'?t Ask/, "Engineering policy must include Tell, Don't Ask.");
assert.match(engineeringPolicy, /Rules of Hooks/, "Engineering policy must cite hook rules.");
assert.match(engineeringPolicy, /Red.*Green.*Refactor/s, "Engineering policy must define TDD flow.");

assert.match(designPolicy, /Design Tokens/, "Design policy must define design tokens.");
assert.match(designPolicy, /--ivory/, "Design policy must keep the existing ivory token.");
assert.match(designPolicy, /--pine/, "Design policy must keep the existing pine token.");
assert.match(designPolicy, /--sage/, "Design policy must keep the existing sage token.");
assert.match(designPolicy, /상태/, "Design policy must cover UI state rules.");
assert.match(designPolicy, /메뉴/, "Design policy must cover progressive menu growth.");

assert.match(skill, /^---\nname: profanity-ui\n/m, "Project skill must have a stable name.");
assert.match(skill, /policies\/engineering\.md/, "Project skill must route agents to engineering policy.");
assert.match(skill, /policies\/design-system\.md/, "Project skill must route agents to design policy.");
