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
- Breaking an I/O Bus drops all installed **Speed Cards**.
- For logic that goes beyond filters (thresholds, conditions, auto-crafting), see the [Scripting addon](Scripting-Overview).

---

## Network Interface (`network_interface`)

A bidirectional automation bridge designed for hoppers, pipes, chutes, conveyors, and machines from other mods.

| Property | Value |
|---|---|
| Idle energy | 2 FE/t |
| Config slots | 9 (ghost Keep-in-Stock slots) |
| Buffer slots | 9 (real item slots) |
| Sync cycle | every 10 ticks |

### How it works
- **Push-In (Input):** Any item inserted into the Network Interface by hoppers, pipes, or machines via `IItemHandler` is routed **directly into the network's storage** (respecting network online state and Eco Mode).
- **Keep-in-Stock (Output Buffer):** Place ghost items with desired stack counts (1–64) in the top 9 **Keep in Stock** slots. The Network Interface automatically pulls items from the network into the matching bottom 9 **Buffer** slots (and returns excess or unconfigured items back to the network). External pipes/hoppers can extract directly from the Buffer slots.
- Respects **Security Terminal** permissions and **Eco Mode**; breaking the block drops any items currently in the Buffer.
