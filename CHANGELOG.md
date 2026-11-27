# Changelog

## 0.1.5 — 2026-11-27

### Fixed

- **Breaking the blocks with a pickaxe dropped nothing**: the mod shipped no loot tables, so all
  nineteen blocks (station, lamp, trade station and the sixteen seats) were destroyed without a drop.
  Loot tables are now generated for every block.
- **Sneak + wrench now puts the block straight into the player's inventory** instead of dropping it
  on the ground (falling back to a drop when the inventory is full).

## 0.1.4 — 2026-11-27

### Added

- `village.keepPreviousCountsWhenTruncated` (default true): a survey that could only see part of the
  area keeps the previous villager and employment counts instead of reporting the smaller partial
  number, so travellers are never lost just because chunks were unloaded.

## 0.1.3 — 2026-11-27

### Fixed

- **Stations went STALE on busy servers and silently stopped paying**: the survey required every
  chunk in the scan radius to be loaded (`village.requireFullyLoadedCensus`, default true). With the
  default 64 block radius that is 9x9 chunks, which is almost never true away from a player, so the
  survey never completed and active stations fell back to STALE after 30 minutes; STALE stations
  neither produced travellers nor settled arrivals. The option now defaults to **false**: a survey
  runs as soon as the station's own chunk is loaded and marks itself truncated instead.
- Stale stations keep producing travellers unless `village.stopGeneratingWhenStale` is enabled
  (that switch finally does what its name says).

## 0.1.2 — 2026-11-26

### Changed

- **Village detection is configurable**: the station no longer relies on the vanilla
  `#minecraft:village` structure tag alone. It matches structures by tag (`village.structureTags`),
  by exact id (`village.structureIds`) or by id fragment (`village.structureNameContains`, default
  `village`), which covers village-replacing mods and datapacks on modpack servers.
- The failure reason stays 
ot_in_village_structure` when no configured structure matches.

## 0.1.1 — 2026-11-26

### Fixed

- **Dedicated servers crashed on startup**: the in-game config screen was registered from the mod
  constructor, which pulled the client-only `Screen` class into the server and aborted mod
  construction (`Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist
  DEDICATED_SERVER`). The registration now lives in a client-only helper class.

### Added

- CI now boots a dedicated server with the built jar on every push, so client classes leaking into
  the common side fail the build instead of failing player servers.

## 0.1.0 — 2026-11-22

### Changed

- **Trade station rework**: the interface now shows the player inventory and hotbar, the button
  switches between deposit and withdraw, and conversion only happens while the block is powered by
  redstone. Deposit converts inserted coins into balance; withdraw mints coins back into the slots.
- **Trade station look**: brass casing body with a waxed copper bulb top that switches between
  unlit and lit textures with the redstone state (texture only, no light level change).
- **Village station look**: brass casing body, emerald indicator on the active variant.
- **Station & lamp models**: explicit particle textures so breaking them no longer shows the
  missing-texture particles.
- **Platform lamp**: vanilla lantern model on top, full-block selection shape, and the two bound
  positions are outlined in the world for five seconds after opening the lamp screen.
- **Passenger seats**: Create’s own seat textures with a thin emerald base ring; model and
  collision match Create’s seat.
- **Traveller list**: scrollable with the mouse wheel, tooltips follow the scroll offset.
- **Patience decay**: fixed aged stacks being thrown away (patience only dropped when someone left);
  decay interval is configurable via `passenger.patienceDecayEveryUpdates`.
- **Payouts**: arrivals within the free distance (256 blocks by default) pay nothing, so returning
  to the origin station no longer earns money.
- **Seat occupancy**: travellers now have a villager sitting with them; the outfit follows the
  purpose (artisan / gentle / unemployed) and the villager type follows the origin biome. These
  villagers are excluded from the village census.
- **Wrench**: villages stations, platform lamps and trade stations can be dismantled by sneak +
  wrench, spilling their contents.
- **Breaking a village station** discards its travellers instead of dropping them as items.
- “Next traveller” became “Next batch of travellers”.

### Initial implementation

### Added

- **Village Station** block: throttled village survey (interval grows with village size), full-chunk
  guard (never counts a half-loaded village), stale detection, manual survey button, one station per
  village and a configurable minimum distance between stations.
- **Travellers**: `passenger` item carrying purpose (travel / relocate / trade), patience (1–8, plus
  ultra patient 10), origin; list updates age travellers and spawn new ones from village population.
- **Platform Lamp** block: links one village station and one Create train station through two
  filter-like slots with in-world block picking; validates type, chunk load, distance and duplicate
  links; lights up only while the village station is active.
- **Passenger Seats** (16 colours): Create seats in the world, traveller carriers on contraptions,
  dyeing stays inside the family; a traveller-occupied seat refuses players and can be inspected
  with sneak + use. A player sitting on a seat stores an internal "player" marker, removed on
  dismount, so boarding and settlement treat the seat as occupied.
- **Train integration**: arrival / crash / dismantle detection through Create's public railway
  manager, boarding in seat order (player seats skipped), alighting by patience and purpose,
  payouts with distance curve, route fatigue and caps.
- **Economy**: wallets stored per player (works offline), seven coin denominations
  (10 … 10,000,000 cents), **Trade Station** with withdraw-before-mint semantics, debt handling and
  hopper/pipe access.
- **Configuration**: 55+ server options covering census, travellers, preferences, rewards,
  anti-farm limits, lamp distance, economy and diagnostics.

### Engineering

- No feature depends on private Create state; a single, guarded mixin implements the seat rule.
- 23 tests: payout maths (dead zone, saturation, patience, caps, boundaries), traveller
  distributions (purpose weights, patience table, expected head count).
- `cvtransit` admin commands for every verification path (`selftest`, `status`, `simulate`, `econ`,
  `lamp`, `board`, `accident`, `markers`, `chunks`, `trains`, `links`, `persist`).
- GitHub Actions build workflow, contribution guide, in-game test plan.

### Known limitations

- The train plumbing (train discovery, seat scanning, boarding on a real train) is exercised by
  synthetic manifests only; no real train was driven during development.
- GUI rendering is verified by screenshots; button clicks are not automated.
- Textures are generated placeholders meant to be replaced.
