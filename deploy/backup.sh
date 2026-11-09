#!/usr/bin/env bash
# 매일 DB를 덤프해 S3에 올린다. 서버 cron 으로 실행: 0 18 * * * /opt/job-tracker/backup.sh  (UTC 18시 = KST 03시)
set -euo pipefail

cd /opt/job-tracker
set -a; source .env; set +a

STAMP=$(date -u +%Y%m%dT%H%M%SZ)
KEY="backups/db-${STAMP}.dump"

# 덤프가 중간에 실패하면 빈 파일이 올라가지 않도록 pipefail 로 전체를 실패 처리한다.
docker compose -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U "${PGUSER:-jobtracker}" -d "${PGDATABASE:-jobtracker}" -Fc \
  | aws s3 cp - "s3://${S3_BUCKET}/${KEY}" --region "${AWS_REGION:-ap-northeast-2}"

echo "backup ok: s3://${S3_BUCKET}/${KEY}"
