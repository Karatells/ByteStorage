# Simple Byte Storage 1.4.0

**Minecraft 1.20.1 · Forge 47.2.0+ · Java 17.** Major feature update for the main mod, the optional **Scripting** addon, and the brand-new optional **Silicon Plane** Dimension addon (`simple_bytestorage_dimension`). Network protocol is now **12** (main mod) and **4** (Scripting addon) — client and server must run the same version.

## Main mod — New blocks, items & features
- **Network Interface (`network_interface`)**: bidirectional bridge for hoppers, pipes, and machines from other mods — inserted items go straight into the network, while 9 ghost config slots automatically keep 9 buffer slots stocked from storage.
- **Portable Byte Cells (`portable_cell_1k` / `4k` / `16k`)**: handheld no-type-limit storage cells; Right-click opens a full Terminal GUI, `Shift + Right-click` toggles **Auto-Pickup** of ground loot, compatible with `Drive` and `Cell Workbench`.
- **Energy Byte Cells (`energy_cell_1k` / `4k` / `16k`)**: store Forge Energy inside any `Drive` at **1 byte = 1,000 FE** (up to 16.38M FE), automatically balancing with the Controller like a Capacitor.
- **Corrupted Byte Cell (`corrupted_cell`)**: drops (15% chance without Silk Touch) from `quantum_cluster` in the `Silicon Rift` biome; restore it in the **Cell Workbench** (`->` button) into cells or upgrade cards.
- **Cell Workbench Migration**: added a second target cell slot and **`->`** button to migrate items between `ByteCellItem` or fluids between `FluidCellItem`.
- **Cable Facades (`cable_facade`)**: disguise any Network Cable as any solid building block (`Shift + Right-click` block to copy, `Right-click` cable to apply, `Shift + Right-click` empty hand to remove).
- **3D In-World `Network Display` Renderer**: renders up to 6 glowing lines directly on the block face with color codes (`&a..&f`) and progress bars (`[bar:0..100:color]`).
- **Terminal Fluids Tab (`Items / Fluids`)**: view all stored fluids from `FluidCellItem` and fill/drain buckets by clicking directly in the Terminal.
- **Smart Terminal Search**: added `%tooltip` search, `>=` / `<=` comparisons, `k` / `m` suffixes (`>=10k`, `<=2m`), and item registry path matching.
- **Redesigned Guide Book**: 3D icons, chapter search, and live crafting/smelting recipe cards pulled straight from `RecipeManager`.

## Scripting addon (`simple_bytestorage_scripting`)
- **Online Script Library (`Library` tab)**: browse moderated community scripts from `https://github.com/Karatells/sbs-scripts` with **categories**, **in-game item/block icons**, **mod version compatibility checks**, and **SHA-256 verification** (never auto-runs). Controlled by server config `allowOnlineLibrary` and `onlineLibraryUrl`.
- **Script Disk (`script_disk`)**: `Shift + Right-click` a Logic Bus to copy its script and interval, `Right-click` another Logic Bus to paste.
- **Inter-Bus Messaging**: `bus.send(targetId, text)` and `onMessage(fromId, text)` across the entire network (including Quantum Relays).
- **Fluid API**: `net.fluidCount(id)` and `net.fluids()` for automating Fluid Byte Cells.
- **Built-in Script Templates**: `Tpl (1/5)` button in the Logic Terminal with 5 ready-to-use scripts.

## Dimension addon (`simple_bytestorage_dimension` — new optional addon)
- **The Silicon Plane (`silicon_plane`)**: optional 3-layer dimension (`Y = -64..256`) with custom aurora/matrix-grid/stars skybox (no vanilla clouds), 8 unique biomes, 7 ores, animated **Liquid Coolant**, 3 cyber trees, 4 glowing plants, 6 tech structures with loot chests, and **Magnetic Updraft** elevators.
- **Rift Gate & `Silicon Rift` Requirement**: requires `100,000 FE` and only ignites inside `simple_bytestorage:silicon_rift` in the Overworld (with a hidden advancement **No Signal Here** if tried elsewhere).
- **4 Custom 3D Mobs**: `Byte Sentinel`, `Glitch Stalker`, `Cryo Harvester`, and the `Registry Warden` mini-boss (with boss bar and guaranteed `Matrix Crystal` + `Quantum Core` drops).
- **Endgame Network Machines & Upgrades**:
  - `Advanced Assembler` (×2 speed, min 2 ticks) & `Singular Assembler` (×4 speed, min 1 tick)
  - `Pattern Bus Mk2` (4-tick cycle, up to 64 pushes per cycle)
  - `Singular Controller` (10M FE buffer, +250 FE/t passive trickle, +4 coprocessor power)
  - `Interdimensional Energy Transmitter` (1M FE buffer, 16 channels, cross-dimension FE transfer up to 20k FE/t)
  - `Quantum Relay` (0 FE/t idle drain, +2 coprocessor power)
  - `Speed Card Mk2` (×3 speed power in Assemblers), `Interdimensional Wireless Card` (cross-dimension access even in unloaded chunks), and `Facade Scanner` (reusable tool that paints up to 32 connected cables).
- **High-Tier Gear & Storage**: `256k` / `1M` Byte Cells, `64k` Portable Cell, `64k` / `256k` Energy Cells, `256k` Fluid Cell, `Return Beacon`, `Phase Stabilizer`, `Capacitor Accumulator` (captures lightning in Capacitor Flats), `Busium` / `Cryo` Pickaxes, `Archive Lore Fragments`, and 6 compact resource blocks (`9 ↔ 1`).
- **Complete Optic Wood Set**: 22 decorative and functional blocks (logs, stripped logs, planks, stairs, slabs, fence, gate, door, trapdoor, pressure plate, button, mosaic, bookshelf, chiseled bookshelf, ladder, 27-slot crate, signs, hanging signs, sapling) plus `Optic Boat` and `Optic Chest Boat`. See [Dimension-Addon.md](wiki/Dimension-Addon.md).
