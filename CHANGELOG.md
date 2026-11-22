# Changelog

## Unreleased — playtest pass

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

## 0.1.0 — first playable

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
