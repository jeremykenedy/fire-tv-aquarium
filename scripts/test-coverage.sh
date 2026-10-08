#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$HERE"
GRADLE_VERSION=8.14.3
GRADLE_SHA=bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531
ARCHIVE="$HERE/build/tools/gradle-$GRADLE_VERSION-bin.zip"
mkdir -p build/tools build/coverage
if [[ ! -f "$ARCHIVE" ]]; then
  curl --proto '=https' --proto-redir '=https' -fsSL --retry 3 --connect-timeout 30 \
    "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"
fi
python3 - "$ARCHIVE" "$GRADLE_SHA" <<'PY'
import hashlib
import sys
from pathlib import Path
if hashlib.sha256(Path(sys.argv[1]).read_bytes()).hexdigest() != sys.argv[2]:
    raise SystemExit('Coverage runner checksum mismatch')
PY
if [[ ! -d "build/tools/gradle-$GRADLE_VERSION" ]]; then
  unzip -q "$ARCHIVE" -d build/tools
fi
python3 - <<'PY'
import xml.etree.ElementTree as ET
ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
manifest = ET.parse('AndroidManifest.xml')
manifest.getroot().attrib.pop('package')
manifest.write('build/coverage/AndroidManifest.xml', encoding='utf-8', xml_declaration=True)
PY
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}" \
  "build/tools/gradle-$GRADLE_VERSION/bin/gradle" --no-daemon --console=plain \
    --gradle-user-home "$HERE/build/coverage/gradle-cache" -p coverage coverageReport
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
report = ET.parse('build/coverage/jacoco.xml').getroot()
expected = {str(path.relative_to('src')) for path in Path('src').rglob('*.java')}
reported = {package.get('name') + '/' + source.get('name')
            for package in report.findall('package') for source in package.findall('sourcefile')}
if expected != reported:
    raise SystemExit(f'Java coverage source mismatch: {expected ^ reported}')
counters = {counter.get('type'): counter for counter in report.findall('counter')}
for kind in ('LINE', 'BRANCH'):
    counter = counters[kind]
    if int(counter.get('missed')) or not int(counter.get('covered')):
        raise SystemExit(f'Java {kind} coverage is below 100%')
    print(f'Java {kind}: {counter.get("covered")} covered, zero missed')
PY
python3 -m venv build/coverage/python
COVERAGE_SPEC="$(sed -n '/^coverage==/p' requirements-dev.txt)"
build/coverage/python/bin/python -m pip install --disable-pip-version-check "$COVERAGE_SPEC"
build/coverage/python/bin/python -m coverage run --branch \
  --include='install.py,check_apk.py,scripts/*.py' -m unittest test_install test_tooling
build/coverage/python/bin/python -m coverage report --fail-under=100
build/coverage/python/bin/python -m coverage xml
build/coverage/python/bin/python - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
report = ET.parse('coverage.xml').getroot()
expected = {'install.py', 'check_apk.py'} | {str(path) for path in Path('scripts').glob('*.py')}
reported = {source.get('filename') for source in report.findall('.//class')}
if expected != reported:
    raise SystemExit(f'Python coverage source mismatch: {expected ^ reported}')
for kind in ('lines', 'branches'):
    covered, total = int(report.get(kind + '-covered')), int(report.get(kind + '-valid'))
    if covered != total or not total:
        raise SystemExit(f'Python {kind} coverage is below 100%')
PY
