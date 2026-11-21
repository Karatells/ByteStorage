# Simple Byte Storage 1.3.1

**Minecraft 1.20.1 · Forge 47.2.0+ · Java 17.** Bugfix and polish release for both the main mod and the optional Scripting addon, verified in-game and with automated tests.

## Downloads

| File | What it is |
|---|---|
| `simple_bytestorage-1.3.1.jar` | The mod (**required**). |
| `simple_bytestorage_scripting-1.3.1.jar` | Optional addon: Logic Bus + Logic Terminal (JavaScript). Requires the main mod. |

Install on **both client and server**.

## What's fixed in 1.3.1

### Main mod — Functional fixes
- **Drive & I/O Bus item drops**: breaking a Drive now flushes cached cell data to NBT before dropping cells and also drops the installed upgrade card (Void Card / Lock Card); breaking an I/O Bus now drops all installed Speed Cards.
- **Security Terminal enforcement**: locked Security Terminals can no longer be broken by non-owners; access checks (`canAccess`) are now enforced on Drives, Crafting Terminals, Pattern Terminals, Pattern Encoding Terminals, Network Controllers, and Wireless Terminals.
- **Fluid I/O Bus & Drive energy**: Fluid I/O Bus import now respects locked Drives (`LockCard`); Fluid Cells are now counted in Drive idle energy drain.
- **Instant network topology updates**: placing or breaking adjacent network blocks immediately marks the Controller topology dirty without waiting for the periodic rescan.
- **Worldgen placement**: `quantum_cluster` and `circuit_bloom` surface features now require a solid non-leaf block underneath (preventing floating spawns over water or tree canopies).

### Main mod — GUI fixes
- **Window titles**: long titles now clip cleanly with `…` before the status LED.
- **Terminal & Crafting Terminal**: memory usage switches to compact formatting (`15.0k/1.31M B (1%)`) and clips cleanly so it never overlaps the `Inventory` label.
- **Controller**: widened Stock tab `-100` / `+100` buttons so labels are no longer truncated; tightened Crafting tab header spacing.
- **Wireless Terminal Settings**: compact single-line upgrade list when 3–4 cards are installed so text never overlaps player inventory slots.
- **I/O Bus & Fluid I/O Bus**: adjusted button widths so `Mode: Import`, `Allow`/`Deny`, and `RS: Always` fit cleanly without clipping.
- **Archiver, Network Display, Pattern Terminal, Drive & Guide Book**: added proper text clipping and widened the Guide Book chapter list.

### Scripting addon (`simple_bytestorage_scripting`)
- **Top-level `side(...)` endpoints with `move()`**: fixed `endpoints` being cleared on every tick, which broke `a.move(b)` when endpoints were stored in top-level variables.
- **Redstone output cleanup**: stopping a script, hitting a runtime error, or losing grid power now resets `redstone.set()` outputs back to `0` and notifies neighboring blocks.
- **Syntax highlighting & autocomplete**: added `1.3.0` APIs (`store.*`, `redstone.*`, `display.*`) to `ApiCatalog`.
- **Security Terminal support**: `LogicTerminal` and `LogicBus` editing now respects `SecurityTerminal` permissions.
- **Logic Terminal GUI**: fixed horizontal centering of the tick interval label between the `-` and `+` buttons.
