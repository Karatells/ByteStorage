# Pattern Bus

Runs **processing** patterns: crafts that happen in a machine such as a furnace, smoker, or any mod machine that exposes an item handler.

## Setup

1. Place the Pattern Bus **touching the machine** and connect it to the network.
2. Put encoded **processing** patterns (made in the [Encoding Terminal](Autocrafting#pattern-encoding-terminal)) into its **9 slots**.
3. Order the craft from the [Pattern Terminal](Autocrafting#pattern-terminal).

## What it does

For each craft the provider takes the inputs from the network, pushes them into the adjacent machine, and later extracts the expected outputs from **any side** of the machine and sends them to the network.

### Powering the machine
The bus also **charges** adjacent machines that have an energy buffer (electric furnace, crusher, ...) with the network's energy (up to 1,000 FE/t, only as much as the machine accepts). The network always keeps a reserve for its own consumption, so machines cannot black it out. To power a machine that has no Pattern Bus next to it, use the [Energy Interface](Network-and-Power#energy-interface).

| Property | Value |
|---|---|
| Pattern slots | 9 |
| Cycle | every 10 ticks |
| Pushes per cycle | 16 |
| Jobs in flight | up to 256 (in practice limited by what the machine accepts) |
| Queue size | 1,024 crafts |

If several providers hold the same pattern, orders are balanced to the one with the least pending crafts.

## Tips

- The machine must expose an item handler (inventory capability) on the face that touches the bus.
- **Mind the face.** Many machines expose different slots on different faces. A vanilla furnace takes input from the **top**, outputs from the bottom, and accepts only fuel from the sides, so put the Pattern Bus on top of a furnace.
- Make sure the machine is able to receive all inputs; a full machine slows the queue.
- The queue and its state are visible in the bus's GUI and in the Pattern Terminal's Queue tab.

## Automatic fuel for furnaces

If a furnace, blast furnace or smoker stands next to the bus, has no fuel and is not burning, and the bus has work (queued crafts or results still to collect), the bus takes **charcoal, then coal, then dried kelp blocks** from the network and puts enough into the fuel slot for the queued crafts (up to 8 items). A furnace that already has fuel is never touched. If the network has none of these fuels, the bus waits with the reason "no fuel"; after the usual delay a stuck warning is sent and the machine shows in `net.machines()` with `wait: "no_fuel"`.

