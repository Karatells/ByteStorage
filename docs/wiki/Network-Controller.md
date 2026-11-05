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

## Wireless Terminal slot
Place a Wireless Terminal into the slot at the bottom to bind it to this network and charge it from the controller's energy buffer. The slot's contents drop when the controller is broken.
