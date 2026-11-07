# Network Controller GUI

Right-click the [Network Controller](Network-and-Power) to open its screen. It has three tabs and a slot for the [Wireless Terminal](Wireless-Terminal).

## Overview
Network status (Online / Offline / Conflict), number of connected devices, stored energy with a bar, the per-tick drain, and a count of each connected block type.

## Storage
Total disk load (used / capacity bytes with a percentage bar), number of distinct item types, and the most plentiful items in the network.

## Crafting
Number of known patterns, busy Assemblers, Pattern Bus jobs and the current craft queue as rows: remaining crafts (`x<crafts>`), a progress bar, the estimated time left, and the priority (`P+2`).
Each row has buttons: **+ / -** change the order's **priority** (-9..+9; when a machine picks its next craft it takes the highest priority first), **pause / resume** (a craft that is already running finishes first, then the order waits) and **cancel**. The estimate is based on Assembler craft time and its queue share; orders on Pattern Buses show no estimate. Priorities and pauses are stored in the Controller and apply to every machine of the network.

## Recipe Library slot
An expensive upgrade (see [Recipes](Recipes)). Installed, the network knows **every crafting-table recipe** of the game (vanilla and modded) without encoding Patterns; they can be ordered from any terminal (Ctrl + click) and used as sub-crafts. Without it only recipes you encoded are known. For ingredients with several options (tags) the item the network holds the most of is used, and the choice is fixed while a craft runs. Recipes that are not plain shaped/shapeless crafting (e.g. special recipes like dyeing) are not included. They also appear in the Pattern Terminal (without the × remove button).

## Recipes tab (recipe settings)
Settings for the Recipe Library (they only matter while it is installed):
- **Recipe blacklist** — 9 ghost slots. Drop an item on one (the item is not consumed); no library recipe that *produces* that item is available to the network any more. Click an empty-handed slot or Shift+click to clear.
- **Preferred ingredient variants** — 9 ghost slots. For ingredients with several options (tags), a listed item is used first, before "the item the network has the most of" (e.g. put Oak Planks so the network always crafts with oak).
- **Skip "unpacking" recipes** (default ON) — hides single-ingredient recipes with more than one result when a reverse recipe exists (block -> 9 ingots), so the network never unpacks your blocks by itself.

## Stock tab (stock rules)
Keep items in stock without any scripting: up to **8 rules** (4 per page, button on the right switches pages). Drop an item on a ghost slot (it is not consumed) and set the minimum with the `-100 … +100` buttons. About every 3 seconds the Controller compares the amount stored in the network with the minimum; if it is lower, it orders the missing amount through autocrafting (same plan rules as a terminal order). While that item is already in the craft queue no second order is placed. The number shows `have/minimum`: green = stocked, blue = being crafted, red with `!` = it cannot be crafted right now (no pattern, missing ingredients or no free machine) — it is retried automatically. Rules are stored in the Controller.

## Journal tab
The last events of the network's autocrafting, newest first, with how long ago they happened (`s`/`m`/`h`); use `^` / `v` to page through the 30 most recent. Entries of the same kind for the same item that follow one another closely are merged into one row (`x12`). Event types:
- **Ordered** — an order was accepted into the queue (sub-crafts get their own rows);
- **Crafted** — an Assembler finished a craft;
- **Machine started** — a Pattern Bus fed a craft into its machine;
- **Stock order** / **Stock: cannot craft** — a stock rule placed an order, or newly failed to (the reason is shown by the rule's red `!`);
- **Cancelled** — an order was cancelled from the Crafting tab.

## Warnings
The network tells you when something does not work, instead of failing silently:
- **Stock rules:** hover the red `have/minimum` text of a failing rule to see **why** it cannot be crafted (no pattern, missing ingredients with the amount, no machine, queues full). After 3 failed attempts in a row (~10 s) a chat warning is sent.
- **Stuck machines:** an Assembler or Pattern Bus that has been waiting for ingredients (or, for Assemblers, for free network storage) for more than 30 seconds is reported (journal type **Stuck** and a chat warning naming the item and what is missing). A Pattern Bus with no machine that accepts the ingredients (none adjacent, or its input slots stay full) is reported after 60 seconds, because a slow recipe can legitimately keep a machine busy.
- Warnings go to every player who has a Wireless Terminal bound to this Controller in their inventory, with the Controller's coordinates. The same warning repeats at most once per 5 minutes.

The journal keeps the last 60 events in memory and is cleared when the world is reloaded.

## Wireless Terminal slot
Place a Wireless Terminal into the slot at the bottom to bind it to this network and charge it from the controller's energy buffer. The slot's contents drop when the controller is broken.

## Stock rule schedule

The button under the rules on the **Stock** tab chooses when stock rules place orders: **always**, **night only**, **day only**, or **only when energy is high** (resumes above 50% of the Controller's buffer, pauses below 25%). While paused by the schedule the button shows "(paused now)" and no new stock orders are made; orders already queued continue. The setting is saved with the Controller.
