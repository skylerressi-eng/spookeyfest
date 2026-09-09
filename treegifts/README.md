# 🌳🎁 Tree Gifts

A **Fabric** client mod that turns every tree you chop into a Hypixel
SkyBlock-style **gift reveal**. Chop a log and a wrapped **Hardened Wood** drops
onto the screen — then axes come flying in and *thud* into it, one at a time.

Each hit does one of two things:

- **Upgrades** the wood to a higher rarity (it glows brighter and cracks a
  little more), or
- **Cracks it open** right there and pays out at whatever rarity it's on.

So it climbs **Common → Uncommon → Rare → Epic → Legendary**, and it can crack
open **at any tier** — if it stops at Rare, you get Rare loot. Reach the top and
one more throw might make the wood **spin** and transform into the jackpot: a
**Mango Dye**. 🥭

> ⚠️ Not affiliated with Hypixel. This is a **client-side, cosmetic reveal** — a
> fun animation with its own loot roll. It doesn't touch your real inventory or
> automate anything; the "loot" is a celebratory pop-up.

## How to trigger a reveal

| Trigger | Where it works |
|---------|----------------|
| **Chop any log** (oak, birch, jungle, …) | Singleplayer / your own world |
| **Press `G`** (rebindable: Controls → Tree Gifts) | Anywhere — including on a server |

On a real multiplayer server a client-only mod can't always see block breaks, so
the **keybind** is the reliable way to pop a gift (and to just enjoy the
animation). While a reveal is playing, **click to skip** to the reward; on the
reward screen, **click or press a key to collect** (it prints the result to chat)
and close.

## The rarities & odds

The first axe reveals a **Common** wood. Every axe after that has a chance to
keep climbing; otherwise the wood cracks and pays out. The defaults:

| From | Chance the next axe **climbs** |
|------|-------------------------------|
| Common | 70% |
| Uncommon | 55% |
| Rare | 35% |
| Epic | 20% |
| Legendary | — (instead: **5%** to spin into a **Mango Dye**) |

Which works out to roughly this per gift (verified over a million rolls in the
test suite):

| Outcome | Chance |
|---------|--------|
| Common | ~30% |
| Uncommon | ~31.5% |
| Rare | ~25% |
| Epic | ~10.8% |
| **Legendary** | ~2.6% |
| **🥭 Mango Dye** | ~0.13% (1 in ~740) |

Every tier has its own little loot table (Hardened Wood, Enchanted Wood, coins,
Treecapitator, a Jungle Key, pets…). Tune all of it in
[`GiftConfig`](src/main/java/com/treegifts/core/GiftConfig.java) and
[`LootTables`](src/main/java/com/treegifts/core/LootTables.java).

## How it's laid out

```
com.treegifts
├── TreeGiftsMod              main entrypoint (does almost nothing)
├── core/                     ← NO Minecraft dependency, fully unit-tested
│   ├── Rarity                the Common→Legendary ladder + SkyBlock colours
│   ├── GiftConfig            the tunable odds
│   ├── GiftStep / GiftResult the reveal, modelled as data
│   ├── Loot / LootTables     rewards per tier + the Mango jackpot
│   └── TreeGiftRoller        rolls a whole outcome up front (injectable RNG)
└── client/
    ├── TreeGiftsClient       keybind + log-break detection → opens the screen
    └── TreeGiftScreen        the animation: axe throws, cracks, spin, confetti
```

The design mirrors the sibling **SpookyBirch** mod in this repo: **all the game
logic is pure Java** (`core/`) so it can be tested without launching Minecraft,
and only `client/` touches the Fabric/rendering API. The screen never decides an
outcome — it just *plays* the `GiftResult` the roller already rolled, which is
what makes the whole thing deterministic and testable.

## Tests

The roll engine has no Minecraft dependency, so it's covered by a plain-Java
suite you can run without Fabric — including a **1,000,000-roll** statistical
check that the published odds above are real:

```bash
./scripts/run-tests.sh
```

It asserts the rarity ladder only ever climbs, every gift pays out exactly once,
a Mango only ever comes off a Legendary, seeded rolls are reproducible, and the
full outcome distribution lands within tolerance. All green:

```
PASSED: 41   FAILED: 0
```

## Installing (just play it)

Grab `treegifts-<version>.jar` from `build/libs/` (or the copy handed to you) and
drop it into your `.minecraft/mods` folder for a **Fabric** profile, alongside
**[Fabric API](https://modrinth.com/mod/fabric-api)**. Launch Minecraft **26.1.2
(Fabric)** — chop a tree or press **G**.

Requirements: **Minecraft 26.1.2**, **Fabric Loader** ≥ 0.19.3, **Fabric API**,
and Java **25** (bundled with the 26.x launcher profile).

## Building it yourself

26.x needs **JDK 25** and **Gradle 9.5+** (a wrapper pinned to 9.5 is committed).
From this `treegifts/` folder:

```bash
JAVA_HOME=/path/to/jdk-25 ./gradlew build
```

The finished jar lands in `build/libs/treegifts-1.0.0.jar`.

> **Version note — how 26.x differs from older Fabric.** This mod is built for a
> real, published **Minecraft 26.1.2**. Three things changed in the 26.x era and
> are already handled in this project:
> 1. **No Yarn / no obfuscation mappings.** Mojang ships 26.x *deobfuscated*, so
>    `build.gradle` has **no `mappings` line** at all — you compile straight
>    against the named jar.
> 2. **Java 25** is required (26.x bumped the JDK).
> 3. **New GUI system.** Screens draw via `extractRenderState(GuiGraphicsExtractor)`
>    with a JOML `Matrix3x2fStack`, and keybinds register through
>    `KeyMappingHelper` — see
>    [`TreeGiftScreen`](src/main/java/com/treegifts/client/TreeGiftScreen.java) and
>    [`TreeGiftsClient`](src/main/java/com/treegifts/client/TreeGiftsClient.java).
>
> All version numbers live in [`gradle.properties`](gradle.properties); check the
> current set at <https://fabricmc.net/develop> and
> <https://modrinth.com/mod/fabric-api/versions>.
