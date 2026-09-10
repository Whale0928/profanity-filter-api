-- Additive portal migration. Existing users remain CLIENT; no administrator is assigned.
-- Historical timestamps and actors remain NULL when they were never recorded.
ALTER TABLE users
    ADD COLUMN role varchar(30) NOT NULL DEFAULT 'CLIENT',
    ADD COLUMN last_login_at datetime(6) DEFAULT NULL,
    ADD CONSTRAINT ck_users_role CHECK (role IN ('CLIENT', 'ADMIN')),
    ADD KEY idx_users_role_created (role, created_at, id);

ALTER TABLE profanity_word
    ADD COLUMN source varchar(30) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN created_at datetime(6) DEFAULT NULL,
    ADD COLUMN updated_at datetime(6) DEFAULT NULL,
    ADD COLUMN created_by binary(16) DEFAULT NULL,
    ADD COLUMN updated_by binary(16) DEFAULT NULL,
    ADD CONSTRAINT fk_words_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    ADD CONSTRAINT fk_words_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);

ALTER TABLE api_keys
    ADD COLUMN last_used_at datetime(6) DEFAULT NULL,
    ADD COLUMN revoked_by binary(16) DEFAULT NULL,
    ADD COLUMN revocation_reason varchar(500) DEFAULT NULL,
    ADD CONSTRAINT fk_keys_revoked_by FOREIGN KEY (revoked_by) REFERENCES users(id);

CREATE TABLE news_posts
(
    id bigint NOT NULL AUTO_INCREMENT,
    title varchar(160) NOT NULL,
    category varchar(30) NOT NULL,
    content_markdown mediumtext NOT NULL,
    status varchar(30) NOT NULL,
    created_by binary(16) NOT NULL,
    updated_by binary(16) NOT NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    published_at datetime(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_news_category CHECK (category IN ('NOTICE','CHANGELOG','MAINTENANCE','ISSUE')),
    CONSTRAINT ck_news_status CHECK (status IN ('DRAFT','PUBLISHED')),
    CONSTRAINT fk_news_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_news_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    KEY idx_news_status_published (status, published_at, id),
    KEY idx_news_category_status_published (category, status, published_at, id),
    KEY idx_news_updated (updated_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inquiries
(
    id bigint NOT NULL AUTO_INCREMENT,
    type varchar(30) NOT NULL,
    title varchar(160) NOT NULL,
    content text NOT NULL,
    requester_user_id binary(16) DEFAULT NULL,
    requester_api_key_id binary(16) DEFAULT NULL,
    status varchar(30) NOT NULL,
    assigned_to binary(16) DEFAULT NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    resolved_at datetime(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_inquiry_type CHECK (type IN ('WORD_REQUEST','GENERAL')),
    CONSTRAINT ck_inquiry_status CHECK (status IN ('RECEIVED','IN_PROGRESS','RESOLVED')),
    CONSTRAINT fk_inquiry_requester FOREIGN KEY (requester_user_id) REFERENCES users(id),
    CONSTRAINT fk_inquiry_key FOREIGN KEY (requester_api_key_id) REFERENCES api_keys(id),
    CONSTRAINT fk_inquiry_assignee FOREIGN KEY (assigned_to) REFERENCES users(id),
    KEY idx_inquiry_status_created (status, created_at, id),
    KEY idx_inquiry_type_created (type, created_at, id),
    KEY idx_inquiry_user_created (requester_user_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inquiry_replies
(
    id bigint NOT NULL AUTO_INCREMENT,
    inquiry_id bigint NOT NULL,
    author_user_id binary(16) NOT NULL,
    content text NOT NULL,
    created_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reply_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT fk_reply_author FOREIGN KEY (author_user_id) REFERENCES users(id),
    KEY idx_reply_inquiry_created (inquiry_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE admin_audit_logs
(
    id bigint NOT NULL AUTO_INCREMENT,
    actor_user_id binary(16) NOT NULL,
    action varchar(60) NOT NULL,
    target_type varchar(60) NOT NULL,
    target_id varchar(100) NOT NULL,
    reason varchar(500) DEFAULT NULL,
    created_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
    KEY idx_audit_actor_created (actor_user_id, created_at, id),
    KEY idx_audit_target_created (target_type, target_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE word_management
    MODIFY COLUMN request_user_id binary(16) DEFAULT NULL COMMENT '기존 API Key 요청자 ID; 로그인 문의는 NULL',
    ADD COLUMN inquiry_id bigint DEFAULT NULL,
    ADD UNIQUE KEY uk_word_management_inquiry (inquiry_id),
    ADD CONSTRAINT fk_word_management_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id);

-- request_user_id historically contains an API Key ID, not a users.id.
-- LEFT JOIN retains orphaned/unowned legacy requests; original IDs and all raw fields survive.
-- Legacy requested_at is Asia/Seoul local time; new Instant fields store UTC.
-- Historical requests stay open for review: no approval or completion time is invented.
INSERT INTO inquiries
    (id, type, title, content, requester_user_id, requester_api_key_id,
     status, created_at, updated_at)
SELECT wm.id,
       'WORD_REQUEST',
       LEFT(CONCAT('단어 요청: ', wm.word), 160),
       wm.reason,
       ak.user_id,
       ak.id,
       'RECEIVED',
       TIMESTAMPADD(HOUR, -9, wm.requested_at),
       TIMESTAMPADD(HOUR, -9, wm.requested_at)
FROM word_management wm
LEFT JOIN api_keys ak ON ak.id = wm.request_user_id;

UPDATE word_management SET inquiry_id = id;
