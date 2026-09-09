#!/usr/bin/env bash
# Compiles and runs the Minecraft-free tree-gift roll engine tests.
# No Fabric/Minecraft needed — this exercises the whole roll engine
# (Rarity, GiftConfig, LootTables, TreeGiftRoller) including a 1,000,000-roll
# statistical check that the published odds are real.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

javac -d "$OUT" \
  src/main/java/com/treegifts/core/*.java \
  tests/com/treegifts/core/TreeGiftRollerTest.java

java -cp "$OUT" com.treegifts.core.TreeGiftRollerTest
