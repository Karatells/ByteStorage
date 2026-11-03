# Getting Started

This page builds a minimal working network.

## What you need
- 1 **Network Controller**
- 1 **Drive** and at least one **Storage Cell**
- 1 **Terminal**
- A source of **FE** (Forge Energy) for the controller, from any energy mod

See [Recipes](Recipes) for how to craft everything.

## Steps

1. **Place the Network Controller.** It is the heart of the network.
2. **Power it.** Feed Forge Energy into the controller from any generator or cable of another mod. The controller buffers 100,000 FE and accepts up to 2,000 FE/t.
3. **Place a Drive touching the controller** (or connect it with [Cables](Network-and-Power#cable)). Open it and insert Storage Cells (up to 10).
4. **Place a Terminal touching the network.** Right-click it. You now see the whole pool of items stored in your cells.
5. Insert items by Shift-clicking them from your inventory.

## How blocks join the network

Every Simple Byte Storage device is a *grid node*. Nodes connect to each other by **touching** (face to face), or through **Cables**. Every 20 ticks (1 second) the network is re-scanned starting from the controller (a breadth-first search over adjacent nodes). Anything not reachable from the controller is offline.

There must be **exactly one controller** per connected network. Two controllers in one network put it into the `CONFLICT` state.

## Where to go next
- Want automatic crafting? Read [Autocrafting](Autocrafting).
- Want items moved automatically between chests and the network? See the [I/O Bus](IO-Bus), or the [Scripting addon](Scripting-Overview) for full control.
- The in-game [Guide Book](Guide-Book) summarises all of this.
