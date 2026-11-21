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
| 256k | 262,144 | Cyan |
| 1M | 1,048,576 | Red |

**1 item = 1 byte**, regardless of the item type or stack size. A stack of 64 cobblestone costs 64 bytes, and so does 64 different single items.

Cell upgrades are crafted by combining the lower tier with extra materials — see [Recipes](Recipes#storage-cells).

## Cells as Assembler buffers

Any storage cell can also be used as the buffer of an [Assembler](Assembler).

## Cell Workbench & Drive Upgrade Cards
- **Cell Workbench (`cell_workbench`)**:
  - **Per-cell filter**: insert a storage cell in the left slot and configure up to 9 filter slots in **Allow** (whitelist) or **Deny** (blacklist) mode. The filter is saved directly on the cell's NBT.
  - **Cell Migration (`->` button)**: insert a source cell in the left slot and a target cell in the right slot, then click **`->`** to transfer all compatible items (between `ByteCellItem`) or fluids (between `FluidCellItem`) respecting filters and remaining capacity.
  - **Corrupted Cell Restoration**: place a **Corrupted Byte Cell (`corrupted_cell`)** found in `Silicon Rift` (`quantum_cluster`, 15% drop chance without Silk Touch) into the left slot with an empty right slot and click **`->`** to restore it into a valuable reward (`4k`/`16k` cell, `Fluid Cell 4k`, `Energy Cell 1k`, `Portable Cell 1k`, or upgrade cards).
- **Void Overflow Card (`void_card`)**: insert into a Drive's upgrade slot; excess items matching the Drive's filter are destroyed when the Drive is full.
- **Lock Card (`lock_card`)**: insert into a Drive's upgrade slot to make the Drive read-only (allows extraction, blocks new item and fluid insertions).

## Portable Byte Cells (`portable_cell_1k` / `4k` / `16k`)
Handheld storage cells that bring the **no-type-limit** byte system on the go:
- **Right-click** in hand to open a full **Terminal GUI** (search, sort, Shift-click, deposit) backed by that cell.
- **Shift + Right-click** in air to toggle **Auto-Pickup**: picked-up ground loot goes straight into the Portable Cell (respecting any filter set in the Cell Workbench).
- Fully compatible with **Drive** and **Cell Workbench** — drop your Portable Cell into a Drive at home or migrate it in a Cell Workbench.
- Protected against recursion (a Portable Byte Cell cannot be inserted inside itself).

## Energy Byte Cells (`energy_cell_1k` / `4k` / `16k`)
Store Forge Energy directly inside any **Drive** slot at **1 byte = 1,000 FE**:

| Energy Cell | Capacity |
|---|---|
| `energy_cell_1k` | 1,024,000 FE |
| `energy_cell_4k` | 4,096,000 FE |
| `energy_cell_16k` | 16,384,000 FE |

Connected Energy Cells automatically balance energy with the **Network Controller** (same thresholds as Capacitor: refilling the Controller below 50% and absorbing surplus above 90%).

## Storage Bus
The **Storage Bus (`storage_bus`)** connects adjacent external inventories (chests, barrels, drawers) directly to the virtual network pool without importing their contents into cells. Supports priority (`-999..+999`) and a 9-slot Allow/Deny filter.

## Byte Archiver & Data Blobs
The **Byte Archiver (`archiver`)** automatically compresses large quantities (`>= 4096`) of stackable, non-NBT items in 64:1 **Data Blobs (`data_blob`)** (costing 160 FE per compression), saving storage bytes. Right-click a Data Blob in your hand to unpack 64 items.

## Fluid Storage
- **Fluid Byte Cells (`fluid_cell_1k` – `fluid_cell_64k`)**: store fluids at **1,000 mB = 1 byte** with no limit on fluid types, inside any Drive.
- **Fluid I/O Bus (`fluid_io_bus`)**: imports or exports fluids (1,000 mB/cycle) between adjacent fluid tanks and Fluid Byte Cells in the network.
