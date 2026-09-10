#!/usr/bin/env bash
# Compiles and runs the Minecraft-free core tests:
#   - TreeGiftChatParserTest : parses Hypixel's REAL Tree Gift chat format
#                              (verified against SkyHanni's example lines) and
#                              reads rarity from the chat colour.
#   - TreeGiftRollerTest     : the legacy demo roller's odds/shape.
# No Fabric/Minecraft needed.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

javac -encoding UTF-8 -d "$OUT" \
  src/main/java/com/treegifts/core/*.java \
  tests/com/treegifts/core/TreeGiftChatParserTest.java \
  tests/com/treegifts/core/TreeGiftRollerTest.java

echo "=== TreeGiftChatParserTest ==="
java -cp "$OUT" com.treegifts.core.TreeGiftChatParserTest
echo
echo "=== TreeGiftRollerTest ==="
java -cp "$OUT" com.treegifts.core.TreeGiftRollerTest
