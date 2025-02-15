-- Install the extension we just compiled

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS test_result
(
    keyword     varchar(30) not null
        constraint test_result_tmp_pkey1 primary key,
    embedding   vector(256) not null,
    modified_at timestamp   not null
);
CREATE INDEX IF NOT EXISTS idx__test_result__embedding
    ON test_result USING hnsw (embedding vector_cosine_ops);
