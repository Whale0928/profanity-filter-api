INSERT INTO `clients` (
  `id`,
  `name`,
  `email`,
  `api_key`,
  `issuer_info`,
  `note`,
  `issued_at`,
  `permissions`,
  `request_count`
) VALUES
  (
    UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')),
    'E2E Read Client',
    'e2e-read@example.com',
    'e2e-read-api-key',
    'e2e-seed',
    '읽기 권한 테스트 클라이언트',
    CURRENT_TIMESTAMP,
    'READ',
    0
  ),
  (
    UNHEX(REPLACE('00000000-0000-0000-0000-000000000002', '-', '')),
    'E2E Write Client',
    'e2e-write@example.com',
    'e2e-write-api-key',
    'e2e-seed',
    '쓰기 권한 테스트 클라이언트',
    CURRENT_TIMESTAMP,
    'READ/WRITE/DELETE',
    0
  );
