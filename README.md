# 🎃 SpookyBirch

A **Birch-style overlay for the Hypixel SkyBlock Spooky Festival**. It puts a
clean, draggable HUD in the corner of your screen and gives you a quick
reference for the two things you actually care about during the fest: **which
mobs give the most candy**, and **what you can catch while spooky fishing**.

Built as a **Forge 1.8.9** client mod — the same version SkyBlock QoL mods
(SkyHanni, Skytils, NEU) run on.

> ⚠️ Not affiliated with Hypixel. Candy/fishing numbers are community-sourced
> **approximations** and easy to edit (see [Tuning the data](#tuning-the-data)).
> This is a **client-side overlay** — it only reads text already on your screen
> (scoreboard, action bar, your own inventory). It never automates gameplay.

## What the HUD shows

| Line | Where it comes from |
|------|---------------------|
| **Festival status + time left** | The SkyBlock sidebar scoreboard |
| **Mana `cur/max`** | The action bar (the `✎` mana readout) |
| **Green candy** (total + gained this session) | Counts *Green Candy* in your inventory |
| **Purple candy** (total + gained this session) | Counts *Purple Candy* in your inventory |
| **Spooky mobs nearby** | Counts spooky-named mobs within 30 blocks |
| **Best candy mob tip** | Top of the candy ranking |

## Commands

| Command | Does |
|---------|------|
| `/spooky` | Show help |
| `/spooky guide` | Open the candy + fishing reference GUI |
| `/spooky candy` | Print the best-candy-mob ranking to chat |
| `/spooky fishing` | Print the spooky sea-creature table to chat |
| `/spooky move` | Drag the HUD around, scroll to scale, Esc to save |
| `/spooky toggle` | Show/hide the HUD |
| `/spooky reset` | Reset the "gained this session" candy counters |

Aliases: `/spookybirch`, `/sb`. There's also a keybind (**default `G`**,
rebindable under Controls → SpookyBirch) that opens the guide.

## Which mobs give the most candy

Ranked by candy value (green + purple×8, since purple trades for ~8 green):

| Rank | Mob | Green/kill | Purple chance | Where |
|------|-----|-----------|---------------|-------|
| 1 | Nightmare | 6.0 | 6.0% | Hub at night (rare, miniboss-tier) |
| 2 | Werewolf | 4.0 | 4.0% | Hub / Spider's Den at night |
| 3 | Wither Gourd | 3.0 | 2.0% | Hub spooky spawns |
| 4 | Scary Jerry | 2.5 | 1.5% | Hub spooky spawns |
| 5 | Batty Witch | 2.2 | 1.2% | Hub spooky spawns |
| 6 | Phantom Spirit | 2.0 | 1.0% | Hub spooky spawns |
| 7 | Trick or Treater | 1.8 | 1.0% | Hub spooky spawns |
| … | Zombie/Skeleton (event) | 1.0 | 0.4% | Any zone (baseline) |

**Rule of thumb:** camp the **Hub during the fest**, prioritize the rare
**Nightmare/Werewolf** night spawns for big candy dumps, and clear the packs of
**Wither Gourds / Scary Jerrys** in between.

## Spooky fishing

Fish anywhere during the fest to hook spooky sea creatures:

| Creature | Approx chance | Notable drops |
|----------|---------------|---------------|
| Scarecrow | 30% | Green Candy, string |
| Nightmare | 22% | Green Candy, rare Nightmare pet |
| Werewolf | 20% | Green Candy, leather |
| Phantom Fisher | 15% | Green + Purple Candy, Fishing XP |
| Grim Reaper | 13% | Purple Candy, rare Reaper Mask |

**Grim Reaper** is the rarest and best for candy + gear.

## Building it

Requires a JDK 8. From the repo root:

```bash
# uses the old Gradle that ForgeGradle 2.1 (MC 1.8.9) needs
gradle setupDecompWorkspace    # first time only, downloads MC/Forge
gradle build
```

The finished mod jar lands in `build/libs/`. Drop it into your
`.minecraft/mods` folder for a **Forge 1.8.9** profile and launch.

> The repo ships the Gradle *wrapper config* but not the wrapper binary. If you
> don't have Gradle installed, run `gradle wrapper --gradle-version 4.10.3`
> once to generate `gradlew`, then use `./gradlew` instead.

## Tuning the data

All the numbers live in two files and nothing else references game constants:

- **Candy mobs** → `src/main/java/com/spookybirch/data/CandyData.java`
- **Fishing** → `src/main/java/com/spookybirch/data/FishingData.java`

Edit a row, rebuild, done — the HUD, the `/spooky` chat commands and the guide
GUI all read from those two lists.

## How it's laid out

```
com.spookybirch
├── SpookyBirch          main @Mod entry — registers everything
├── core/                SpookyState (live values) + SpookyConfig (saved settings)
├── event/               StateUpdater (fills state each tick) + KeyBinds
├── hud/                 SpookyHud (the corner overlay)
├── gui/                 MoveHudScreen (drag/scale) + GuideScreen (reference book)
├── command/             /spooky command
├── data/                CandyData / FishingData reference tables
└── util/                scoreboard + text parsing helpers
```

Every feature reads from `SpookyState`, which `StateUpdater` refreshes each
tick — so adding a new HUD line or data source stays a one-file change.
