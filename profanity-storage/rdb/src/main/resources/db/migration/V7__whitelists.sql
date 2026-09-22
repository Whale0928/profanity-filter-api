-- 계정이 소유하는 허용 단어 그룹(ADR 0009).
-- 단어 목록은 필터 경로에서 그룹마다 한 행만 읽도록 TEXT 컬럼 하나에 줄바꿈으로 이어 담는다.
-- 시각은 V5 이후의 관례대로 UTC Instant다.
CREATE TABLE whitelists
(
    id binary(16) NOT NULL,
    user_id binary(16) NOT NULL,
    name varchar(60) NOT NULL,
    words text NOT NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_whitelist_user FOREIGN KEY (user_id) REFERENCES users(id),
    KEY idx_whitelist_user_created (user_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
