#!/bin/bash

# 환경 변수 설정
NGINX_CONFIG_PATH="/etc/nginx/sites-enabled/api.profanity-filter.run"

# 현재 활성화된 환경 확인
if grep -q "^    server 127.0.0.1:9997;" $NGINX_CONFIG_PATH && grep -q "^    server 127.0.0.1:9996;" $NGINX_CONFIG_PATH; then
    ACTIVE_ENV="green"
    INACTIVE_ENV="blue"
else
    ACTIVE_ENV="blue"
    INACTIVE_ENV="green"
fi

echo "현재 활성화 된 환경: $ACTIVE_ENV"
echo "비 활성화 된 환경: $INACTIVE_ENV"
