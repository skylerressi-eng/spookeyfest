# 🌳🎁 Tree Gifts — real Hypixel Tree Gift reveal

A **Fabric** client mod for **Minecraft 26.1.2** that turns your real Hypixel
SkyBlock **Tree Gift** drops into a dramatic reveal — Hardened Wood target, three
iron axes flying in, crack, break, and the **actual item Hypixel gave you**
bursting out with rarity-scaled glow and confetti.

> **The reward is never faked.** The mod does **not** roll its own loot. Hypixel's
> server decides the Tree Gift and prints it in chat; the mod reads that message,
> locks the real item + rarity, and the animation only controls *how* it's
> revealed. The animation is the exciting part — the drop is 100% the real one.

## Commands & keybind

- **`/treegifts`** (alias **`/tg`**) — replay your last real drop.
- **`/tg demo [rarity]`** — preview a reveal at any rarity (`common`…`special`).
- **`/tg stats`** / **`/tg reset`** — lifetime stats (per-rarity counts, best pull,
  rarest find) and clear them.
- **`/tg auto`** — toggle auto-reveal on a real Tree Gift.
- **`/tg threshold <rarity>`** — only auto-reveal that rarity **and above** (great
  for grinding: set `epic` so only exciting drops interrupt you).
- **`/tg sound`** — toggle the reveal sounds.
- **Key `G`** — replay the last drop (rebindable under Controls → Tree Gifts).

## Config

Settings + lifetime stats persist in `config/treegifts.properties`:
`autoReveal`, `minRarity` (0 Common … 6 Special), `playSounds`,
`particleScale` (0–2), `animationSpeed` (0.5–2). Higher rarities automatically get
a bigger reveal — more glow, particles and confetti, and a screen flash on
Legendary+.

## How it works (the pipeline)

```
You fell a tree on Galatea
        ↓
Hypixel sends the Tree Gift result in chat   (§2§l▬▬▬… TREE GIFT … ▬▬▬)
        ↓
TreeGiftChatListener rebuilds the §-formatted text (colour preserved)
        ↓
TreeGiftChatParser reads: item name, rarity (from the chat COLOUR), drop %, tree
        ↓
TreeGiftResult  ← immutable, locked (no randomness anywhere)
        ↓
RealTreeGiftReveal queues it → TreeGiftScreen animates THAT exact drop
        ↓
🪓🪓🪓 → 💥 → ✨ big reveal (rarity-scaled), then auto-closes
```

- **Rarity comes from the chat colour**, exactly as Hypixel sends it — §f Common,
  §a Uncommon, §9 Rare, §5 Epic, §6 Legendary, §d Mythic, §c Special. Higher
  rarity → bigger glow, more particles, more confetti, louder sound.
- **Only real Tree Gifts trigger it.** The parser requires the real bordered
  `TREE GIFT` block, so guild/party/NPC/other chat never fires a reveal (there are
  explicit no-false-positive tests).
- **Reveals the notable drops** — the rare "bonus gift" items and the phantom
  drops (Phanpyre/Phanflare/Dreadwing), i.e. the exciting ones. Mundane
  essence/XP gifts don't interrupt you.
- **Queue + de-dupe**: several notable drops from one gift are revealed in order;
  a duplicated chat message won't double-fire.
- **Keybind `G`** replays the *last real drop* (never a fake one), rebindable
  under Controls → Tree Gifts.

## Verified against the real chat format

The parser's format is taken from **SkyHanni's** Tree Gift parser (the
authoritative one), and the test suite feeds SkyHanni's own example chat lines to
prove we read them correctly — including reading rarity from the colour:

```bash
./scripts/run-tests.sh
#   TreeGiftChatParserTest : PASSED 23   FAILED 0
#   TreeGiftRollerTest     : PASSED 42   FAILED 0   (legacy demo engine, preserved)
```

`TreeGiftChatParserTest` checks the full block parses (Stretching Sticks =
Uncommon, Tree the Fish = Special, Phanpyre = Mythic phantom, Enchanted Book with
nested parens, tree type = Fig, …), that rarity follows the colour, and that
unrelated chat and header-less bordered blocks are ignored.

## Layout

```
com.treegifts
├── core/                       ← NO Minecraft dependency, fully unit-tested
│   ├── Rarity                  SkyBlock rarities + colour-code → rarity
│   ├── TreeGiftChatParser      parses Hypixel's real Tree Gift chat block
│   ├── TreeGiftResult          the immutable, locked real drop
│   ├── ItemIcons               real loot → vanilla render stand-in (centralised)
│   └── (legacy demo engine: GiftConfig/GiftStep/GiftResult/Loot/LootTables/
│        TreeGiftRoller — kept, not used by the real reveal)
└── client/
    ├── TreeGiftChatListener    chat event → §-string → parser → queue
    ├── RealTreeGiftReveal      the reveal queue (sequential, de-duped)
    ├── TreeGiftScreen          the animation (Hardened Wood, iron axes, reveal)
    └── TreeGiftsClient         registers the listener + the replay keybind
```

All the parsing/data logic is pure Java (`core/`) so it's tested without launching
the game; only `client/` touches the Minecraft 26.x API.

## Build

Requires **JDK 25** (Minecraft 26.x runs on Java 25). From this folder:

```bash
./gradlew build      # jar lands in build/libs/
```

Drop the jar into `.minecraft/mods` for a **Fabric 26.1.2** profile, alongside
**Fabric API**. A prebuilt jar is in [`dist/`](dist/).

Version coordinates (all real, published) live in
[`gradle.properties`](gradle.properties). Minecraft 26.x ships **deobfuscated**
(no Yarn/mappings), so `build.gradle` uses no `mappings` line — matching the
official fabric-example-mod.
