CREATE TABLE api_keys
(
	id            binary(16) NOT NULL COMMENT 'API Key 식별자',
	user_id       binary(16) DEFAULT NULL COMMENT '소유 사용자 ID',
	name          varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API Key 이름',
	email         varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '발급 사용자 이메일',
	key_hash      char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'API Key SHA-256 해시',
	key_hint      varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '화면 표시용 API Key 힌트',
	issuer_info   varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '발급자 정보',
	note          varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
	permissions   text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '권한',
	issued_at     datetime(6) NOT NULL COMMENT '발급 시각',
	expired_at    datetime(6) DEFAULT NULL COMMENT '만료 시각',
	request_count bigint NOT NULL DEFAULT 0 COMMENT '요청 횟수',
	PRIMARY KEY (id),
	UNIQUE KEY uk_api_keys_key_hash (key_hash),
	KEY idx_api_keys_user_issued (user_id, issued_at),
	KEY idx_api_keys_email_owner (email, user_id),
	CONSTRAINT fk_api_keys_user_id
		FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSO 사용자 소유 API Key';

INSERT INTO api_keys
(
	id,
	user_id,
	name,
	email,
	key_hash,
	key_hint,
	issuer_info,
	note,
	permissions,
	issued_at,
	expired_at,
	request_count
)
SELECT id,
       NULL,
       name,
       LOWER(email),
       COALESCE(SHA2(api_key, 256), SHA2(CONCAT('retired:', HEX(id)), 256)),
       CASE
         WHEN api_key IS NULL THEN '사용 불가'
         ELSE CONCAT(LEFT(api_key, 6), '...', RIGHT(api_key, 4))
       END,
       issuer_info,
       note,
       COALESCE(permissions, 'READ'),
       COALESCE(issued_at, CURRENT_TIMESTAMP(6)),
       CASE
         WHEN api_key IS NULL THEN COALESCE(expired_at, CURRENT_TIMESTAMP(6))
         ELSE expired_at
       END,
       COALESCE(request_count, 0)
FROM clients;

ALTER TABLE records
	ADD COLUMN api_key_hash char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT 'API Key SHA-256 해시' AFTER mode;

UPDATE records
SET api_key_hash = SHA2(api_key, 256)
WHERE api_key IS NOT NULL;

ALTER TABLE records
	ADD KEY idx_records_api_key_hash_created (api_key_hash, created_at),
	DROP COLUMN api_key;

ALTER TABLE client_reports
	DROP INDEX idx_client_reports_api_key,
	DROP COLUMN api_key,
	ADD CONSTRAINT fk_client_reports_api_key_id
		FOREIGN KEY (client_id) REFERENCES api_keys (id);

DROP TABLE clients;
