#!/bin/bash
#############################################################
# blue-green-swap.sh
#
# 목적: Nginx에서 블루/그린 환경 사이의 트래픽을 전환
#
# 저장 위치: /usr/local/bin/blue-green-swap.sh
#
# 작동 방식:
# 1. 현재 활성화된 환경(blue 또는 green)을 확인
# 2. 반대 환경으로 트래픽을 전환
# 3. Nginx 설정을 업데이트하고 재로드
#
# 사용법: ./blue-green-swap.sh [check|force-blue|force-green]
#############################################################

# 색상 정의 (출력 강조용)
BLUE='\033[0;34m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # 색상 초기화

# Nginx 설정 파일 경로
NGINX_CONF="/etc/nginx/sites-available/api.profanity.kr-filter.com"

# 오류 처리 함수
error_exit() {
    echo -e "${RED}오류: $1${NC}" >&2
    exit 1
}

# 현재 활성 환경 확인 함수
get_current_environment() {
    # grep으로 'set $current_upstream blue' 또는 'set $current_upstream green' 패턴 찾기
    # awk로 세 번째 단어(blue 또는 green) 추출
    CURRENT=$(grep -o "set \$current_upstream blue\|set \$current_upstream green" $NGINX_CONF | awk '{print $3}')

    # 결과가 없으면 오류 처리
    if [ -z "$CURRENT" ]; then
        error_exit "현재 환경을 확인할 수 없습니다. Nginx 설정 파일을 확인하세요."
    fi

    echo $CURRENT
}

# Nginx 설정 업데이트 함수
update_nginx_config() {
    local current=$1
    local new=$2

    echo -e "Nginx 설정을 ${YELLOW}$current${NC}에서 ${YELLOW}$new${NC}로 업데이트 중..."

    # sed로 현재 환경을 새 환경으로 교체
    sudo sed -i "s/set \$current_upstream $current/set \$current_upstream $new/" $NGINX_CONF || error_exit "Nginx 설정 업데이트 실패"

    echo "Nginx 설정 업데이트 완료"
}

# Nginx 재로드 함수
reload_nginx() {
    echo "Nginx 설정 테스트 중..."
    sudo nginx -t || error_exit "Nginx 설정 테스트 실패"

    echo "Nginx 재로드 중..."
    sudo systemctl reload nginx || error_exit "Nginx 재로드 실패"

    echo "Nginx 재로드 완료"
}

# 메인 로직
main() {
    # 현재 활성 환경 확인
    CURRENT=$(get_current_environment)

    # 현재 상태 출력
    if [ "$CURRENT" = "blue" ]; then
        echo -e "현재 환경: ${BLUE}BLUE${NC}"
    else
        echo -e "현재 환경: ${GREEN}GREEN${NC}"
    fi

    # 인자에 따른 처리
    case "$1" in
        check)
            # 현재 환경만 확인하고 종료
            exit 0
            ;;
        force-blue)
            # blue로 강제 전환
            NEW="blue"
            ;;
        force-green)
            # green으로 강제 전환
            NEW="green"
            ;;
        *)
            # 기본: 반대 환경으로 전환
            if [ "$CURRENT" = "blue" ]; then
                NEW="green"
            else
                NEW="blue"
            fi
            ;;
    esac

    # 현재 환경과 새 환경이 같으면 아무 작업 안 함
    if [ "$CURRENT" = "$NEW" ]; then
        echo -e "${YELLOW}현재 이미 $NEW 환경이 활성화되어 있습니다. 변경이 필요하지 않습니다.${NC}"
        exit 0
    fi

    # Nginx 설정 업데이트
    update_nginx_config $CURRENT $NEW

    # Nginx 재로드
    reload_nginx

    # 완료 메시지
    if [ "$NEW" = "blue" ]; then
        echo -e "✅ 트래픽이 ${GREEN}GREEN${NC}에서 ${BLUE}BLUE${NC}로 성공적으로 전환되었습니다."
    else
        echo -e "✅ 트래픽이 ${BLUE}BLUE${NC}에서 ${GREEN}GREEN${NC}으로 성공적으로 전환되었습니다."
    fi
}

# 스크립트 실행
main "$@"
