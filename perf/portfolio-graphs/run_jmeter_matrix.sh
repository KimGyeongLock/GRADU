#!/usr/bin/env bash
set -euo pipefail

TAG="${1:-redis}"   # redis or mysql
JMX="/Users/admin/Desktop/perf-results/${TAG}/ranking-${TAG}.jmx"
OUT_DIR="/Users/admin/Desktop/perf-results/${TAG}"
USERS_LIST=(5 10 20 30 50)
DURATION=240

BASE_URL="${BASE_URL:-http://localhost:8080}"
LOGIN_EMAIL="${LOGIN_EMAIL:?set LOGIN_EMAIL}"
LOGIN_PASSWORD="${LOGIN_PASSWORD:?set LOGIN_PASSWORD}"

mkdir -p "${OUT_DIR}"

get_access_token() {
  local resp token
  resp=$(curl -s -X POST "${BASE_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${LOGIN_EMAIL}\",\"password\":\"${LOGIN_PASSWORD}\"}")

  token=$(python3 - <<'PY' "$resp"
import json,sys
d=json.loads(sys.argv[1])
print(d.get("accessToken",""))
PY
)
  if [ -z "$token" ]; then
    echo "failed to get access token"
    echo "$resp"
    exit 1
  fi
  echo "$token"
}

for U in "${USERS_LIST[@]}"; do
  echo "== ${TAG} | users=${U} =="

  ACCESS_TOKEN="$(get_access_token)"

  jmeter -n \
    -t "${JMX}" \
    -Jusers="${U}" \
    -Jduration="${DURATION}" \
    -Jaccess_token="${ACCESS_TOKEN}" \
    -Jjmeter.save.saveservice.output_format=csv \
    -l "${OUT_DIR}/results_${U}.jtl"
done

echo "done: ${OUT_DIR}"
