# Network Controller GUI

Right-click the [Network Controller](Network-and-Power) to open its screen. It has three tabs and a slot for the [Wireless Terminal](Wireless-Terminal).

## Overview
Network status (Online / Offline / Conflict), number of connected devices, stored energy with a bar, the per-tick drain, and a count of each connected block type.

## Storage
Total disk load (used / capacity bytes with a percentage bar), number of distinct item types, and the most plentiful items in the network.

## Crafting
Number of known patterns, busy Assemblers, Pattern Bus jobs and the current craft queue (`x<crafts>`, `*` marks a craft that is actively running).

## Recipe Library slot
An expensive upgrade (see [Recipes](Recipes)). Installed, the network knows **every crafting-table recipe** of the game (vanilla and modded) without encoding Patterns; they can be ordered from any terminal (Ctrl + click) and used as sub-crafts. Without it only recipes you encoded are known. For ingredients with several options (tags) the item the network holds the most of is used, and the choice is fixed while a craft runs. Recipes that are not plain shaped/shapeless crafting (e.g. special recipes like dyeing) are not included. The Pattern Terminal does not list library recipes.

## Wireless Terminal slot
Place a Wireless Terminal into the slot at the bottom to bind it to this network and charge it from the controller's energy buffer. The slot's contents drop when the controller is broken.
