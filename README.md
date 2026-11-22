# Create: Village Transit / 机械动力：通勤运输

<img src="logo.png" alt="Create: Village Transit" width="160" align="right">

> **Release 0.1.0** · Minecraft 1.21.1 · NeoForge 21.1 · Create 6.0.10 · MIT

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)]()
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1-orange)]()
[![Create](https://img.shields.io/badge/Create-6.0.10-blue)]()
[![License](https://img.shields.io/badge/License-MIT-yellow)]()

A **Create** add-on that turns villages into passenger stations. Villages attract travellers,
platform lamps link a village to a train station, passenger seats carry travellers on trains,
and every arrival pays the train owner for the people who got off.

> 一个 Create 附属模组：把村庄变成客运站点。村庄会自然产生旅客，站台路灯把村庄与列车站点绑定，
> 旅客座垫把旅客装上车，列车到站时按目的、耐心与距离向车主结算报酬。

---

## Features / 玩法

| Block / Item | 中文 | Purpose |
|---|---|---|
| Village Station | 村庄站点 | Census of the surrounding village, traveller list, manual refresh |
| Platform Lamp | 站台路灯 | Binds one village station to one train station (two filter-like slots) |
| Passenger Seat ×16 | 旅客座垫 | Works like a Create seat in the world; holds one traveller on a train |
| Trade Station | 交易站 | Shows the owner's balance; withdraw / deposit coins, hopper & pipe friendly |
| Passenger | 旅客 | Non-stackable item carrying purpose, patience and origin |
| Coins ×7 | 硬币 | Copper … Netherite, face values `10 … 10^7` |

**Travellers** have a purpose (Travel / Relocate / Trade) and patience (1–8, plus an ultra-patient
variant that never expires while waiting). Every list update decrements patience; patience 0 means
the traveller gives up and leaves. Boarding happens in arrival order; on board, patience is
decremented at every station and the traveller leaves when patience runs out or when the arriving
village matches the preference of their purpose.

**Rewards** scale with purpose base value, remaining patience and travel distance (with a dead zone
and soft cap), are reduced for repeated runs on the same village pair, and are paid into the train
owner's wallet — even while the owner is offline. Crashes, derailments and dismantling charge the
owner based on where the accident happened.

Everything — census interval, travel chance, purpose weights, patience weights, reward bases,
distance curve, anti-farm limits, link distance, auto-withdraw — is configurable. See
[DESIGN.md](DESIGN.md) for the full specification.

---

## Requirements

- Minecraft **1.21.1**
- NeoForge **21.1.x**
- Create **6.0.10** (compatible with `6.0.x`)

## Installation

Drop the jar into `mods/` next to Create. No other dependencies.

## Building

```bash
./gradlew build
# jar: build/libs/create_village_transit-<version>.jar
```

Requires JDK 21. The Gradle wrapper is included.

> On networks where `maven.neoforged.net` needs a proxy, set `systemProp.https.proxyHost` /
> `systemProp.https.proxyPort` in `gradle.properties`.

### Dev commands

```bash
./gradlew runServer        # dedicated dev server (run/server)
./gradlew runClientQuick   # client that joins 127.0.0.1:25565 directly
./gradlew test             # pure maths unit tests (13 cases)
```

With RCON enabled on the dev server, `tools/rcon.ps1 -Command "cvtransit …"` drives the in-game
self checks:

| Command | Checks |
|---|---|
| `selftest` / `status` | places a station with beds + villagers, prints every station's census |
| `simulate` | arrival settlement (ageing, purpose rules, payout) |
| `board` | seat filling order, player seats skipped |
| `accident` | crash penalty, debt allowed |
| `econ` | withdraw-before-mint, deposit, debt blocks withdrawal |
| `lamp` | six binding branches (success / duplicate / type / range / slot / unloaded) |
| `chunks` / `perf` / `packet` | loaded chunks, scheduler cost of the last tick, and wire size of a station snapshot |
| `load <n> [travellers]` | dev only: creates synthetic stations for load / save-size measurements |
| `trains` / `links` | live view of Create trains and of every lamp's wiring, for testing with a real train |
| `persist write` / `persist check` | survival of stations, manifests and wallets across a restart |

Manual checklist for things that need a human (a real train, mouse clicks, textures): [TESTPLAN.md](TESTPLAN.md).

### How it is verified

| Layer | Evidence |
|---|---|
| Build | `./gradlew build` green, jar attached to every CI run |
| Payout maths | 13 JUnit cases (dead zone, saturation, patience, caps, degenerate settings) |
| Station loop | in-game: census → `ACTIVE`, villagers killed → `INVALID`, travellers keep growing |
| Settlement / boarding / crash | driven through `cvtransit simulate / board / accident` against real code |
| Money invariants | `cvtransit econ`: withdraw-before-mint, deposit, debt blocks withdrawal |
| Lamp branches | `cvtransit lamp`: success + 5 rejection reasons |
| Persistence | write → flush → restart → `cvtransit persist check` returns identical values |
| No stray chunk loads | `cvtransit chunks` stays flat while a far away station exists |
| GUI | auto-cycled screens + window capture (`-Dcvtransit.guiDemo`) |

## Status (0.1.0)

Implemented: village census with throttling and full-chunk guard, traveller generation and ageing,
station GUI with manual survey, platform lamp binding, 16 passenger seats (Create seat tag),
train arrival/crash/dismantle tracking with boarding, alighting, payouts and penalties, wallets,
seven coin denominations and the trade station (deposit / withdraw / pipe access).

`./gradlew test` runs the pure payout maths (13 cases). The add-on has exactly **one** mixin, used
only to refuse players a seat that already carries a traveller; everything else is public API.
Remaining gaps are listed in [DESIGN.md](DESIGN.md) §13.2.

Contributions are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md). Release notes live in
[CHANGELOG.md](CHANGELOG.md).

## Design notes / 设计要点

- **No mixins.** Only Create's public API is used; trains are observed by polling
  `Create.RAILWAYS.trains`, so the add-on keeps working across minor Create updates.
- **Runs while unloaded.** Station state lives in a `SavedData` (like Create's package ports keep
  an offline buffer), so travellers keep being generated and counted while the chunk is unloaded.
- **Cheap census.** Village scans are throttled, staggered (at most one station per tick) and
  refuse to run on partially loaded chunks, so village size is never under-reported.
- **One village, one station.** A village can only be claimed once, with a configurable minimum
  distance between stations.

## Contributing

Issues and pull requests are welcome — especially **art** (see the asset list in `DESIGN.md`).
The prototype textures are generated placeholders and are meant to be replaced by hand-drawn ones;
any texture can be swapped by dropping a same-named PNG into
`src/main/resources/assets/create_village_transit/textures/`.

## License

MIT — see [LICENSE](LICENSE).
