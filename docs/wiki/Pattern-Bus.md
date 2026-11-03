# Pattern Bus

Runs **processing** patterns: crafts that happen in a machine such as a furnace, smoker, or any mod machine that exposes an item handler.

## Setup

1. Place the Pattern Bus **touching the machine** and connect it to the network.
2. Put encoded **processing** patterns (made in the [Encoding Terminal](Autocrafting#pattern-encoding-terminal)) into its **9 slots**.
3. Order the craft from the [Pattern Terminal](Autocrafting#pattern-terminal).

## What it does

For each craft the provider takes the inputs from the network, pushes them into the adjacent machine, and later extracts the expected outputs from **any side** of the machine and sends them to the network.

| Property | Value |
|---|---|
| Pattern slots | 9 |
| Cycle | every 10 ticks |
| Pushes per cycle | 16 |
| Jobs in flight | up to 256 (in practice limited by what the machine accepts) |
| Queue size | 1,024 crafts |

If several providers hold the same pattern, orders are balanced to the one with the least pending crafts.

## Tips

- The machine must expose an item handler (inventory capability) on the face that touches the provider.
- Make sure the machine is able to receive all inputs; a full machine slows the queue.
- The queue and its state are visible in the provider's GUI and in the Pattern Terminal's Queue tab.
