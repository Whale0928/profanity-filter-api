SET
FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `client_reports`
(
	`id`                        bigint                                  NOT NULL AUTO_INCREMENT COMMENT '리포트 ID',
	`client_id`                 binary(16) NOT NULL COMMENT '클라이언트 ID',
	`api_key`                   varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API 키',
	`report_year`               int                                     NOT NULL COMMENT '리포트 년도',
	`report_month`              int                                     NOT NULL COMMENT '리포트 월',
	`report_day`                int                                     NOT NULL COMMENT '리포트 일',
	`request_count`             bigint                                  NOT NULL DEFAULT '0' COMMENT '일일 요청 횟수',
	`profanity_detection_count` bigint                                  NOT NULL DEFAULT '0' COMMENT '욕설 검출 횟수',
	`created_at`                datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uk_client_daily_report` (`client_id`, `report_year`, `report_month`, `report_day`),
	KEY                         `idx_client_reports_api_key` (`api_key`),
	KEY                         `idx_client_reports_client_id` (`client_id`),
	KEY                         `idx_client_reports_date` (`report_year`, `report_month`, `report_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='클라이언트 일일 사용 리포트';

CREATE TABLE `clients`
(
	`id`            binary(16) NOT NULL,
	`name`          varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '클라이언트명',
	`email`         varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '이메일',
	`api_key`       varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`issuer_info`   varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '발급자 정보',
	`note`          varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '비고',
	`issued_at`     datetime                                DEFAULT CURRENT_TIMESTAMP COMMENT '발급일시',
	`permissions`   text COLLATE utf8mb4_unicode_ci,
	`expired_at`    datetime                                DEFAULT NULL COMMENT '만료일시',
	`request_count` bigint                                  DEFAULT '0' COMMENT '요청 횟수',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uk_clients_email` (`email`),
	UNIQUE KEY `uk_clients_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='클라이언트 정보';

CREATE TABLE `manage_account`
(
	`id`       bigint                                  NOT NULL AUTO_INCREMENT,
	`password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '비밀번호',
	`username` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '관리자명',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uk_manage_account_password` (`password`),
	UNIQUE KEY `uk_manage_account_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `profanity_word`
(
	`id`      bigint                                  NOT NULL AUTO_INCREMENT,
	`is_used` enum('N', 'Y') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
	`word`    varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
	PRIMARY KEY (`id`),
	UNIQUE KEY `uk_profanity_word_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `records`
(
	`id`           bigint                                  NOT NULL AUTO_INCREMENT,
	`tracking_id`  binary(16) NOT NULL COMMENT '요청 트래킹 ID',
	`api_key`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`referrer`     varchar(255) COLLATE utf8mb4_unicode_ci                       DEFAULT NULL COMMENT '요청 referrer',
	`request_text` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '요청된 텍스트',
	`mode`         enum('FILTER', 'NORMAL', 'QUICK') COLLATE utf8mb4_unicode_ci NOT NULL,
	`words`        varchar(255) COLLATE utf8mb4_unicode_ci                       DEFAULT NULL COMMENT '필터링 된 욕설들',
	`ip`           varchar(255) COLLATE utf8mb4_unicode_ci                       DEFAULT NULL COMMENT '요청 IP',
	`created_at`   datetime(6) NOT NULL COMMENT '요청 시각',
	`updated_at`   datetime(6) DEFAULT NULL COMMENT '요청 수정 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uk_records_tracking_id` (`tracking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shedlock`
(
	`name`       varchar(64) COLLATE utf8mb4_unicode_ci  NOT NULL,
	`lock_until` timestamp                               NOT NULL,
	`locked_at`  timestamp                               NOT NULL,
	`locked_by`  varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
	PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `word_management`
(
	`id`              bigint                                  NOT NULL AUTO_INCREMENT,
	`request_user_id` binary(16) NOT NULL COMMENT '요청자 ID',
	`word`            varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '단어',
	`reason`          text COLLATE utf8mb4_unicode_ci         NOT NULL COMMENT '사유',
	`severity`        varchar(50) COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '심각도',
	`request_type`    varchar(50) COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '요청 타입',
	`status`          varchar(50) COLLATE utf8mb4_unicode_ci  NOT NULL DEFAULT 'REQUEST' COMMENT '상태',
	`requested_at`    datetime                                NOT NULL COMMENT '요청일시',
	PRIMARY KEY (`id`),
	KEY               `idx_word_management_status` (`status`),
	KEY               `idx_word_management_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET
FOREIGN_KEY_CHECKS = 1;
