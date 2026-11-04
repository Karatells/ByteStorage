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

## Upgrades: Auto-Transfer Card

Craft the **Auto-Transfer Card** (see [Recipes](Recipes)) and put it into one of the two upgrade slots of the terminal settings (Shift + right-click). The terminal must be bound and anywhere in your inventory.

| Mode | Effect |
|---|---|
| **Restock** | The stack in your main or off hand is topped up from the network (e.g. keep building with cobblestone). |
| **Pickup** | Items you pick up are inserted into the network instead of your inventory (e.g. ore). Whatever does not fit stays a normal pickup. |

Each item moved costs **2 FE** of the terminal's own energy.

### Filter

Nine ghost slots (click with an item to set, click empty-handed or Shift-click to clear). The button switches **Whitelist** (only listed items) / **Blacklist** (everything except listed). An empty filter means all items. The filter applies to both Restock and Pickup.
