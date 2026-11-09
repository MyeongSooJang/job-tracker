#!/usr/bin/env bash
# Ubuntu LTS(arm64/x86_64) EC2 최초 1회 설정. SSM Session Manager 접속 후 root 로 실행한다.
#   sudo bash setup-server.sh
# 여러 번 실행해도 안전하도록(멱등) 이미 있는 항목은 건너뛴다.
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "root 로 실행하세요: sudo bash $0" >&2
  exit 1
fi

APP_DIR=/opt/job-tracker
SWAP_SIZE=2G
export DEBIAN_FRONTEND=noninteractive

echo "==> 기본 패키지"
apt-get update -y
apt-get install -y ca-certificates curl unzip cron openssl

echo "==> Docker (공식 apt 저장소)"
if ! command -v docker >/dev/null 2>&1; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

# 컨테이너 로그가 디스크를 채우지 않도록 크기를 제한한다.
if [[ ! -f /etc/docker/daemon.json ]]; then
  cat > /etc/docker/daemon.json <<'JSON'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
JSON
  systemctl restart docker
fi
systemctl enable --now docker

echo "==> AWS CLI v2"
if ! command -v aws >/dev/null 2>&1; then
  case "$(uname -m)" in
    aarch64) ARCH=aarch64 ;;
    x86_64)  ARCH=x86_64 ;;
    *) echo "지원하지 않는 아키텍처: $(uname -m)" >&2; exit 1 ;;
  esac
  TMP=$(mktemp -d)
  curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-${ARCH}.zip" -o "$TMP/awscliv2.zip"
  unzip -q "$TMP/awscliv2.zip" -d "$TMP"
  "$TMP/aws/install"
  rm -rf "$TMP"
fi

echo "==> 스왑 ${SWAP_SIZE}"
if ! swapon --show | grep -q '/swapfile'; then
  if [[ ! -f /swapfile ]]; then
    fallocate -l "$SWAP_SIZE" /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
  fi
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
# 메모리가 정말 부족할 때만 스왑을 쓰도록 한다.
echo 'vm.swappiness=10' > /etc/sysctl.d/99-swappiness.conf
sysctl -p /etc/sysctl.d/99-swappiness.conf >/dev/null

echo "==> 앱 디렉터리와 .env"
mkdir -p "$APP_DIR/pgdata"
if [[ ! -f "$APP_DIR/.env" ]]; then
  # PGPASSWORD 는 Postgres 최초 기동 때 DB에 박히므로 이후 바꾸지 않는다.
  cat > "$APP_DIR/.env" <<EOF
APP_IMAGE=
API_DOMAIN=

PGDATABASE=ssangtteongnip
PGUSER=ssangtteongnip
PGPASSWORD=$(openssl rand -hex 24)

JWT_SECRET=$(openssl rand -hex 32)
MAIL_FROM=
GMAIL_CLIENT_ID=
GMAIL_CLIENT_SECRET=
GMAIL_REFRESH_TOKEN=

S3_BUCKET=
AWS_REGION=ap-northeast-2
EOF
  chmod 600 "$APP_DIR/.env"
  echo "   .env 골격을 만들었습니다. 빈 항목을 채우세요: nano $APP_DIR/.env"
else
  echo "   .env 가 이미 있어 건드리지 않았습니다."
fi

echo "==> 백업 cron (UTC 14:30 = KST 23:30, 서버를 끄기 직전)"
cat > /etc/cron.d/job-tracker-backup <<'CRON'
30 14 * * * root /opt/job-tracker/backup.sh >> /var/log/job-tracker-backup.log 2>&1
CRON
chmod 644 /etc/cron.d/job-tracker-backup
systemctl enable --now cron

echo
echo "==> 확인"
docker --version
docker compose version
aws --version
swapon --show
# Ubuntu 공식 AMI는 SSM 에이전트가 snap 으로 들어 있다.
(snap list amazon-ssm-agent 2>/dev/null || systemctl status amazon-ssm-agent --no-pager 2>/dev/null | head -3) || echo "SSM 에이전트 상태를 확인하세요"
echo
echo "완료. 다음: $APP_DIR/.env 의 빈 항목(MAIL_FROM, GMAIL_*, S3_BUCKET)을 채운 뒤 배포하세요."
