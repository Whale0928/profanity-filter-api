export const OPENAPI_DOCUMENT_URL = "https://api.kr-filter.com/openapi.json";
export const OVERVIEW_MARKDOWN_PATH = "https://api.kr-filter.com/overview.md";
export const HTTP_METHODS = [
  "get",
  "post",
  "put",
  "patch",
  "delete",
  "options",
  "head",
  "trace",
] as const;

export const FALLBACK_OVERVIEW_MARKDOWN = `# Profanity Filter API

한국어 비속어 필터링 API는 문장 안의 부적절한 표현을 감지하고, 필요한 경우 검출된 단어를 마스킹해 반환합니다.

## 시작하기

1. 클라이언트 등록 API로 API Key를 발급합니다.
2. 인증이 필요한 요청은 \`x-api-key\` 헤더에 발급받은 API Key를 포함합니다.
3. \`/api/v1/filter\`에 검사할 \`text\`와 처리 방식인 \`mode\`를 전달합니다.

## 주요 기능

- 문장 안의 한국어와 영어 비속어를 검출합니다.
- 검출 결과를 목록으로 받거나, 검출된 단어를 \`*\`로 마스킹한 문장을 받을 수 있습니다.
- 실제 처리 결과는 응답 본문의 \`status.code\`와 \`status.message\`에서 확인합니다.
`;
