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
