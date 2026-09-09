# 🐉 DragonLoot (Fabric)

A **gambling-style dragon egg reveal** for Minecraft. Slay a summoned dragon and
a **Minecraft dragon egg gambles its way up the rarity ladder** with a
**slot-machine** animation:

> **Uncommon → Rare → Epic → Legendary**

The egg starts cracking at **Uncommon**. A rarity reel whips around and
**decelerates on an ease-out curve**, so the `tick… tick… tick` slows into real
suspense before it lands on your result. The egg shakes, cracks pile on, its glow
bleeds toward the final color — then it **breaks open on a beam of light** with a
particle burst and a reward card that springs up. Legendaries get a **JACKPOT**.

Client-side and **cosmetic only** — it watches chat to know when a dragon died
and never automates or touches your inventory. The reward is flavor.

Built for **Fabric**, Minecraft **26.1.2** (the version is a one-line knob — see
[Building](#building)).

## Test it right now

```
/dragongamble
```

Plays a full reveal on demand — no dragon required. Great for showing it off.

## Loot table

**Only these three are Legendary** (the jackpots):

| Legendary |
|-----------|
| 🐉 Ender Dragon Pet |
| 👤 Enderman Pet |
| 🎨 Pearlescent Dye |

Everything else is sorted below:

| Tier | Loot |
|------|------|
| **Epic** | Aspect of the Void · Superior Dragon Armor · Ender Relic |
| **Rare** | Aspect of the Dragons · Dragon Claw · Strong Dragon Armor |
| **Uncommon** | Dragon Horn · Dragon Scale · Ender Pearls · Dragon Fragment |

All of it lives in `data/RewardData.java` — edit a row and the reveal, the guide
and `/dragonloot` all update.

## Commands & keybind

| Command | Does |
|---------|------|
| `/dragongamble` | Play a reveal right now |
| `/dragonloot guide` | Odds + luck + rewards + stats GUI (also key **H**) |
| `/dragonloot stats` | Print your lifetime pulls |
| `/dragonloot odds <rare\|epic\|legendary> <%>` | Set a crack chance |
| `/dragonloot luck <x>` | Global luck multiplier |
| `/dragonloot sound` | Toggle reveal sounds |
| `/dragonloot auto` | Toggle auto-trigger on dragon death |
| `/dragonloot reset` | Clear stats |

Alias `/dl`. Default odds: Uncommon→Rare **50%**, Rare→Epic **30%**,
Epic→Legendary **15%** (so a Legendary is ~2.25% — genuinely rare). All persist
in `config/dragonloot.properties`.

## Building

You need **JDK 21**. From this folder (`dragonloot-fabric/`):

```bash
gradle wrapper --gradle-version 8.10.2   # first time, to get ./gradlew
./gradlew build
```

The finished mod jar lands in **`build/libs/dragonloot-1.0.0.jar`** — drop it
into your `.minecraft/mods` folder (Fabric profile, with **Fabric API**
installed) and launch.

### Setting your Minecraft version

The four coordinates in **`gradle.properties`** must match one real Minecraft
version. Look them up at **https://fabricmc.net/develop** — pick your version and
it prints the exact `yarn_mappings`, `loader_version` and `fabric_version`. They
ship set for 26.1.2; if Fabric hasn't published that exact version yet, use the
newest one listed there. If Loom is too old for a very new version, bump the
`fabric-loom` version in `build.gradle`.

### Prefer a download link?

Every push runs the GitHub Actions workflow **Build DragonLoot (Fabric)**, which
builds the jar and uploads it as a downloadable artifact (**Actions → the run →
Artifacts → DragonLoot-jar**). That only produces a jar once the coordinates
above resolve for the target version.

## Tests

The whole gambling core (odds, escalation, easing, loot table, stats) is
**dependency-free and deterministic**, so it's covered without launching the
game — including a **1,000,000-roll** stress run:

```bash
./scripts/run-tests.sh    # -> DragonLoot  PASSED: 46   FAILED: 0
```

## Layout

```
com.dragonloot
├── DragonLootClient        ClientModInitializer — keybind, chat trigger, commands
├── core/                   Rarity, RollEngine (the gamble), RollResult,
│                           DragonStats, DragonConfig, Easing (animation math)
├── data/                   RewardData / DragonReward (loot tables)
├── client/                 DragonReveal (kick-off), Sfx (sounds)
├── gui/                    EggRevealScreen (slot-machine reveal), DragonGuideScreen
└── render/                 EggRenderer (procedural egg + cracks — no textures)
```
