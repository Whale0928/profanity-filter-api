#!/bin/bash

# 색상 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'

# 결과 저장용 변수
FAILED_TESTS=0
TOTAL_TESTS=0
declare -A TEST_RESULTS
declare -A TEST_MESSAGES
declare -A TEST_TIMINGS

# 시작 시간 기록
START_TIME=$(date +%s)

# 테스트 결과 출력 함수
print_result() {
    local status=$1
    local message=$2
    local category=$3
    local detail=$4

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    TEST_RESULTS[$TOTAL_TESTS]=$status
    TEST_MESSAGES[$TOTAL_TESTS]="[$category] $message"

    if [ -n "$detail" ]; then
        TEST_MESSAGES[$TOTAL_TESTS]="${TEST_MESSAGES[$TOTAL_TESTS]}\n    └─ $detail"
    fi

    if [ $status -ne 0 ]; then
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

print_separator() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

echo -e "\n${YELLOW}🔍 Redis 클러스터 헬스 체크 시작${NC}"
echo -e "${CYAN}실행 시간: $(date '+%Y년 %m월 %d일 %H:%M:%S')${NC}"
print_separator

# 1. 연결 테스트
echo -e "\n${YELLOW}[1] 노드 연결 상태 검사${NC}"
for port in 10001 10002 10003; do
    node_type=$([ $port -eq 10001 ] && echo "메인노드" || echo "서브노드 $((port-10001))")
    response_time=$(redis-cli -p $port ping 2>/dev/null | tr -d '\r')
    if [ "$response_time" == "PONG" ]; then
        latency=$(redis-cli -p $port ping | tr -d '\r')
        print_result 0 "노드 연결 성공" "$node_type" "응답: PONG (포트: $port)"
    else
        print_result 1 "노드 연결 실패" "$node_type" "포트 $port 응답 없음"
    fi
done

# 2. 복제 상태 테스트
echo -e "\n${YELLOW}[2] 복제 상태 검사${NC}"
replication_info=$(redis-cli -p 10001 info replication)
connected_slaves=$(echo "$replication_info" | grep "connected_slaves:" | cut -d: -f2 | tr -d '\r')
print_result $? "복제 상태" "메인노드" "연결된 서브노드 수: $connected_slaves"

# 서브노드 각각의 상태 확인
for port in 10002 10003; do
    sub_info=$(redis-cli -p $port info replication)
    master_link_status=$(echo "$sub_info" | grep "master_link_status" | cut -d: -f2 | tr -d '\r')
    master_sync_speed=$(echo "$sub_info" | grep "master_sync_in_progress" | cut -d: -f2 | tr -d '\r')

    if [ "$master_link_status" == "up" ]; then
        print_result 0 "복제 상태" "서브노드 $((port-10001))" "메인노드와 연결 정상"
    else
        print_result 1 "복제 상태" "서브노드 $((port-10001))" "메인노드와 연결 실패"
    fi
done

# 3. 데이터 쓰기/읽기 테스트
echo -e "\n${YELLOW}[3] 데이터 동기화 검사${NC}"
TEST_VALUE="healthcheck_$(date +%s)"
redis-cli -p 10001 set testkey "$TEST_VALUE" > /dev/null
sleep 1  # 복제 대기

# 메인노드 쓰기 확인
write_status=$(redis-cli -p 10001 get testkey)
if [ "$write_status" == "$TEST_VALUE" ]; then
    print_result 0 "데이터 쓰기" "메인노드" "테스트 값 저장 성공"
else
    print_result 1 "데이터 쓰기" "메인노드" "테스트 값 저장 실패"
fi

# 각 서브노드 읽기 확인
for port in 10002 10003; do
    read_value=$(redis-cli -p $port get testkey)
    if [ "$read_value" == "$TEST_VALUE" ]; then
        print_result 0 "데이터 읽기" "서브노드 $((port-10001))" "복제 데이터 일치"
    else
        print_result 1 "데이터 읽기" "서브노드 $((port-10001))" "복제 데이터 불일치"
    fi
done

# 4. 쓰기 권한 테스트
echo -e "\n${YELLOW}[4] 쓰기 권한 검사${NC}"
for port in 10002 10003; do
    write_response=$(redis-cli -p $port set writetest "test" 2>&1)
    if [[ $write_response == *"READONLY"* ]]; then
        print_result 0 "쓰기 권한" "서브노드 $((port-10001))" "읽기 전용 설정 정상"
    else
        print_result 1 "쓰기 권한" "서브노드 $((port-10001))" "쓰기 권한 제한 실패"
    fi
done

# 5. 대량 데이터 테스트
echo -e "\n${YELLOW}[5] 대량 데이터 동기화 검사${NC}"
echo -e "${CYAN}대량의 데이터를 생성하고 복제 상태를 확인합니다...${NC}"

# 메인노드에 데이터 쓰기
for i in {1..100}; do
    redis-cli -p 10001 set "bulk:$i" "value:$i" > /dev/null
done
sleep 2  # 복제 대기

# 데이터 개수 확인
main_count=$(redis-cli -p 10001 keys "bulk:*" | wc -l)
print_result 0 "데이터 생성" "메인노드" "생성된 키 개수: $main_count"

for port in 10002 10003; do
    sub_count=$(redis-cli -p $port keys "bulk:*" | wc -l)
    if [ "$sub_count" == "$main_count" ]; then
        print_result 0 "데이터 복제" "서브노드 $((port-10001))" "복제된 키 개수: $sub_count"
    else
        print_result 1 "데이터 복제" "서브노드 $((port-10001))" "불일치 (예상: $main_count, 실제: $sub_count)"
    fi
done

# 실행 시간 계산
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# 종합 결과 출력
print_separator
echo -e "\n${YELLOW}📊 테스트 결과 요약${NC}"
echo -e "${CYAN}실행 완료 시간: $(date '+%Y년 %m월 %d일 %H:%M:%S')${NC}"
echo -e "${CYAN}총 실행 시간: ${DURATION}초${NC}"
print_separator
echo -e "\n${CYAN}테스트 상세 결과:${NC}"

for i in $(seq 1 $TOTAL_TESTS); do
    if [ ${TEST_RESULTS[$i]} -eq 0 ]; then
        echo -e "${GREEN}[✓] ${TEST_MESSAGES[$i]}${NC}"
    else
        echo -e "${RED}[✗] ${TEST_MESSAGES[$i]}${NC}"
    fi
done

print_separator
echo -e "\n${YELLOW}📈 최종 통계${NC}"
echo -e "총 테스트 수: $TOTAL_TESTS"
echo -e "성공한 테스트: ${GREEN}$((TOTAL_TESTS - FAILED_TESTS))${NC}"
echo -e "실패한 테스트: ${RED}${FAILED_TESTS}${NC}"
echo -e "성공률: ${CYAN}$(( (TOTAL_TESTS - FAILED_TESTS) * 100 / TOTAL_TESTS ))%${NC}"
print_separator

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}✨ 모든 테스트가 성공적으로 완료되었습니다.${NC}\n"
    exit 0
else
    echo -e "\n${RED}❌ 일부 테스트가 실패했습니다. 상세 로그를 확인해주세요.${NC}\n"
    exit 1
fi
