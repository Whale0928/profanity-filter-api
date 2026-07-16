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
    NULL,
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
    NULL,
    'E2E Write Client',
    'e2e-write@example.com',
    SHA2('u6N_yQZAPfyrLheRXi7V0tZkvqe5Mno__vV0BlxpCjk', 256),
    'u6N_yQ...pCjk',
    'e2e-seed',
    '쓰기 권한 테스트 클라이언트',
    CURRENT_TIMESTAMP,
    'READ/WRITE/DELETE',
    0
  );
