#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$HERE"
if [[ -z "${CODACY_PROJECT_TOKEN:-}" ]]; then
  echo "CODACY_PROJECT_TOKEN is required for coverage reporting" >&2
  exit 1
fi
REPORTER="$HERE/build/tools/codacy-coverage-reporter.jar"
REPORTER_SHA="c33fb4f6e469c6c221a74c5fe2c251d22302f91a3b7a9a0259386a71bdda62a5"
mkdir -p build/tools
if [[ ! -f "$REPORTER" ]]; then
  curl --proto '=https' --proto-redir '=https' -fsSL \
    https://github.com/codacy/codacy-coverage-reporter/releases/download/14.1.3/codacy-coverage-reporter-assembly.jar \
    -o "$REPORTER"
fi
python3 - "$REPORTER" "$REPORTER_SHA" <<'PY'
import hashlib
from pathlib import Path
import sys
if hashlib.sha256(Path(sys.argv[1]).read_bytes()).hexdigest() != sys.argv[2]:
    raise SystemExit('Codacy coverage reporter checksum mismatch')
PY
REVISION="$(git rev-parse HEAD)"
java -jar "$REPORTER" report --commit-uuid "$REVISION" \
  --language Java --force-coverage-parser jacoco \
  --coverage-reports build/coverage/jacoco.xml --prefix src --partial
java -jar "$REPORTER" report --commit-uuid "$REVISION" \
  --language Python --force-coverage-parser cobertura \
  --coverage-reports coverage.xml --partial
java -jar "$REPORTER" final --commit-uuid "$REVISION"
