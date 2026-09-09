#!/usr/bin/env bash
# Compiles and runs DragonLoot's dependency-free core tests (no Minecraft/Fabric).
# Exercises the roll engine, easing, reward tables and stats — including a
# 1,000,000-roll stress run.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

echo "=== DragonLoot core tests ==="
javac -d "$OUT" \
  src/main/java/com/dragonloot/core/Rarity.java \
  src/main/java/com/dragonloot/core/RollEngine.java \
  src/main/java/com/dragonloot/core/RollResult.java \
  src/main/java/com/dragonloot/core/DragonStats.java \
  src/main/java/com/dragonloot/core/Easing.java \
  src/main/java/com/dragonloot/data/DragonReward.java \
  src/main/java/com/dragonloot/data/RewardData.java \
  src/test/java/com/dragonloot/core/DragonTest.java
java -cp "$OUT" com.dragonloot.core.DragonTest
