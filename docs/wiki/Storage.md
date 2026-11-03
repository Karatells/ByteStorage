# Storage

## Drive

A **Drive** holds up to **10 storage cells**. The contents of all cells in all drives are merged into one virtual item pool for the whole network.

### Drive settings

- **Priority** — when inserting, higher-priority drives are filled first. Hold Shift while changing it to step by ±10.
- **Whitelist** (9 filter slots) — only items in the list are accepted. **Empty = accepts everything.**

Combine priority and whitelist to sort items into dedicated drives (for example, a whitelisted drive for ores with a high priority).

## Storage Cells

| Cell | Capacity (bytes = items) | Crystal colour |
|---|---|---|
| 1k | 1,024 | Green |
| 4k | 4,096 | Blue |
| 16k | 16,384 | Purple |
| 64k | 65,536 | Orange |

**1 item = 1 byte**, regardless of the item type or stack size. A stack of 64 cobblestone costs 64 bytes, and so does 64 different single items.

Cell upgrades are crafted by combining the lower tier with extra materials — see [Recipes](Recipes#storage-cells).

## Cells as Assembler buffers

Any storage cell can also be used as the buffer of an [Assembler](Assembler).
