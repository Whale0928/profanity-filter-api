import { adminRequest, buildQuery, type AdminPageData } from "./adminClient";

export type AdminKeyStatus = "ACTIVE" | "EXPIRED";

export type AdminKeyView = {
  active: boolean;
  email: string;
  expiredAt: string | null;
  id: string;
  issuedAt: string;
  keyHint: string;
  lastUsedAt: string | null;
  name: string;
  ownerName: string;
};

export function listAdminKeys(accessToken: string, query: string, status: AdminKeyStatus | "", page: number, signal?: AbortSignal) {
  const params = buildQuery({ page, query: query || undefined, status: status || undefined });
  return adminRequest<AdminPageData<AdminKeyView>>(accessToken, `/api/v1/admin/keys?${params}`, { signal });
}

export function revokeAdminKey(accessToken: string, id: string, reason: string) {
  return adminRequest<AdminKeyView>(accessToken, `/api/v1/admin/keys/${encodeURIComponent(id)}/revoke`, {
    body: JSON.stringify({ reason }),
    method: "POST",
  });
}
