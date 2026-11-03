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
| Pattern Provider | 2 |
| I/O Bus | 2 |
| Drive | 2 + per-cell cost (1k: 1, 4k: 2, 16k: 3, 64k: 4) |
| Cable | 0 |
| Logic Bus *(addon)* | 4 while running, 1 otherwise |
| Logic Terminal *(addon)* | 2 |

## Cable

Cables carry the network over distance and cost no energy. Crafting 1 recipe gives 4 cables. Devices that touch each other connect without a cable.

## Re-scan timing

The network re-scans every 20 ticks. After placing or breaking a block, give it up to a second to join or leave the network.
