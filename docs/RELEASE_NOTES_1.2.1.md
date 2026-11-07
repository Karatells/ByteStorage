# Simple Byte Storage 1.2.1 (beta)

**Minecraft 1.20.1 · Forge 47.2.0+ · Java 17.** This is a **beta**: the new features were verified by automated tests and builds but have had limited in-game testing. Please report problems.

## Downloads

| File | What it is |
|---|---|
| `simple_bytestorage-1.2.1.jar` | The mod (**required**). |
| `simple_bytestorage_scripting-1.2.1.jar` | Optional addon: Logic Bus + Logic Terminal (JavaScript). Requires the main mod. |

Install on **both client and server**. The network protocol changed (now 10) — client and server must run the same version.

## What's new since 1.2.0

### Main mod
- **Controller redesigned**: tabs with icons in a left column, a header with a network icon and a close cross, an Overview with a status/energy panel and a network-contents panel (items stored, charge, estimated time left). The Wireless Terminal and Recipe Library slots no longer overlap other controls.
- **All other screens restyled** in the same style: icons in every window header, a status LED, a clickable close cross (ESC still works), icons on section labels and on some buttons.
- **Statistics graphs** (new Graphs tab): items stored and energy over the last 20 minutes, plus the items that rose and fell the most. History is not saved across reloads.
- **Stock rule schedule**: rules can place orders always, at night only, by day only, or only while network energy is high.
- **Capacitor** block: a 500,000 FE reserve that refills the Controller below 50%, absorbs surplus above 90% and accepts energy from adjacent generators.
- **"Max" button** in the order dialog: when ingredients are short, one click orders the largest amount that can be crafted now.
- **Terminal tooltips** show stored / reserved / in-production amounts for the hovered item.
- **Reservations survive world reloads.**
- **Stuck Pattern Buses** are detected and reported like stuck Assemblers.
- **Saved searches**: three slots under the terminals' sort buttons (stored in `config/simple_bytestorage_searches.txt`).

### Scripting addon
- **Code editor** in the Logic Terminal: line numbers, syntax highlighting, undo/redo, auto-indent, auto-closing brackets, Tab indent, Ctrl+/ comments, and **autocomplete** for API members and item ids (Ctrl+Space to force).
- **Events**: optional `onCraft`, `onLowStock`, `onAlert` handlers instead of polling in `tick()`.
- `net.machines()` lists Assemblers and Pattern Buses with their state; `stock.mode()` / `stock.setMode()` control the schedule.

## Known limitations
- Statistics history and saved searches are not stored on the server (history resets on reload; searches are per client).
- The new GUI visuals have not been checked on every GUI scale.
