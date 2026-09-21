-- 관리자 통계는 기간만으로 records를 조회한다.
-- 기존 idx_records_api_key_hash_created는 선두 컬럼이 api_key_hash라 기간 단독 범위 조건에 쓰이지 못하므로
-- created_at을 선두에 둔 인덱스를 별도로 둔다. 정렬 컬럼 구성은 V5의 (..., created_at, id) 관례를 따른다.
ALTER TABLE records
    ADD KEY idx_records_created (created_at, id);
