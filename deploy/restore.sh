#!/usr/bin/env bash
# S3 백업을 DB에 복구한다. 기존 객체를 덮어쓰므로 새 서버 또는 복구 훈련에서만 쓴다.
# 사용: ./restore.sh backups/db-20261110T180000Z.dump
set -euo pipefail

KEY="${1:?사용법: restore.sh <s3 key>}"

cd /opt/job-tracker
set -a; source .env; set +a

aws s3 cp "s3://${S3_BUCKET}/${KEY}" - --region "${AWS_REGION:-ap-northeast-2}" \
  | docker compose -f docker-compose.prod.yml exec -T postgres \
      pg_restore -U "${PGUSER:-jobtracker}" -d "${PGDATABASE:-jobtracker}" --clean --if-exists --no-owner

echo "restore ok: ${KEY}"
