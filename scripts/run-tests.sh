#!/usr/bin/env bash
# Compiles and runs the Forge-free core-logic stress tests for both mods in this
# repo. No Minecraft/Forge needed — these exercise the pure logic (trackers,
# roll engine, data tables, formatting) with thousands/millions of events.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

echo "=== SpookyBirch core tests ==="
javac -d "$OUT" \
  src/main/java/com/spookybirch/core/CandyTracker.java \
  src/main/java/com/spookybirch/util/Fmt.java \
  src/main/java/com/spookybirch/util/TextUtil.java \
  src/main/java/com/spookybirch/data/*.java \
  tests/com/spookybirch/core/StressTest.java
java -cp "$OUT" com.spookybirch.core.StressTest

echo
echo "=== DragonLoot core tests ==="
# List the Forge-free core files explicitly (DragonLootConfig depends on Forge).
javac -d "$OUT" \
  src/main/java/com/dragonloot/core/Rarity.java \
  src/main/java/com/dragonloot/core/RollEngine.java \
  src/main/java/com/dragonloot/core/RollResult.java \
  src/main/java/com/dragonloot/core/DragonStats.java \
  src/main/java/com/dragonloot/data/DragonReward.java \
  src/main/java/com/dragonloot/data/RewardData.java \
  tests/com/dragonloot/core/DragonTest.java
java -cp "$OUT" com.dragonloot.core.DragonTest
