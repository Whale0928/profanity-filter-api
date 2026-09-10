import { adminRequest, buildQuery, type AdminPageData } from "./adminClient";
import type { UserRole } from "./auth";

export type UserStatus = "ACTIVE" | "DISABLED";

export type UserView = {
  createdAt: string;
  displayName: string;
  id: string;
  lastLoginAt: string | null;
  primaryEmail: string;
  role: UserRole;
  status: UserStatus;
};

export function listUsers(accessToken: string, query: string, role: UserRole | "", page: number, signal?: AbortSignal) {
  const params = buildQuery({ page, query: query || undefined, role: role || undefined });
  return adminRequest<AdminPageData<UserView>>(accessToken, `/api/v1/admin/users?${params}`, { signal });
}

export function updateUserStatus(accessToken: string, id: string, status: UserStatus) {
  return adminRequest<UserView>(accessToken, `/api/v1/admin/users/${encodeURIComponent(id)}/status`, {
    body: JSON.stringify({ status }),
    method: "PATCH",
  });
}
