CREATE TABLE login_exchange_codes
(
	id binary(16) NOT NULL COMMENT '로그인 교환 코드 식별자',
	user_id binary(16) NOT NULL COMMENT '사용자 ID',
	code_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'SHA-256 교환 코드 해시',
	created_at datetime(6) NOT NULL COMMENT '생성 시각',
	expires_at datetime(6) NOT NULL COMMENT '만료 시각',
	consumed_at datetime(6) DEFAULT NULL COMMENT '소비 시각',
	PRIMARY KEY (id),
	UNIQUE KEY uk_login_exchange_codes_hash (code_hash),
	KEY idx_login_exchange_codes_user_id (user_id),
	KEY idx_login_exchange_codes_expires_at (expires_at),
	CONSTRAINT fk_login_exchange_codes_user_id
		FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSO 로그인 일회용 교환 코드';

CREATE TABLE login_refresh_sessions
(
	id binary(16) NOT NULL COMMENT 'refresh token family 식별자',
	user_id binary(16) NOT NULL COMMENT '사용자 ID',
	created_at datetime(6) NOT NULL COMMENT '생성 시각',
	absolute_expires_at datetime(6) NOT NULL COMMENT '세션 절대 만료 시각',
	last_rotated_at datetime(6) NOT NULL COMMENT '마지막 rotation 시각',
	revoked_at datetime(6) DEFAULT NULL COMMENT '폐기 시각',
	revoke_reason varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '폐기 사유',
	PRIMARY KEY (id),
	KEY idx_login_refresh_sessions_user_id (user_id),
	KEY idx_login_refresh_sessions_expires_at (absolute_expires_at),
	CONSTRAINT fk_login_refresh_sessions_user_id
		FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='로그인 refresh token family';

CREATE TABLE login_refresh_tokens
(
	id binary(16) NOT NULL COMMENT 'refresh token 식별자',
	session_id binary(16) NOT NULL COMMENT 'refresh token family 식별자',
	token_hash char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'SHA-256 refresh token 해시',
	issued_at datetime(6) NOT NULL COMMENT '발급 시각',
	expires_at datetime(6) NOT NULL COMMENT '만료 시각',
	consumed_at datetime(6) DEFAULT NULL COMMENT 'rotation 소비 시각',
	replaced_by_token_id binary(16) DEFAULT NULL COMMENT '교체된 refresh token 식별자',
	PRIMARY KEY (id),
	UNIQUE KEY uk_login_refresh_tokens_hash (token_hash),
	KEY idx_login_refresh_tokens_session_id (session_id),
	KEY idx_login_refresh_tokens_expires_at (expires_at),
	KEY idx_login_refresh_tokens_replaced_by (replaced_by_token_id),
	CONSTRAINT fk_login_refresh_tokens_session_id
		FOREIGN KEY (session_id) REFERENCES login_refresh_sessions (id),
	CONSTRAINT fk_login_refresh_tokens_replaced_by
		FOREIGN KEY (replaced_by_token_id) REFERENCES login_refresh_tokens (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='rotating refresh token';
