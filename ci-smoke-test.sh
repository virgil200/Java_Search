#!/usr/bin/env bash
set -euo pipefail

PORT="${PORT:-18080}"
LOG_FILE="/tmp/public-records-search-ci.log"

cleanup() {
  if [[ -n "${SERVER_PID:-}" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
    kill "$SERVER_PID" || true
  fi
}
trap cleanup EXIT

rm -f src/*.class
javac src/PublicRecordsSearchServer.java
java -cp src PublicRecordsSearchServer "$PORT" > "$LOG_FILE" 2>&1 &
SERVER_PID=$!

# Wait up to 10 seconds for the server.
for _ in {1..20}; do
  if curl -fsS "http://localhost:${PORT}/health" >/tmp/health.json 2>/dev/null; then
    break
  fi
  sleep 0.5
done

curl -fsS "http://localhost:${PORT}/" | grep -q "Public Records Due-Diligence Search"

curl -fsS -X POST "http://localhost:${PORT}/api/search" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'name=Juan Dela Cruz' \
  --data-urlencode 'country=PH' \
  --data-urlencode 'scope=court' \
  --data-urlencode 'scope=business' \
  --data-urlencode 'scope=credentials' \
  --data-urlencode 'purpose=Demo consented/self-check due diligence workflow' \
  --data-urlencode 'consent=yes' \
  --data-urlencode 'notMinor=yes' \
  --data-urlencode 'fairUse=yes' >/tmp/search.json

grep -q 'Philippine Supreme Court Jurisprudence' /tmp/search.json
grep -q 'PRC License Verification' /tmp/search.json

echo "Smoke test passed on http://localhost:${PORT}"
