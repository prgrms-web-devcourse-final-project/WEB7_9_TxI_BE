#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🔧 .env 로드..."
if [ -f .env ]; then
  export $(grep -v '^#' .env | grep -v '^$' || true)
fi

echo ""
echo "📥 1) docker compose 최신 이미지 pull + up"
# compose v2 기준: --pull always 로 항상 최신 이미지 사용
docker compose up -d --pull always --remove-orphans

echo ""
echo "✅ 완료!"
echo "  - 백엔드: http://localhost:${BACKEND_PORT:-8080}"
echo "  - Postgres: localhost:${POSTGRES_PORT:-5432}"