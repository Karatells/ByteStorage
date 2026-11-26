# Network and Power

## Network Controller

The controller coordinates the network and supplies its power.

| Property | Value |
|---|---|
| Energy buffer | 100,000 FE |
| Max receive | 2,000 FE/t |
| Own consumption | 10 FE/t |

### Grid states

| State | Meaning |
|---|---|
| `ONLINE` | Network active, power stable. The controller face shows a bright cyan core. |
| `OFFLINE` | Not enough power. Devices stop working. |
| `CONFLICT` | More than one controller in the same connected network. Remove one. |

## Idle energy consumption

Every device draws a small amount of FE per tick from the network.

| Device | FE/t |
|---|---|
| Network Controller | 10 |
| Terminal | 2 |
| Crafting Terminal | 3 |
| Pattern Terminal | 1 |
| Pattern Encoding Terminal | 2 |
| Assembler | 3 |
| Pattern Bus | 2 |
| I/O Bus | 2 |
| Drive | 2 + per-cell cost (1k: 1, 4k: 2, 16k: 3, 64k: 4) |
| Cable | 0 |
| Logic Bus *(addon)* | 4 while running, 1 otherwise |
| Logic Terminal *(addon)* | 2 |

## Cable & Cable Facades

Cables carry the network over distance and cost no energy. Crafting 1 recipe gives 4 cables. Devices that touch each other connect without a cable.

- **Cable Facade (`cable_facade`)**: disguise any Network Cable as a full solid building block:
  1. **Shift + Right-click** any solid block with a Cable Facade to copy its appearance.
  2. **Right-click** a Network Cable to apply the facade (renders the copied block directly over the cable).
  3. **Shift + Right-click** a disguised cable with an empty hand (or break the cable) to recover the facade.

## Topology Updates

Placing or breaking a network block immediately triggers a topology refresh on adjacent Controllers (with a periodic fallback scan as a safeguard).

## Network Display (3D In-World Monitor)
The **Network Display (`network_display`)** renders up to 6 lines of glowing text and progress bars **directly on its front face in the world** (no need to open the GUI):
- Supports leading color codes: `&a` (green), `&b` (cyan), `&c` (red), `&d` (purple), `&e` (yellow), `&f` (white), `&6` (orange), `&7` (gray), `&9` (blue).
- Supports inline progress bars: `[bar:75]` or `[bar:75:green]` (0–100%, colors: `green`, `cyan`, `red`, `yellow`, `orange`, `blue`, `purple`, `white`, `gray`).
- Updated automatically by Logic Bus scripts via `display.set(line, text)`.

## Network Interface
See [I/O Bus & Network Interface](IO-Bus#network-interface) — bridges external pipes, hoppers, and machines into the network (2 FE/t idle).

## Energy Interface
A network block (recipe: Cable + Redstone + Gold Ingot, shapeless) with no GUI. Connect it to the network with cables and place it **next to a machine with an energy buffer**; every few ticks it charges the neighbouring machines with energy from the Controller's buffer (up to 1,000 FE/t per interface, only what the machine accepts). Network blocks next to it are not charged. It costs 1 FE/t of idle energy. The Controller always keeps a reserve (about 40 ticks of the network's own consumption, at least 500 FE) so that powering machines cannot turn the network offline. The Pattern Bus charges its own adjacent machines the same way.

## Capacitor
A network block (recipe: 4 Gold Ingots, 4 Redstone Blocks and a Cable, in the shape of a ring around the cable) that stores up to **500,000 FE** as a reserve for the network. Connect it with cables, like any network block. Every few ticks it works with the Controller's buffer:
- when the Controller is **below 50%** full, the Capacitor tops it up to 60% (up to 4,000 FE/t);
- when the Controller is **above 90%** full, the Capacitor takes the surplus down to 85%, so incoming energy is not wasted.

It also accepts energy directly from generators placed next to it (up to 2,000 FE/t) but never gives energy to anything except the Controller. Right-click it to see how much it holds. The stored energy is saved with the block. It costs 1 FE/t of idle energy. Several Capacitors work independently and add up.

## Eco Mode
When the Controller's energy buffer drops **below 10%**, the network enters **Eco Mode**: I/O Buses, Fluid I/O Buses, and Byte Archivers pause automatically while Terminals remain usable so you can still access your items.

## Quantum Relay & Entangled Crystal
Connect remote bases or different dimensions to the same network without cables:
1. Hold **2 or more Entangled Crystals (`entangled_crystal`)** and **Shift + Right-Click** to pair them with a shared Link ID.
2. Place **Quantum Relays (`quantum_relay`)** at each end and insert one paired crystal into each relay's GUI.
3. Linked relays bridge both sides as a single network (50 FE/t idle per linked relay).

## Security Terminal & Network Analyzer
- **Security Terminal (`security_terminal`)**: binds to the player who places it and lets the owner toggle **Locked / Public** access for network terminals.
- **Network Analyzer (`network_analyzer`)**: right-click any network block to inspect its grid state, node count, power drain, energy, and byte capacity.
