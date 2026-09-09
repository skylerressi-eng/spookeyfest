#!/usr/bin/env bash
# Compiles and runs the Forge-free core-logic stress test.
# No Minecraft/Forge needed — this exercises CandyTracker, Fmt, TextUtil and the
# data tables with thousands of simulated events and edge cases.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

javac -d "$OUT" \
  src/main/java/com/spookybirch/core/CandyTracker.java \
  src/main/java/com/spookybirch/util/Fmt.java \
  src/main/java/com/spookybirch/util/TextUtil.java \
  src/main/java/com/spookybirch/data/*.java \
  tests/com/spookybirch/core/StressTest.java

java -cp "$OUT" com.spookybirch.core.StressTest
