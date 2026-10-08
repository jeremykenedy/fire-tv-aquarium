#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$HERE"
FORMAT_JAR="$HERE/build/tools/google-java-format.jar"
FORMAT_SHA="834b2a0c38cb774953322a84b5ca3f2f40dd3156650b3cd44d3b744345962f7a"
mkdir -p build/tools
if [[ ! -f "$FORMAT_JAR" ]]; then
  curl --proto '=https' --proto-redir '=https' -fsSL https://github.com/google/google-java-format/releases/download/v1.37.0/google-java-format-1.37.0-all-deps.jar -o "$FORMAT_JAR"
fi
python3 - "$FORMAT_JAR" "$FORMAT_SHA" <<'PY'
import hashlib
from pathlib import Path
import sys
if hashlib.sha256(Path(sys.argv[1]).read_bytes()).hexdigest() != sys.argv[2]:
    raise SystemExit('Java formatter checksum mismatch')
PY
java_files=()
while IFS= read -r file; do java_files+=("$file"); done < <(find src tests device-tests -name '*.java' | sort)
java -jar "$FORMAT_JAR" --aosp --dry-run --set-exit-if-changed "${java_files[@]}"
ruff check .
ruff format --check .
shellcheck build.sh test.sh scripts/*.sh
