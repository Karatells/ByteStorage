# Simple Byte Storage 1.1.0

**Minecraft 1.20.1 · Forge 47.2.0+ · Java 17.**

## Downloads

| File | What it is |
|---|---|
| `simple_bytestorage-1.1.0.jar` | The mod (**required**). |
| `simple_bytestorage_scripting-1.1.0.jar` | Optional addon: Logic Bus + Logic Terminal (JavaScript). Requires the main mod. |

Install on **both client and server**. The network protocol changed — client and server must run the same version.

## What's new

- **Network Controller GUI** — right-click the controller: Overview (status, energy, connected blocks), Storage (disk load, item types, top items) and Crafting (patterns, busy Assemblers, Pattern Bus jobs, craft queue) tabs.
- **Wireless Terminal binding via the controller** — put the terminal into the controller's slot to bind and charge it (replaces Shift + right-click on a Terminal).
- **Wireless Terminal upgrades** — Shift + right-click opens settings. The new **Auto-Transfer Card** refills the stack in your hand from the network and sends picked-up items to the network; 9-slot filter with whitelist/blacklist; 2 FE per item.
- **Order crafts from any terminal** — in the Terminal and Crafting Terminal, click a dimmed craftable item or Ctrl + click (middle click) an item in stock to open an order dialog.
- **Sorting** in terminals — by name or count, ascending or descending.
- **Pattern Encoding Terminal** — ordinary crafting-table recipes are written straight into the network, no Pattern item needed; Patterns are only for processing crafts.
- Compact item count text in terminals and the Assembler buffer (no more overflow on large stacks).
- Guide Book chapters for the controller GUI and the Wireless Terminal (en / ru / uk).

## Fixes

- Items vanishing when a second stack was put into a Terminal (stack count overflowed the NBT byte).
- Pattern Bus dispatched only 16 crafts of a large order; now up to 256 in flight.
- "Pattern Provider" renamed to "Pattern Bus" in the guide and docs.

## Scripting addon

- Logic Bus identifiers are assigned in the bus's own GUI; the Logic Terminal lists them.

Full documentation: project wiki / `docs/wiki`. Full history: `CHANGELOG.md`.
