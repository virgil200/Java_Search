#!/usr/bin/env bash
set -euo pipefail
PORT="${1:-8080}"
javac src/PublicRecordsSearchServer.java
java -cp src PublicRecordsSearchServer "$PORT"
