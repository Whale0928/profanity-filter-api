#!/bin/bash

# 도커파일과 환경파일 경로 설정
DOCKER_DIR="$(cd .. && pwd)"  # 상위 디렉토리의 절대 경로
DOCKERFILE="$DOCKER_DIR/Dockerfile"
ENV_FILE="$DOCKER_DIR/.env"

# 파일 존재 확인
if [ ! -f "$DOCKERFILE" ]; then
  echo "에러: Dockerfile을 찾을 수 없습니다: $DOCKERFILE"
  echo "현재 스크립트 위치: $(pwd)"
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "경고: .env 파일을 찾을 수 없습니다: $ENV_FILE"
  echo "환경 변수가 없는 상태로 진행합니다."
  ENV_PARAM=""
else
  ENV_PARAM="--env-file $ENV_FILE"
fi

# 현재 실행 중인 컨테이너와 포트 상태 확인
BLUE_CONTAINER=$(docker ps --filter "status=running" --filter "name=profanity-blue" -q)
GREEN_CONTAINER=$(docker ps --filter "status=running" --filter "name=profanity-green" -q)
PORT_9090_CONTAINER=$(docker ps --filter "status=running" --filter "publish=9090-9090" -q)
PORT_9091_CONTAINER=$(docker ps --filter "status=running" --filter "publish=9091-9091" -q)

echo "현재 상태:"
echo "- Blue 컨테이너: ${BLUE_CONTAINER:-없음}"
echo "- Green 컨테이너: ${GREEN_CONTAINER:-없음}"
echo "- 9090 포트 사용: ${PORT_9090_CONTAINER:-없음}"
echo "- 9091 포트 사용: ${PORT_9091_CONTAINER:-없음}"

# 블루 환경이 실행 중인지 확인
if [ -n "$BLUE_CONTAINER" ] || [ -n "$PORT_9090_CONTAINER" ]; then
  NEW_ENV="green"
  NEW_PORT="9091"
  OLD_ENV="blue"
  OLD_PORT="9090"
else
  NEW_ENV="blue"
  NEW_PORT="9090"
  OLD_ENV="green"
  OLD_PORT="9091"
fi

echo "배포 환경: $NEW_ENV (포트: $NEW_PORT)"
echo "Dockerfile 경로: $DOCKERFILE"
echo "환경 파일 경로: $ENV_FILE"

# 새 환경용 기존 컨테이너가 있으면 제거
docker rm -f profanity-$NEW_ENV || true

# 새로 배포할 포트를 점유하고 있는 프로세스가 있다면 죽이기
if command -v lsof >/dev/null 2>&1; then
  PORT_PID=$(lsof -ti:$NEW_PORT)
  if [ -n "$PORT_PID" ]; then
    echo "포트 $NEW_PORT를 사용 중인 프로세스(PID: $PORT_PID)를 종료합니다."
    kill -9 $PORT_PID || true
    sleep 2
  fi
fi

# 이미지 빌드 및 컨테이너 실행
echo "도커 이미지 빌드 중... (컨텍스트: $DOCKER_DIR)"
docker build -t profanity-$NEW_ENV:latest -f $DOCKERFILE $DOCKER_DIR

echo "컨테이너 실행 중..."
if [ -n "$ENV_PARAM" ]; then
  docker run -d \
    --name profanity-$NEW_ENV \
    --restart unless-stopped \
    -p $NEW_PORT:9999 \
    $ENV_PARAM \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e TZ=Asia/Seoul \
    profanity-$NEW_ENV:latest
else
  docker run -d \
    --name profanity-$NEW_ENV \
    --restart unless-stopped \
    -p $NEW_PORT:9999 \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e TZ=Asia/Seoul \
    profanity-$NEW_ENV:latest
fi

# 이전 환경 컨테이너 이름 변경 (나중에 삭제하기 위해)
OLD_CONTAINER=$(docker ps --format "{{.Names}}" --filter "publish=$OLD_PORT-$OLD_PORT" | grep -v "old_container_")
if [ -n "$OLD_CONTAINER" ]; then
  TIMESTAMP=$(date +%Y%m%d%H%M%S)
  NEW_NAME="old_container_$TIMESTAMP"

  echo "이전 환경 컨테이너($OLD_CONTAINER)의 이름을 $NEW_NAME으로 변경합니다."
  docker rename $OLD_CONTAINER $NEW_NAME
  echo "이름이 변경된 컨테이너는 나중에 다음 명령으로 삭제할 수 있습니다:"
  echo "docker rm -f $NEW_NAME"
fi

echo "$NEW_ENV 환경이 포트 $NEW_PORT에서 실행 중입니다."
echo "DEPLOY_RESULT:SUCCESS:$NEW_ENV:$NEW_PORT"
