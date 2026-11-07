# Autocrafting

Simple Byte Storage has two kinds of crafts:

| Kind | Where it runs | Pattern lives in |
|---|---|---|
| **Crafting** (crafting table recipes) | [Assembler](Assembler) | Global network pattern list |
| **Processing** (furnaces, other machines) | [Pattern Bus](Pattern-Bus) + a machine | The Pattern Bus |

## Patterns

A **Pattern** item is blank or encoded. An encoded pattern stores its recipe in NBT and can be overwritten.

## Pattern Encoding Terminal

1. Choose the mode with the mode button:
   - **Crafting** — click items onto the 3×3 grid (they are *not* consumed). The vanilla recipe decides the result. Press *Encode*: the recipe is added straight to the network's global list. **No Pattern item is needed.**
   - **Processing** — insert a blank Pattern, then set the inputs and the result by hand (ghost slots, with counts). The pattern is a physical item: put it into a [Pattern Bus](Pattern-Bus). It is *not* added to the global list.
2. Patterns are only needed for processing crafts (furnaces, crushers and other machines).
3. *Clear* empties the grid.

With **JEI**, the "+" button on a recipe fills the encoder for you (see [JEI Integration](JEI-Integration)).

## Ordering from any terminal

You do not have to open the Pattern Terminal to order a craft: in the Terminal and the Crafting Terminal, click a dimmed (craftable, out of stock) item or Ctrl + click / middle-click an item in stock to open an order dialog. See [Terminals](Terminals).

## Pattern Terminal

Shows the list of every pattern available to the network (global patterns plus patterns inside Pattern Buss).

- **Search** and an **All / Ready** filter ("Ready" = all ingredients available).
- Hover a pattern to see each ingredient marked ✓ (in the network) or ✗ (missing). The list row also shows *Missing: <item>*.
- **Order**: left-click = 1 craft, Shift = 10, Ctrl = 64.
- A **confirmation window** shows the full plan, including sub-crafts and anything missing, before you confirm.
- The **Queue tab** lists running orders per pattern, how many machines work on them, and lets you **cancel**.
- Shift + left-click on × removes a global pattern.

## Sub-crafts

If an ingredient is missing but another pattern in the network produces it, the planner schedules that craft first (recursively).

| Limit | Value |
|---|---|
| Max sub-craft depth | 8 |
| Max crafts in one order | 1,024 |

Steps are ordered with dependencies first. If a plan has missing base materials the confirmation window lists them and the order can't be placed.

## Requirements

- Network online.
- For crafting patterns: at least one [Assembler](Assembler) with a buffer cell.
- For processing patterns: a [Pattern Bus](Pattern-Bus) holding the pattern, touching a machine.

## Learning mode (Pattern Bus)
Instead of encoding a processing pattern by hand you can let the bus **record** it:
1. Place the machine next to the Pattern Bus and make sure the bus has a free pattern slot.
2. Open the bus and press **Record a recipe**. (Put fuel/energy into the machine first, otherwise it is recorded as an ingredient.)
3. Put the ingredients into the machine yourself, once, and let it finish one cycle.
4. When the result appears, the bus writes a processing pattern (what the machine consumed → what it produced) into a free slot. No blank pattern is needed.

While recording the bus does not move items by itself. A pattern holds a single output item; if the machine makes several products, the largest gain is used. Machines that take all ingredients at once and machines that consume one item per cycle both work, because the pattern is based on what disappeared when the result appeared.

## Resource reservation
When an order is accepted, the **raw materials taken from the network storage** for it (not intermediate items made by sub-crafts) are **reserved**. Reserved items:
- cannot be taken out by terminals, I/O Buses or the Wireless Terminal (the terminal simply shows less available);
- are not counted when a *new* order is planned, so two orders can no longer both rely on the same iron;
- are still used by the Assemblers and Pattern Buses for crafting, and the reservation shrinks as they take the items.

Cancelling an order releases what is left of its reservation; when the whole craft queue is empty all reservations are cleared automatically. Reservations are saved with the Controller, so they survive world reloads together with the queue (a reservation whose order no longer exists is dropped as soon as the queue is empty). Intermediate items (e.g. sticks made for a pickaxe) are not reserved while they sit in the storage.
