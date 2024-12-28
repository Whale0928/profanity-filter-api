#!/bin/bash
# script/redis-cluster-init.sh
# 기본값으로 실행 => ./script/redis-cluster-init.sh
# 환경변수 설정하여 실행 => REDIS_MASTER_HOST=192.168.1.10 REDIS_MASTER_PORT=16379 ./script/redis-cluster-init.sh

# 환경 변수 기본값 설정
REDIS_MASTER_HOST=${REDIS_MASTER_HOST:-"localhost"}
REDIS_MASTER_PORT=${REDIS_MASTER_PORT:-16379}
REDIS_REPLICA_HOST=${REDIS_REPLICA_HOST:-"localhost"}
REDIS_REPLICA_PORT=${REDIS_REPLICA_PORT:-16380}
REDIS_PASSWORD=${REDIS_PASSWORD:-"demo-mutil-redis-cluster"}

echo "Redis 클러스터 초기화를 시작합니다... (Starting Redis Cluster initialization...)"
echo "마스터 노드 (Master node): $REDIS_MASTER_HOST:$REDIS_MASTER_PORT"
echo "레플리카 노드 (Replica node): $REDIS_REPLICA_HOST:$REDIS_REPLICA_PORT"

# 노드가 준비될 때까지 대기
wait_for_redis() {
    local host=$1
    local port=$2
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if redis-cli -h $host -p $port -a $REDIS_PASSWORD ping >/dev/null 2>&1; then
            echo "Redis 노드 준비 완료 (Redis Ready): $host:$port"
            return 0
        fi
        echo "Redis 노드 대기 중 (Waiting for Redis): $host:$port (시도 횟수/attempts: $attempt/$max_attempts)..."
        sleep 2
        ((attempt++))
    done

    echo "Redis 노드 준비 실패 (Redis Failed): $host:$port"
    return 1
}

# 각 노드가 준비될 때까지 대기
wait_for_redis $REDIS_MASTER_HOST $REDIS_MASTER_PORT || exit 1
wait_for_redis $REDIS_REPLICA_HOST $REDIS_REPLICA_PORT || exit 1

# 클러스터 생성 시도
echo "Redis 클러스터 생성을 시작합니다... (Creating Redis Cluster...)"
redis-cli --cluster create \
    $REDIS_MASTER_HOST:$REDIS_MASTER_PORT \
    $REDIS_REPLICA_HOST:$REDIS_REPLICA_PORT \
    --cluster-replicas 1 \
    --pass $REDIS_PASSWORD \
    -a $REDIS_PASSWORD

if [ $? -eq 0 ]; then
    echo "Redis 클러스터가 성공적으로 초기화되었습니다. (Redis Cluster successfully initialized)"
else
    echo "Redis 클러스터 초기화 실패 (Failed to initialize Redis Cluster)"
    exit 1
fi

# 클러스터 상태 확인
echo "클러스터 상태를 확인합니다... (Checking cluster status...)"
redis-cli -h $REDIS_MASTER_HOST -p $REDIS_MASTER_PORT -a $REDIS_PASSWORD cluster info
