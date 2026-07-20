import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const appDirectory = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const distDirectory = resolve(appDirectory, "dist");

async function readDistFile(path) {
  return readFile(resolve(distDirectory, path), "utf8");
}

function titleOf(html) {
  return html.match(/<title>([^<]+)<\/title>/)?.[1] ?? "";
}

function canonicalOf(html) {
  return html.match(/<link[^>]+rel="canonical"[^>]+href="([^"]+)"/)?.[1] ?? "";
}

const [landing, docs, robots, sitemap, llms] = await Promise.all([
  readDistFile("index.html"),
  readDistFile("docs/index.html"),
  readDistFile("robots.txt"),
  readDistFile("sitemap.xml"),
  readDistFile("llms.txt"),
]);

assert.equal(titleOf(landing), "한국어 욕설·비속어 필터 API | 말조심하세욧");
assert.equal(titleOf(docs), "한국어 텍스트 처리 API 문서 | 말조심하세욧");
assert.notEqual(titleOf(landing), titleOf(docs));
assert.equal(canonicalOf(landing), "https://developers.kr-filter.com/");
assert.equal(canonicalOf(docs), "https://developers.kr-filter.com/docs");
assert.match(landing, /<h1>한국어 욕설·비속어<br\s*\/?\s*>필터 API<\/h1>/);
assert.match(docs, /<h1>한국어 텍스트 처리 API 문서<\/h1>/);
assert.match(docs, /href="https:\/\/api\.kr-filter\.com\/overview\.md"/);
assert.match(docs, /href="https:\/\/api\.kr-filter\.com\/openapi\.json"/);

const jsonLdBlocks = [...landing.matchAll(/<script type="application\/ld\+json">\s*([\s\S]*?)\s*<\/script>/g)];
assert.ok(jsonLdBlocks.length > 0, "landing JSON-LD is required");
const jsonLdTypes = jsonLdBlocks
  .flatMap((match) => {
    const document = JSON.parse(match[1]);
    return Array.isArray(document["@graph"]) ? document["@graph"] : [document];
  })
  .map((item) => item["@type"]);
assert.ok(jsonLdTypes.includes("WebSite"));
assert.ok(jsonLdTypes.includes("SoftwareApplication"));

assert.doesNotMatch(robots, /<html/i);
assert.match(robots, /User-agent: OAI-SearchBot[\s\S]*?Allow: \//);
assert.match(robots, /User-agent: GPTBot\s+Disallow: \//);
assert.match(robots, /Sitemap: https:\/\/developers\.kr-filter\.com\/sitemap\.xml/);

assert.match(sitemap, /^<\?xml version="1\.0" encoding="UTF-8"\?>/);
const sitemapLocations = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map((match) => match[1]);
assert.deepEqual(sitemapLocations, ["https://developers.kr-filter.com/", "https://developers.kr-filter.com/docs"]);

assert.doesNotMatch(llms, /<html/i);
assert.match(llms, /https:\/\/developers\.kr-filter\.com\/docs/);
assert.match(llms, /https:\/\/api\.kr-filter\.com\/overview\.md/);
assert.match(llms, /https:\/\/api\.kr-filter\.com\/openapi\.json/);
assert.doesNotMatch(`${landing}\n${docs}\n${llms}`, /입력을 저장하지|익명 학습|privacy-first|private/i);

console.log("Public discovery artifacts verified: 5 files");
