export type IdentityTabOption = {
  id: "free-practical" | "korean-focused" | "developer-integration";
  tabLabel: string;
  label: string;
  intro: string;
  title: string;
  body: string;
  points: readonly string[];
  proofItems: readonly string[];
  visualLabel: string;
  visualTitle: string;
  visualLines: readonly string[];
};

export type ScenarioStep = {
  id: "scenario-1" | "scenario-2" | "scenario-3";
  label: string;
  title: string;
  body: string;
};

export const IDENTITY_OPTIONS: readonly IdentityTabOption[] = [
  {
    id: "free-practical",
    tabLabel: "무료/실용",
    label: "작은 서비스",
    intro: "비용 때문에 입력 안전망을 미루지 않게",
    title: "작은 서비스도 한국어 입력을 안전하게 다룰 수 있게",
    body: "포트폴리오, 학습, 비영리 서비스가 댓글과 게시글 저장 전에 기본 비속어 필터링을 붙일 수 있도록 만든 무료 REST API입니다.",
    points: ["무료로 시작", "키 발급 후 호출", "현실적인 첫 안전망"],
    proofItems: ["키 발급 후 바로 호출", "문서 보고 붙이기", "상업용은 KISO 권장"],
    visualLabel: "추천 대상",
    visualTitle: "무료로 시작하는 입력 안전망",
    visualLines: ["학습 서비스", "비영리 프로젝트", "사이드 프로젝트"],
  },
  {
    id: "korean-focused",
    tabLabel: "한국어 중심",
    label: "한국어 필터",
    intro: "영어권 moderation이 놓치는 한국어 입력을 기준으로",
    title: "한국어 비속어 사전을 빠르게 검색합니다",
    body: "Aho-Corasick Trie로 등록된 비속어 목록을 한 번에 매칭하고, 초성/한글/영문이 섞인 사용자 입력에서도 검출 위치를 계산합니다.",
    points: ["빠른 사전 검색", "검출부터 마스킹까지", "단어 목록 동기화"],
    proofItems: ["첫 단어만 빠르게 확인", "전체 검출", "마스킹 문장 반환"],
    visualLabel: "검출 기준",
    visualTitle: "한국어 문장을 기준으로",
    visualLines: ["초성 입력", "한글 문장", "영문 혼합"],
  },
  {
    id: "developer-integration",
    tabLabel: "개발자 연동",
    label: "REST API",
    intro: "서비스 흐름을 크게 바꾸지 않고",
    title: "저장 전에 API 한 번으로 붙입니다",
    body: "댓글, 채팅, 게시글 저장 직전에 필터 API를 호출하고, 검출 결과와 마스킹된 문장, 응답 코드를 확인하면 됩니다.",
    points: ["문장 전달", "동기 또는 콜백 처리", "문서 기반 연동"],
    proofItems: ["필터 API 호출", "문장 전달", "응답 코드 확인", "키로 인증"],
    visualLabel: "연동 흐름",
    visualTitle: "저장 전에 한 번 확인",
    visualLines: ["입력 받기", "필터 호출", "결과 반영"],
  },
] as const;

export const SCENARIO_STEPS: readonly ScenarioStep[] = [
  {
    id: "scenario-1",
    label: "01",
    title: "사용자가 댓글을 입력",
    body: "서비스는 저장 전에 필터 API를 호출합니다.",
  },
  {
    id: "scenario-2",
    label: "02",
    title: "FILTER 모드로 마스킹",
    body: "비속어 위치를 찾고 노출 가능한 문장으로 바꿉니다.",
  },
  {
    id: "scenario-3",
    label: "03",
    title: "정리된 문장을 저장",
    body: "응답 결과를 댓글, 채팅, 게시글 흐름에 그대로 반영합니다.",
  },
] as const;
