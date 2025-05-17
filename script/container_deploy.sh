#!/bin/bash

# 현재 실행 중인 컨테이너와 포트 상태 확인
BLUE_CONTAINER=$(docker ps --filter "status=running" --filter "name=profanity-blue" -q)
GREEN_CONTAINER=$(docker ps --filter "status=running" --filter "name=profanity-green" -q)
PORT_9090_CONTAINER=$(docker ps --filter "status=running" --filter "publish=9090-9090" -q)
PORT_9091_CONTAINER=$(docker ps --filter "status=running" --filter "publish=9091-9091" -q)

# 다른 이름으로 포트를 사용 중인 컨테이너 확인
PORT_9090_OTHER=$(docker ps --format "{{.Names}}" --filter "publish=9090-9090" | grep -v "profanity-blue")
PORT_9091_OTHER=$(docker ps --format "{{.Names}}" --filter "publish=9091-9091" | grep -v "profanity-green")

echo "현재 상태:"
echo "- Blue 컨테이너: ${BLUE_CONTAINER:-없음}"
echo "- Green 컨테이너: ${GREEN_CONTAINER:-없음}"
echo "- 9090 포트 사용: ${PORT_9090_CONTAINER:-없음}"
echo "- 9091 포트 사용: ${PORT_9091_CONTAINER:-없음}"
if [ -n "$PORT_9090_OTHER" ]; then echo "- 9090 포트 사용 중인 다른 컨테이너: $PORT_9090_OTHER"; fi
if [ -n "$PORT_9091_OTHER" ]; then echo "- 9091 포트 사용 중인 다른 컨테이너: $PORT_9091_OTHER"; fi

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

# 새 포트를 사용 중인 다른 이름의 컨테이너가 있으면 제거
OTHER_CONTAINER=$(docker ps --format "{{.Names}}" --filter "publish=$NEW_PORT-$NEW_PORT" | grep -v "profanity-$NEW_ENV")
if [ -n "$OTHER_CONTAINER" ]; then
  echo "포트 $NEW_PORT를 사용 중인 다른 컨테이너 ($OTHER_CONTAINER)가 있습니다. 제거합니다."
  docker rm -f $OTHER_CONTAINER || true
fi

# 새 환경용 기존 컨테이너가 있으면 제거
docker rm -f profanity-$NEW_ENV || true

# 새로 배포할 포트를 점유하고 있는 프로세스가 있다면 죽이기
if command -v lsof >/dev/null 2>&1; then
  PORT_PID=$(lsof -ti:$NEW_PORT)
  if [ -n "$PORT_PID" ]; then
    echo "포트 $NEW_PORT를 사용 중인 프로세스(PID: $PORT_PID)를 종료합니다."
    kill -9 $PORT_PID || true
    # 프로세스가 종료되기를 잠시 기다림
    sleep 2
  fi
else
  echo "lsof 명령어가 설치되어 있지 않습니다. 포트 점유 프로세스를 확인할 수 없습니다."
  echo "sudo apt-get update && sudo apt-get install -y lsof 명령으로 설치하세요."
fi

# 이미지 빌드 및 컨테이너 실행
docker build -t profanity-$NEW_ENV:latest .
docker run -d \
  --name profanity-$NEW_ENV \
  --restart unless-stopped \
  -p $NEW_PORT:9999 \
  --env-file .env \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e TZ=Asia/Seoul \
  profanity-$NEW_ENV:latest

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
echo "프록시를 재시작한 후, 이름이 변경된 컨테이너를 확인하려면 다음 명령을 사용하세요:"
echo "docker ps -a --filter \"name=old_container_*\""
