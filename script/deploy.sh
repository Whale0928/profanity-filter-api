#!/bin/bash

# 환경 변수 설정
PROJECT_NAME="profanity-filter-api"
DEPLOY_PATH="/home/ubuntu/app/$PROJECT_NAME"
NGINX_CONFIG_PATH="/etc/nginx/sites-enabled/api.profanity-filter.run"
LOG_DIR="/home/ubuntu/deploy-logs/$PROJECT_NAME"

# 실행 권한 부여
chmod +x ./check_environment.sh

# 로그 디렉토리 생성 (없으면 생성)
mkdir -p $LOG_DIR

# 현재 날짜와 시간을 파일 이름에 포함시킨 로그 파일 생성
TIMESTAMP=$(date +"%Y%m%d-%H%M")
LOG_FILE="$LOG_DIR/deploy-$PROJECT_NAME-$TIMESTAMP.log"

# 로그 파일에 출력되는 모든 내용을 기록
exec > >(tee -a $LOG_FILE) 2>&1

echo "배포 스크립트 시작: $(date +"%Y-%m-%d %H:%M:%S")"

# 현재 활성화된 환경 확인
source ./check_environment.sh

echo "현재 활성화된 환경: $ACTIVE_ENV"
echo "비활성화된 환경: $INACTIVE_ENV"

# 비활성화된 환경에 배포
cd $DEPLOY_PATH
if [ "$INACTIVE_ENV" = "green" ]; then
    docker-compose up -d --no-deps --build profanity-filter-api-green-1
    docker-compose up -d --no-deps --build profanity-filter-api-green-2
else
    docker-compose up -d --no-deps --build profanity-filter-api-blue-1
    docker-compose up -d --no-deps --build profanity-filter-api-blue-2
fi

# 새 인스턴스가 준비될 때까지 대기
echo "새 인스턴스가 준비될 때까지 대기 중..."
if [ "$INACTIVE_ENV" = "green" ]; then
    until curl -f http://localhost:9997/system/actuator/health && curl -f http://localhost:9996/system/actuator/health; do
        printf '.'
        sleep 5
    done
else
    until curl -f http://localhost:9999/system/actuator/health && curl -f http://localhost:9998/system/actuator/health; do
        printf '.'
        sleep 5
    done
fi

echo "새 인스턴스가 정상적으로 실행 중입니다. Nginx 설정을 업데이트합니다."

# 도커 이미지 정리
docker image prune -f

# Nginx 설정을 새로운 환경으로 전환
if [ "$INACTIVE_ENV" = "green" ]; then
    sudo sed -i 's/# server 127.0.0.1:9997;/server 127.0.0.1:9997;/' $NGINX_CONFIG_PATH
    sudo sed -i 's/# server 127.0.0.1:9996;/server 127.0.0.1:9996;/' $NGINX_CONFIG_PATH
    sudo sed -i 's/server 127.0.0.1:9999;/# server 127.0.0.1:9999;/' $NGINX_CONFIG_PATH
    sudo sed -i 's/server 127.0.0.1:9998;/# server 127.0.0.1:9998;/' $NGINX_CONFIG_PATH
else
    sudo sed -i 's/server 127.0.0.1:9997;/# server 127.0.0.1:9997;/' $NGINX_CONFIG_PATH
    sudo sed -i 's/server 127.0.0.1:9996;/# server 127.0.0.1:9996;/' $NGINX_CONFIG_PATH
    sudo sed -i 's/# server 127.0.0.1:9999;/server 127.0.0.1:9999;/' $NGINX_CONFIG_PATH
    sudo sed -i 's/# server 127.0.0.1:9998;/server 127.0.0.1:9998;/' $NGINX_CONFIG_PATH
fi

# Nginx 재시작
sudo systemctl reload nginx

echo "Nginx 설정이 업데이트되었습니다."

# 오래된 컨테이너 정리
if [ "$ACTIVE_ENV" = "blue" ]; then
    docker stop profanity-filter-api-blue-1 profanity-filter-api-blue-2
    docker rm profanity-filter-api-blue-1 profanity-filter-api-blue-2
else
    docker stop profanity-filter-api-green-1 profanity-filter-api-green-2
    docker rm profanity-filter-api-green-1 profanity-filter-api-green-2
fi

echo "$INACTIVE_ENV 환경으로 배포가 완료되었습니다: $(date +"%Y-%m-%d %H:%M:%S")"
