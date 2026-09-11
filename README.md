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
| **Candy score + live rate** | Weighted score & candy/hr (see below) |
| **Goal ETA** | Time left to your candy-score goal at the current rate |
| **Spooky mobs nearby** | Counts spooky-named mobs within 30 blocks |
| **Best candy mob tip** | Top of the candy ranking |

## Candy score tracker

The tracker watches your candy totals and counts **only the gains** — so
trading candy away or selling it never drags your session numbers backwards.
From that it works out:

- **Candy score** — `green + purple × weight` (weight defaults to 8, since a
  purple candy is worth roughly 8 green when trading). Tune with
  `purpleWeight` in the config.
- **Rate (recent)** — candy score per hour over the **last 5 minutes**, so it
  reacts when you switch spots or the fest heats up.
- **Rate (session)** — candy score per hour across the whole session.
- **Best rate** — the highest recent rate you've hit this session.
- **Idle warning** — if no candy comes in for 30s+, the HUD nudges you.
- **Goal + ETA** — set a target with `/spooky goal <n>` and the HUD/stats show
  how long until you reach it at your current rate.

See it all live in **`/spooky stats`** or the **My Score** tab of `/spooky guide`.
Reset a session with **`/spooky reset`**.

## ⚒ Blacksmith reforge gamble (cinematic)

A dramatic, **purely visual** overlay for the normal SkyBlock **Apply Reforge**
action. When you reforge an item at the Blacksmith, the mod plays a three-hit
anvil cinematic that flashes through believable fake reforges and then **lands on
the exact reforge Hypixel actually applied**, before the anvil cracks, shatters
and reveals the result.

> **The server stays authoritative.** The mod never invents a reforge, never
> sends a packet, never touches your coins or inventory. It reads the real result
> back from the item's NBT (`ExtraAttributes.modifier`) and the animation *ends*
> on it — it never decides it. If the result can't be confirmed (you close the
> GUI, the reforge fails, you can't afford it), **no reveal is shown**.

- **Item-aware fakes** — the two intermediate reforges are pulled from the
  *current* basic-Blacksmith pool for that item's category (sword, bow, armor,
  equipment, axe, hoe, pickaxe). See [`docs/REFORGES.md`](docs/REFORGES.md) for
  the full researched data (Sept 2026, community wiki fork).
- **Three hits** — clang, clang, then a heavy finale; sparks, screen shake, the
  anvil cracks and breaks into fragments, then the real reforge glows in.
- **No assets** — the whole thing is drawn with GL primitives, so there's nothing
  to ship and it runs identically on any 1.8.9 client.

Configure or preview it:

| Command | Does |
|---------|------|
| `/spooky reforge` | Show status |
| `/spooky reforge test` | Play a preview (random reforges — not a real item) |
| `/spooky reforge on` / `off` | Toggle the cinematic |
| `/spooky reforge sounds` | Toggle anvil sounds |
| `/spooky reforge motion` | Toggle reduced motion (less shake/particles, shorter) |
| `/spooky reforge speed <0.5–3.0>` | Animation speed |

All of these persist in `config/spookybirch.cfg` under the `reforge` section.

## Commands

| Command | Does |
|---------|------|
| `/spooky` | Show help |
| `/spooky guide` | Open the score + candy + fishing GUI (3 tabs) |
| `/spooky stats` | Print your candy score, rate & ETA to chat |
| _(tab-complete)_ | Press Tab after `/spooky ` to cycle sub-commands |
| `/spooky goal <n>` | Set a candy-score goal for the ETA (`0` turns it off) |
| `/spooky candy` | Print the best-candy-mob ranking to chat |
| `/spooky fishing` | Print the spooky sea-creature table to chat |
| `/spooky reforge` | Blacksmith reforge cinematic — status / `test` / `on` / `off` / `sounds` / `motion` / `speed <n>` |
| `/spooky move` | Drag the HUD around, scroll to scale, Esc to save |
| `/spooky toggle` | Show/hide the HUD |
| `/spooky reset` | Reset the session candy counters + rates |

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

## Tests

The score-tracking engine, number/time formatting and text parsing have **no
Minecraft dependency**, so they're covered by a plain-Java stress test you can
run without Forge:

```bash
./scripts/run-tests.sh
```

It runs ~40 checks including a **500,000-event** simulation (~27 hours of play)
that verifies: candy is only ever added on gains (selling never rolls it back),
the score weighting, session/recent/best rates, the best-rate spike guard, ETA
math, reset behaviour, and that the rate window stays memory-bounded. All green:

```
PASSED: 42   FAILED: 0
```

The reforge cinematic's core logic is decoupled the same way: `ReforgeAnimation`
is pure Java (no Minecraft imports), so the suite also drives it to completion
hundreds of times and asserts the invariant that matters most — **the reveal is
always the real result it was handed, never an intermediate fake** — plus stage
monotonicity, one-shot hit/break/reveal timing, and that degenerate reforge pools
never hang.

`CandyTracker` and `ReforgeAnimation` are deliberately decoupled from Forge
(config values are pushed in, clocks/time are injected) which is what makes this
testable.

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
├── core/                SpookyState (live values), SpookyConfig (settings),
│                        CandyTracker (score / rate / ETA engine)
├── event/               StateUpdater (fills state each tick) + KeyBinds
├── hud/                 SpookyHud (the corner overlay)
├── reforge/             Blacksmith reforge cinematic — ReforgeData (pools),
│                        ReforgeAnimation (Forge-free state machine),
│                        ReforgeGambleRenderer (GL draw), ReforgeGambleHandler (detect)
├── gui/                 MoveHudScreen (drag/scale) + GuideScreen (score + reference)
├── command/             /spooky command
├── data/                CandyData / FishingData reference tables
└── util/                scoreboard, text parsing + Fmt (number/time formatting)
```

Every feature reads from `SpookyState`, which `StateUpdater` refreshes each
tick — so adding a new HUD line or data source stays a one-file change.
