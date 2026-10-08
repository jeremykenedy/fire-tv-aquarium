#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$HERE"
case "$(uname -s):$(uname -m)" in
  Darwin:arm64) archive=gitleaks_8.30.1_darwin_arm64.tar.gz; digest=b40ab0ae55c505963e365f271a8d3846efbc170aa17f2607f13df610a9aeb6a5 ;;
  Linux:x86_64) archive=gitleaks_8.30.1_linux_x64.tar.gz; digest=551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb ;;
  *) echo "Run secret checks on macOS arm64 or Linux x86_64." >&2; exit 1 ;;
esac
mkdir -p build/tools/gitleaks
curl --proto '=https' --proto-redir '=https' -fsSL "https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/$archive" -o "build/tools/$archive"
python3 - "build/tools/$archive" "$digest" <<'PY'
import hashlib
from pathlib import Path
import sys
if hashlib.sha256(Path(sys.argv[1]).read_bytes()).hexdigest() != sys.argv[2]:
    raise SystemExit('Secret scanner checksum mismatch')
PY
tar -xzf "build/tools/$archive" -C build/tools/gitleaks gitleaks
build/tools/gitleaks/gitleaks git --redact --no-banner .
build/tools/gitleaks/gitleaks dir --redact --no-banner --config .gitleaks.toml .
