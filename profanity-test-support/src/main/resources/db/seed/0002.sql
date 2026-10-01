-- 미연결 API Key는 인증에서 거절되므로 필터·단어 요청 E2E가 쓰는 키는 seed 사용자에 연결한다.
-- 로그인은 하지 않는 계정이라 테스트에서 쓰는 로그인 이메일과 겹치지 않아야 한다.
INSERT INTO `users` (
  `id`,
  `display_name`,
  `primary_email`,
  `status`,
  `role`,
  `created_at`,
  `updated_at`
) VALUES (
  UNHEX(REPLACE('00000000-0000-0000-0000-0000000000a1', '-', '')),
  'E2E Seed Owner',
  'e2e-seed-owner@example.com',
  'ACTIVE',
  'CLIENT',
  CURRENT_TIMESTAMP(6),
  CURRENT_TIMESTAMP(6)
);

INSERT INTO `api_keys` (
  `id`,
  `user_id`,
  `name`,
  `email`,
  `key_hash`,
  `key_hint`,
  `issuer_info`,
  `note`,
  `issued_at`,
  `permissions`,
  `request_count`
) VALUES
  (
    UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')),
    UNHEX(REPLACE('00000000-0000-0000-0000-0000000000a1', '-', '')),
    'E2E Read Client',
    'e2e-read@example.com',
    SHA2('HmikqfE546l5lP4R5UbETsfROP8go0Kq-9cZqNw-nDU', 256),
    'Hmikqf...-nDU',
    'e2e-seed',
    '읽기 권한 테스트 클라이언트',
    CURRENT_TIMESTAMP,
    'READ',
    0
  ),
  (
    UNHEX(REPLACE('00000000-0000-0000-0000-000000000002', '-', '')),
    UNHEX(REPLACE('00000000-0000-0000-0000-0000000000a1', '-', '')),
    'E2E Write Client',
    'e2e-write@example.com',
    SHA2('u6N_yQZAPfyrLheRXi7V0tZkvqe5Mno__vV0BlxpCjk', 256),
    'u6N_yQ...pCjk',
    'e2e-seed',
    '쓰기 권한 테스트 클라이언트',
    CURRENT_TIMESTAMP,
    'READ/WRITE/DELETE',
    0
  ),
  (
    UNHEX(REPLACE('00000000-0000-0000-0000-000000000003', '-', '')),
    NULL,
    'E2E Legacy Client',
    'e2e-legacy@example.com',
    SHA2('GFx8znIDWePH3M_MOPd00aLwXp0C7LGPCKsQb13FZzI', 256),
    'GFx8zn...FZzI',
    'e2e-seed',
    '로그인 계정에 연결되지 않은 기존 키 테스트 클라이언트',
    CURRENT_TIMESTAMP,
    'READ',
    0
  );
