# Terminals

## Terminal

Browse and withdraw items from the whole network.

- **Search box** for quick filtering.
- **Scrolling** through large item lists.
- **Byte usage bar**: used / maximum bytes.
- **Shift-click** moves whole stacks; stack splitting is supported.
- Insert items by Shift-clicking them from your inventory or clicking with a stack on the cursor.
- **Sorting** (two buttons to the right of the window, shared by all terminals): by *name* or *count*, ascending (A-Z / 1-9) or descending (Z-A / 9-1).
- **Autocrafting from any terminal** (Terminal and Crafting Terminal): items the network can craft but does not have appear dimmed, listed after everything that is in stock, with the word **Craft** instead of a count — click one to open the order dialog. For items in stock, **Ctrl + click** (or middle click) does the same. The window is the same as in the Pattern Terminal: it lists everything that will be crafted (sub-crafts included) and what is missing; pick an amount (±1/±10/±64) and press *Start*.

## Search and favorites
The search box takes several words; **all** must match:

| Word | Meaning |
|------|---------|
| `iron` | part of the item name |
| `@mod` | the item's mod id contains `mod` |
| `#tag` | the item has a tag containing `tag` (e.g. `#ingots`) |
| `>100` `<100` `=100` | stored amount (craft-only items count as 0) |
| `$craftable` | the network can craft it |
| `$stock` | it is stored in the network |
| `$fav` | favorites only |

Example: `@minecraft #ingots >64`. **Alt + click** an item to mark it as a favorite (gold corner); favorites are listed first inside their group. Favorites are stored on your computer (`config/simple_bytestorage_favorites.txt`). The list order is always: items in stock first, then craft-only items.

## Crafting Terminal

A Terminal with a **3×3 crafting grid** and a result slot.

- Results follow the standard vanilla crafting recipes.
- Ingredients are taken from the network's virtual pool (1 item = 1 byte).
- The crafted result goes straight to your inventory.

## Pattern Terminal and Pattern Encoding Terminal

These belong to autocrafting — see [Autocrafting](Autocrafting).

## Wireless Terminal

Open a Terminal remotely — see [Wireless Terminal](Wireless-Terminal).

## Power

Terminals need the network to be online (see [Network and Power](Network-and-Power)).

## Item tooltip

Hovering an item in a terminal shows how many are stored, how many of them are **reserved** for accepted orders, and how many are currently **in production** (queued or running crafts).

## Order dialog: "Max"

If an order cannot be done in full (an ingredient is missing), the **Start** button becomes **Max: N**, where N is the largest amount the network can craft right now. Click it to recalculate the plan for N, then press **Start**.
