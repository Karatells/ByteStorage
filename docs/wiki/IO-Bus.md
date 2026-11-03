# I/O Bus

Moves items between a **neighbouring inventory** and the network automatically.

| Property | Value |
|---|---|
| Cycle | every 10 ticks |
| Items per cycle | 64 |
| Filter slots | 9 (ghost slots) |

## Modes (toggle with the button)

### Import
Pulls items from the neighbouring inventory into the network. With the filter **empty, everything is imported**; otherwise only filtered items.

### Export
Keeps the listed items stocked in the neighbouring inventory. The **count in a filter slot is the amount to keep**. The bus tops up from the network when the inventory has less than that.

## Notes
- The bus works on **every adjacent inventory** (not other network devices).
- The status line shows whether a machine/inventory is connected and the network is online.
- For logic that goes beyond filters (thresholds, conditions, auto-crafting), see the [Scripting addon](Scripting-Overview).
