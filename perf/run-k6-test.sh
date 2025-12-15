#!/bin/bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ENV_FILE="$SCRIPT_DIR/.env"

# Load environment variables if exists
if [ -f "$ENV_FILE" ]; then
  export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

# Args
SCENARIO=${1:-loadtest.js}
VUS=${2:-10}
DURATION=${3:-10s}
ROOM_ID=${4:-1}
BASE_URL=${5:-http://host.docker.internal:8080}
ENABLE_LOG=${6:-true}

# Test naming
TEST_RAW=$(basename "$SCENARIO" .js)          # e.g. getRoom.test
TEST_NAME=${TEST_RAW%.test}                   # e.g. getRoom
TEST_DATE=$(date +"%Y%m%d")
TEST_ID=$(date +"%H%M%S")

# Folder structure (LOCAL)
RESULT_DIR="$SCRIPT_DIR/results/${TEST_DATE}-${TEST_NAME}"
LOG_DIR="$RESULT_DIR/logs"
REPORT_DIR="$RESULT_DIR/reports"
CSV_DIR="$RESULT_DIR/csv"

mkdir -p "$LOG_DIR" "$REPORT_DIR" "$CSV_DIR"

echo "🚀 Starting K6 load test"
echo "   • Scenario : $SCENARIO"
echo "   • VUS      : $VUS"
echo "   • Duration : $DURATION"
echo "   • Output   : $RESULT_DIR"
echo "---------------------------------------------"

# Run test inside Docker
docker compose run --rm -T \
  -v "$SCRIPT_DIR":/scripts \
  -e TEST_ID="$TEST_ID" \
  -e JWT_SECRET="$JWT_SECRET" \
  -e VUS="$VUS" \
  -e DURATION="$DURATION" \
  -e ROOM_ID="$ROOM_ID" \
  -e BASE_URL="$BASE_URL" \
  -e ENABLE_LOG="$ENABLE_LOG" \
  -e K6_PROMETHEUS_RW_SERVER_URL="http://prometheus:9090/api/v1/write" \
  k6 run \
    --summary-trend-stats="avg,min,max,p(90),p(95),p(99)" \
    --out "dashboard=host=0.0.0.0&port=5665&period=2s&open=false&export=/scripts/results/${TEST_DATE}-${TEST_NAME}/reports/xk6_${TEST_NAME}_${TEST_DATE}_${TEST_ID}.html" \
    --out "csv=/scripts/results/${TEST_DATE}-${TEST_NAME}/csv/k6_metrics_${TEST_NAME}_${TEST_DATE}_${TEST_ID}.csv" \
    -o experimental-prometheus-rw \
    /scripts/k6-scripts/tests/${SCENARIO} \
    | tee "${LOG_DIR}/k6_${TEST_NAME}_${VUS}vus_${DURATION}_${TEST_DATE}_${TEST_ID}.log"

echo ""
echo "✅ Test completed!"
echo "📁 Results saved in:"
echo "   ├─ Logs:     ${LOG_DIR}"
echo "   ├─ Reports:  ${REPORT_DIR}"
echo "   └─ CSV:      ${CSV_DIR}"
echo "---------------------------------------------"