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

## Cable

Cables carry the network over distance and cost no energy. Crafting 1 recipe gives 4 cables. Devices that touch each other connect without a cable.

## Re-scan timing

The network re-scans every 20 ticks. After placing or breaking a block, give it up to a second to join or leave the network.

## Energy Interface
A network block (recipe: Cable + Redstone + Gold Ingot, shapeless) with no GUI. Connect it to the network with cables and place it **next to a machine with an energy buffer**; every few ticks it charges the neighbouring machines with energy from the Controller's buffer (up to 1,000 FE/t per interface, only what the machine accepts). Network blocks next to it are not charged. It costs 1 FE/t of idle energy. The Controller always keeps a reserve (about 40 ticks of the network's own consumption, at least 500 FE) so that powering machines cannot turn the network offline. The Pattern Bus charges its own adjacent machines the same way.

## Capacitor
A network block (recipe: 4 Gold Ingots, 4 Redstone Blocks and a Cable, in the shape of a ring around the cable) that stores up to **500,000 FE** as a reserve for the network. Connect it with cables, like any network block. Every few ticks it works with the Controller's buffer:
- when the Controller is **below 50%** full, the Capacitor tops it up to 60% (up to 4,000 FE/t);
- when the Controller is **above 90%** full, the Capacitor takes the surplus down to 85%, so incoming energy is not wasted.

It also accepts energy directly from generators placed next to it (up to 2,000 FE/t) but never gives energy to anything except the Controller. Right-click it to see how much it holds. The stored energy is saved with the block. It costs 1 FE/t of idle energy. Several Capacitors work independently and add up.
