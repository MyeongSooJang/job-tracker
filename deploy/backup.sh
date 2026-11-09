#!/usr/bin/env bash
# 매일 DB를 덤프해 S3에 올린다. 서버 cron 으로 실행: 30 14 * * * /opt/ssangtteongnip/backup.sh  (UTC 14:30 = KST 23:30)
# 서버를 KST 00:00~05:50에 꺼 둘 계획이라, 꺼지기 직전 시각으로 잡는다. 새벽 시간대로 옮기면 백업이 돌지 않는다.
set -euo pipefail

cd /opt/ssangtteongnip
set -a; source .env; set +a

STAMP=$(date -u +%Y%m%dT%H%M%SZ)
KEY="backups/db-${STAMP}.dump"

# 덤프가 중간에 실패하면 빈 파일이 올라가지 않도록 pipefail 로 전체를 실패 처리한다.
docker compose -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U "${PGUSER:-ssangtteongnip}" -d "${PGDATABASE:-ssangtteongnip}" -Fc \
  | aws s3 cp - "s3://${S3_BUCKET}/${KEY}" --region "${AWS_REGION:-ap-northeast-2}"

echo "backup ok: s3://${S3_BUCKET}/${KEY}"
