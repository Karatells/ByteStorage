# Wireless Terminal

A handheld item that opens the network from a distance and, with an upgrade, moves items for you automatically.

| Property | Value |
|---|---|
| Energy capacity | 20,000 FE (item energy) |
| Cost per opening | 100 FE |
| Charging | Automatically from the [Network Controller](Network-Controller) while it sits in the controller's slot (50 FE/t) |

## Usage

1. **Bind and charge**: put the terminal into the slot of the [Network Controller](Network-Controller) GUI. It binds to that network and charges from the controller's buffer. Take it out when done.
2. **Open**: right-click in the air.
3. **Settings**: Shift + right-click in the air.

The item shows its energy as a durability bar and in the tooltip. It also exposes the standard Forge item energy capability.

## Limits and messages

- The controller's **chunk must be loaded** (`Terminal is out of reach`).
- The network must be online (`The network is offline`).
- You need at least 100 FE (`Not enough energy: charge it in the Network Controller`).
- Not bound yet: `Not bound: place it in the Network Controller's slot`.

## Settings and upgrades

Shift + right-click the terminal in the air to open its settings. There are three tabs:

- **Upgrades** — 8 slots for cards; each kind of card can be installed once.
- **Transfer** — Restock / Pickup switches, Whitelist / Blacklist, and an 18-slot ghost filter (click with an item to set, click empty-handed or Shift-click to clear; empty filter = everything). Works with the Auto-Transfer Card.
- **Macro** — the macro name used by the Macro Card.

| Card | Effect | Cost |
|---|---|---|
| **Auto-Transfer Card** | *Restock*: the stack in your main/off hand is topped up from the network. *Pickup*: picked-up items go into the network (whatever does not fit stays a normal pickup). Filtered. | 2 FE per item |
| **Insurance Card** | When you die, the items you would drop are moved into the network (as far as the terminal's energy allows; the terminal itself too). When you respawn, they are taken back out; armor is re-equipped. If the network is unreachable they wait in it. | 2 FE per item |
| **Tool Card** | When a tool breaks, the same kind of item is taken from the network into the same hand. | 20 FE |
| **Macro Card** | A hotkey (default **V**, see Controls) runs once every Logic Bus whose identifier equals the macro name. Needs the [Scripting addon](Scripting-Overview). | 50 FE |

The terminal must be bound and anywhere in your inventory for cards to work. Recipes are in [Recipes](Recipes).
