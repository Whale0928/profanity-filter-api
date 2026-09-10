import { adminRequest, buildQuery, type AdminPageData } from "./adminClient";

export type InquiryType = "GENERAL" | "WORD_REQUEST";
export const INQUIRY_TYPE_LABELS: Record<InquiryType, string> = { GENERAL: "일반 문의", WORD_REQUEST: "단어 요청" };

export type InquiryStatus = "IN_PROGRESS" | "RECEIVED" | "RESOLVED";
const INQUIRY_STATUS_LABELS: Record<string, string> = { IN_PROGRESS: "처리 중", RECEIVED: "접수", RESOLVED: "완료" };
export function inquiryStatusLabel(value: string): string {
  return INQUIRY_STATUS_LABELS[value] ?? value;
}

// 문의 등록 시 전송하는 값(ADD|REMOVE|MODIFY). 과거 저장된 요청은 NEW|EXCEPTION으로 남아 있을 수 있어 표시용 매핑을 별도로 둔다.
export type NewWordRequestType = "ADD" | "MODIFY" | "REMOVE";
const WORD_REQUEST_TYPE_LABELS: Record<string, string> = { ADD: "추가", EXCEPTION: "제외", MODIFY: "수정", NEW: "추가", REMOVE: "제외" };
export function wordRequestTypeLabel(value: string): string {
  return WORD_REQUEST_TYPE_LABELS[value] ?? value;
}

export type WordSeverity = "HIGH" | "LOW" | "MEDIUM";
export const WORD_SEVERITY_LABELS: Record<WordSeverity, string> = { HIGH: "높은 수위", LOW: "낮은 수위", MEDIUM: "중간 수위" };

export type WordDecision = "APPROVE" | "REJECT";
const WORD_REQUEST_STATUS_LABELS: Record<string, string> = { APPROVED: "승인됨", REJECTED: "거절됨", REQUEST: "대기 중" };
export function wordRequestStatusLabel(value: string): string {
  return WORD_REQUEST_STATUS_LABELS[value] ?? value;
}

export type InquiryWordRequest = {
  id: number;
  reason: string;
  requestType: string;
  severity: string;
  status: string;
  word: string;
};

export type InquirySummary = {
  createdAt: string;
  id: number;
  requesterEmail: string | null;
  requesterName: string | null;
  resolvedAt: string | null;
  status: string;
  title: string;
  type: string;
  updatedAt: string;
};

export type InquiryReply = { authorName: string; content: string; createdAt: string; id: number };

export type InquiryDetail = InquirySummary & {
  content: string;
  replies: InquiryReply[];
  wordRequest: InquiryWordRequest | null;
};

export type InquiryPageData = AdminPageData<InquirySummary>;

export type NewInquiryInput = {
  content: string;
  requestType?: NewWordRequestType;
  severity?: WordSeverity;
  title: string;
  type: InquiryType;
  word?: string;
};

export function listAdminInquiries(
  accessToken: string,
  type: InquiryType | "",
  status: string,
  query: string,
  page: number,
  signal?: AbortSignal,
) {
  const params = buildQuery({ page, query: query || undefined, status: status || undefined, type: type || undefined });
  return adminRequest<InquiryPageData>(accessToken, `/api/v1/admin/inquiries?${params}`, { signal });
}

export function getAdminInquiry(accessToken: string, id: number, signal?: AbortSignal) {
  return adminRequest<InquiryDetail>(accessToken, `/api/v1/admin/inquiries/${id}`, { signal });
}

export function updateInquiryStatus(accessToken: string, id: number, status: InquiryStatus) {
  return adminRequest<InquiryDetail>(accessToken, `/api/v1/admin/inquiries/${id}/status`, {
    body: JSON.stringify({ status }),
    method: "PATCH",
  });
}

export function replyToInquiry(accessToken: string, id: number, content: string, resolve: boolean) {
  return adminRequest<InquiryDetail>(accessToken, `/api/v1/admin/inquiries/${id}/replies`, {
    body: JSON.stringify({ content, resolve }),
    method: "POST",
  });
}

export function decideWordRequest(accessToken: string, id: number, decision: WordDecision, reason: string) {
  return adminRequest<InquiryDetail>(accessToken, `/api/v1/admin/inquiries/${id}/word-decision`, {
    body: JSON.stringify({ decision, reason }),
    method: "POST",
  });
}

export function listMyInquiries(accessToken: string, page: number, signal?: AbortSignal) {
  const params = buildQuery({ page });
  return adminRequest<InquiryPageData>(accessToken, `/api/v1/dashboard/inquiries?${params}`, { signal });
}

export function getMyInquiry(accessToken: string, id: number, signal?: AbortSignal) {
  return adminRequest<InquiryDetail>(accessToken, `/api/v1/dashboard/inquiries/${id}`, { signal });
}

export function createInquiry(accessToken: string, input: NewInquiryInput) {
  return adminRequest<InquiryDetail>(accessToken, "/api/v1/dashboard/inquiries", { body: JSON.stringify(input), method: "POST" });
}
