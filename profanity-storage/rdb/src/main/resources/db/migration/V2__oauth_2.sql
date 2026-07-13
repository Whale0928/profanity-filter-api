CREATE TABLE users
(
	id binary (16) NOT NULL COMMENT '사용자 고유 식별자',
	display_name  varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '표시 이름',
	primary_email varchar(255) COLLATE utf8mb4_bin        NOT NULL COMMENT '대표 이메일',
	avatar_url    varchar(500) COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '프로필 이미지 URL',
	status        varchar(30) COLLATE utf8mb4_unicode_ci  NOT NULL DEFAULT 'ACTIVE' COMMENT '사용자 상태',
	created_at    datetime(6)                             NOT NULL COMMENT '생성 시각',
	updated_at    datetime(6)                             NOT NULL COMMENT '수정 시각',
	PRIMARY KEY (id),
	UNIQUE KEY    uk_users_primary_email(primary_email),
	KEY           idx_users_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 계정';

CREATE TABLE oauth_accounts
(
	id binary (16) NOT NULL COMMENT 'OAuth 계정 연결 식별자',
	user_id binary (16) NOT NULL COMMENT '사용자 ID',
	provider          varchar(30) COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT 'OAuth 제공자',
	provider_user_id  varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '제공자 사용자 ID',
	provider_email    varchar(255) COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '제공자 이메일',
	email_verified    tinyint                                 NOT NULL DEFAULT 0 COMMENT '제공자 이메일 검증 여부',
	provider_username varchar(100) COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '제공자 사용자명',
	display_name      varchar(100) COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '제공자 표시 이름',
	avatar_url        varchar(500) COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '제공자 프로필 이미지 URL',
	linked_at         datetime(6)                             NOT NULL COMMENT '연결 시각',
	PRIMARY KEY (id),
	UNIQUE KEY uk_oauth_accounts_provider_user (provider, provider_user_id),
	UNIQUE KEY uk_oauth_accounts_user_provider (user_id, provider),
	KEY               idx_oauth_accounts_provider_email(provider_email),
	CONSTRAINT fk_oauth_accounts_user_id
		FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth 계정 연결';
