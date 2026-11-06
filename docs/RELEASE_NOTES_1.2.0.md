# Simple Byte Storage 1.2.0

**Minecraft 1.20.1 · Forge 47.2.0+ · Java 17.**

## Downloads

| File | What it is |
|---|---|
| `simple_bytestorage-1.2.0.jar` | The mod (**required**). |
| `simple_bytestorage_scripting-1.2.0.jar` | Optional addon: Logic Bus + Logic Terminal (JavaScript). Requires the main mod. |

Install on **both client and server**. The network protocol changed (now 9) — client and server must run the same version.

## What's new since 1.1.3

### Main mod
- **Controller — Journal tab**: the latest autocrafting events (orders, finished crafts, Pattern Bus starts, stock-rule orders and failures, cancellations, stuck machines) with timestamps and paging. Identical events are merged. The controller window is wider (288 px); the Russian "Хранилище" tab is now "Склад".
- **Resource reservation**: raw materials taken by an accepted order are reserved. Terminals, I/O buses and the Wireless Terminal cannot take them; new orders are planned only from the free remainder; Assemblers and Pattern Buses consume from the reservation. Cancelling releases it. Reservations are not saved across world restarts.
- **Machine power from the network**: the Pattern Bus now charges adjacent machines that have an energy buffer (electric furnace, etc.) from network energy. The new **Energy Interface** block (cable + redstone + gold ingot) connects by cable and charges adjacent machines. Up to 1000 FE/t per device; the network keeps a reserve so machines cannot black it out.
- **Shortage warnings**: stock-rule failure reasons (no pattern, missing ingredient with amount, no machine, queues full) show in a tooltip over the red `!`; after 3 failures in a row a chat message is sent. An Assembler waiting for ingredients or storage space for more than 30 s is logged as "Stuck" and reported. Players carrying a Wireless Terminal bound to the controller are notified, at most once per 5 minutes per reason.
- **Fix**: the Recipes tab no longer draws an extra column of slot frames.
- **API**: stable `com.simpleae.bytestorage.api.NetworkControl` facade for addons.

### Scripting addon
- **Network info**: `net.available`, `net.reserved`, `net.totalReserved`, `net.energy()`, `net.stats()`.
- **Planning** (orders nothing): `net.canCraft`, `net.why` (reason code plus missing item and count), `net.plan`.
- **Order queue**: `net.queue()`, `net.setPriority`, `net.pause`, `net.resume`, `net.cancel`.
- **Stock rules**: `stock.list`, `stock.set`, `stock.clear`.
- **Recipe settings**: `recipes.block/unblock/prefer/unprefer`, `recipes.skipUnpacking`.
- **Journal**: `journal.write`, `journal.warn`, `journal.read` — script entries appear in the Controller's Journal tab.
- **Alerts**: `alert(text)` sends a chat warning to owners of bound Wireless Terminals.
- New server config options `allowControl` and `allowAlerts`; new per-run limits (4 journal writes, 1 alert).
- Wiki, in-game guide (en/ru/uk) and examples 17–23 updated.

## Notes and limitations

- Reservations cover only raw materials taken from storage and are not persisted.
- Stuck detection covers Assemblers only.
- Pattern Bus learning mode is heuristic: one output item per pattern; put fuel/energy into the machine before recording.
- Order time estimates are unavailable for Pattern Bus orders.
- Wireless alerts scan only the main inventory.
- Priority/pause apply when a machine picks its next craft: a craft that is already running finishes first.
