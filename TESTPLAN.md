# In-game test plan / 真机验收清单

Anything that cannot be checked headless (a real train, mouse clicks, textures) is listed here.
Each item states how to set it up, what to type in chat, and what you should see.

## 0. Setup

1. Create 6.0.10 + NeoForge 21.1.x, this jar in `mods/`.
2. Creative mode, cheats on.
3. Optional helper commands (op):

| Command | What it does |
|---|---|
| `/cvtransit status` | every village station: state, villagers, employed, beds, workstations, traveller count |
| `/cvtransit links` | every lamp: linked village / train station, station id, `lit` |
| `/cvtransit trains` | every Create train: short id, current station, derailed flag, carriages, onboard travellers |
| `/cvtransit chunks` | loaded chunk count (should not jump when stations are far away) |

---

## A. Village station end to end

1. Find a village, place a **Village Station** inside it.
2. Wait ~5 s, run `/cvtransit status`.
   - **Expected**: `state=ACTIVE`, `villagers>0`, `beds>=3`, traveller count grows over a few minutes.
3. Breaking all beds / killing all villagers and running `/cvtransit refresh`:
   - **Expected**: `state=INVALID`.
4. Open the station GUI:
   - **Expected**: status line, villagers/employed, work/beds, travellers, paid-this-hour, next survey countdown, traveller list with icons.
   - Hover a traveller row → tooltip with purpose / patience / origin.
   - Click **Survey now** → button greys out until the survey finishes, then the countdown resets.

## B. Platform lamp binding

1. Put a lamp within 30 blocks of both the village station and a Create **Train Station**.
2. Right-click the lamp → click the **Village Station** slot → right-click the village station block.
   - **Expected**: slot shows the block icon + coordinates; message on success.
3. Repeat for the **Train Station** slot with the Create station block.
   - **Expected**: `Lamp ready` in green, lamp lights up (block light 15), `/cvtransit links` shows `lit=true`.
4. Try binding a stone block / a station 40 blocks away / an unloaded chunk.
   - **Expected**: refused with the matching message.

## C. A real train (the important one)

1. Build a line with a Train Station at both ends, assemble a train with at least two **Passenger Seats**
   (any colour) and a **Train Controls** block.
2. Put a village station + lamp at one end (A) and optionally at the other (B).
3. Wait until station A has several travellers (`/cvtransit status`).
4. Give the train a schedule A → B, let it arrive at A.
   - **Expected**:
     - `/cvtransit trains` shows `station=<A id>` and `onboard` equal to the number of passenger seats that could be filled.
     - `/cvtransit status` travellers drop by the number boarded.
     - The station panel's `Paid/h` counter rises after people get off.
5. Try to sit on a seat that carries a traveller.
   - **Expected**: message "This seat is taken by a traveller", you do not sit.
6. Sneak + right-click such a seat.
   - **Expected**: read-only panel with the traveller's purpose / patience / origin.

## D. Crash and dismantle penalties

1. Note the owner's wallet (trade station GUI or `/cvtransit status` after a payout).
2. Drive a train carrying travellers into a dead end (or dismantle it at a station).
   - **Expected**: `/cvtransit trains` no longer lists it; the owner's balance drops by the accident
     settlement; the balance may go negative (debt).
3. With a negative balance, open a **Trade Station** as the owner and press **Withdraw all**.
   - **Expected**: nothing is minted; inserting coins immediately reduces the debt.

## E. Trade station

1. Place a trade station (owner = placer).
2. `/give @s create_village_transit:gold_coin 5` and put them in the slots.
   - **Expected**: coins are consumed, the balance line rises by 5 × 1000 cents.
3. Press **Withdraw all** with a positive balance.
   - **Expected**: slots fill largest-first; balance drops by exactly the minted value.
4. Put a hopper under the station and a chest above it.
   - **Expected**: extraction only ever yields coins that were already paid for; insertion credits the wallet.

## F. Textures / models

1. Place all four blocks side by side in daylight and at night.
   - **Expected**: village station shows an emerald core (brighter when active), the lamp is a two-block
     tall post with a warm lantern on top and lights the area, the trade station looks like a counter,
     the seats match Create seat proportions.
2. If something looks off, replace the PNG with the same name under
   `src/main/resources/assets/create_village_transit/textures/` and rebuild.

---

## What to report back

For each failed item: the command you ran, the chat output, and (if visual) a screenshot.
`run/logs/latest.log` is enough for anything that throws.
