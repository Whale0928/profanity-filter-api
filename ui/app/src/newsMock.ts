import { useSyncExternalStore } from "react";
import type { NewsCategory } from "./news";

export type MockPost = {
  category: NewsCategory;
  content: string;
  createdAt: string;
  id: string;
  state: "published" | "draft";
  summary: string;
  title: string;
  updatedAt: string;
};
type MockPostInput = { category: NewsCategory; content: string; title: string };
const seed = (id: string, category: MockPost["category"], title: string, date: string, summary: string, content: string, state: MockPost["state"] = "published"): MockPost => ({ id, category, title, createdAt: `${date}T01:00:00Z`, updatedAt: `${date}T01:00:00Z`, summary, content, state });
let posts: MockPost[] = [
  seed("portal-update", "CHANGELOG", "개발자 포털이 새로워졌습니다", "2026-09-10", "API 문서부터 키 관리까지, 필요한 기능을 더 쉽게 찾을 수 있습니다.", `개발자 포털의 화면과 탐색 경험을 개선했습니다. 이번 업데이트에서 달라진 내용을 확인하세요.

## 주요 변경 사항

- 소개 화면에서 QUICK, NORMAL, FILTER 모드의 차이를 바로 확인할 수 있습니다.
- 첫 요청에 필요한 코드를 복사해 연동을 시작할 수 있습니다.
- 모바일에서도 API 문서의 목차를 접고 펼칠 수 있습니다.

## API 연동에는 영향이 없습니다

기존 API 주소와 요청 형식은 그대로 사용할 수 있습니다. 추가 설정이나 API Key 재발급은 필요하지 않습니다.

\`\`\`json
{ "text": "안녕하세요", "mode": "FILTER" }
\`\`\`

## 다음 업데이트

운영 소식과 변경 내역을 이 페이지에서 지속적으로 안내할 예정입니다. 자세한 사용 방법은 [API 문서](/docs)를 확인해 주세요.`),
  seed("scheduled-maintenance", "MAINTENANCE", "9월 정기 점검 안내", "2026-09-09", "점검 시간과 서비스 이용에 미치는 영향을 안내합니다.", `서비스 안정화를 위한 정기 점검을 진행할 예정입니다.

## 점검 일정

| 항목 | 내용 |
| --- | --- |
| 일시 | 9월 15일 02:00–02:30 (KST) |
| 대상 | API 및 개발자 포털 |
| 예상 영향 | 일부 요청의 응답이 지연될 수 있습니다. |

점검이 끝나면 이 글에 결과를 안내하겠습니다.

> 이 일정은 화면 검토를 위한 예시이며 실제 점검 공지가 아닙니다.`),
  seed("getting-started", "NOTICE", "소식 페이지를 열었습니다", "2026-09-08", "공지, 변경 내역, 점검 일정과 알려진 이슈를 한곳에서 확인하세요.", `서비스를 이용하면서 알아두면 좋은 소식을 한곳에 모았습니다.

## 어떤 소식을 볼 수 있나요?

- **공지**: 서비스 운영과 이용 안내
- **변경 내역**: 새 기능과 개선 사항
- **점검 안내**: 예정된 점검과 영향 범위
- **이슈**: 확인 중인 문제와 해결 안내

필요한 유형을 선택하면 관련된 글만 볼 수 있습니다.`),
  seed("response-delay", "ISSUE", "일부 요청의 응답 지연 안내", "2026-09-06", "영향 범위와 확인 중인 내용을 안내합니다.", `일부 요청에서 응답이 지연되는 상황을 확인하고 있습니다.

## 영향 범위

일부 시간대에 API 응답이 평소보다 늦어질 수 있습니다.

## 확인 중인 내용

원인과 영향을 확인하고 있으며, 추가로 확인되는 내용은 이 글에 업데이트하겠습니다.

> 화면 검토를 위한 예시입니다. 실제 서비스 상태를 나타내지 않습니다.`),
  seed("docs-update", "CHANGELOG", "API 문서의 요청 예시를 보완했습니다", "2026-09-04", "모드별 요청과 응답을 나란히 비교할 수 있습니다.", `API 문서의 요청 예시를 정리했습니다.

- 모드별 JSON 요청 예시를 추가했습니다.
- 비동기 처리와 콜백 안내를 보완했습니다.
- 인증 헤더의 설명을 명확하게 정리했습니다.`),
  seed("next-release", "CHANGELOG", "다음 업데이트 안내", "2026-09-03", "", "## 준비 중인 변경 사항\n\n- 변경 내용을 작성해 주세요.\n\n## 적용 일정\n\n확정된 일정을 안내해 주세요.", "draft"),
];
const listeners = new Set<() => void>();
function emit() { listeners.forEach(listener => listener()); }
export function useMockPosts() { return useSyncExternalStore(listener => { listeners.add(listener); return () => { listeners.delete(listener); }; }, () => posts); }
export function saveMockPost(input: MockPostInput, state: MockPost["state"], id?: string) {
  const existing = posts.find(post => post.id === id);
  const now = new Date().toISOString();
  const post: MockPost = { ...input, id: id ?? crypto.randomUUID(), state, summary: input.content.split("\n").find(line => line.trim() && !line.startsWith("#")) ?? "", createdAt: existing?.createdAt ?? now, updatedAt: now };
  posts = [post, ...posts.filter(item => item.id !== post.id)]; emit(); return post;
}
export function removeMockPost(id: string) { posts = posts.filter(post => post.id !== id); emit(); }
