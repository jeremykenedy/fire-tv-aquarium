#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"
python3 -m unittest -v test_install.py test_tooling.py
mkdir -p build/tests
javac -d build/tests src/com/jeremykenedy/firetv/aquarium/AquariumOptions.java \
  src/com/jeremykenedy/firetv/aquarium/AquariumRandomizer.java \
  src/com/jeremykenedy/firetv/aquarium/FishMotion.java tests/AquariumTest.java
java -cp build/tests com.jeremykenedy.firetv.aquarium.AquariumTest
