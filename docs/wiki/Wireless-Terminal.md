# Wireless Terminal

A handheld item that opens a bound [Terminal](Terminals) from a distance.

| Property | Value |
|---|---|
| Energy capacity | 20,000 FE (item energy) |
| Cost per opening | 100 FE |
| Charge per Shift-click | up to 5,000 FE, drawn from the network |

## Usage

1. **Bind**: Shift + right-click a Terminal.
2. **Charge**: Shift + click the bound Terminal again; energy is drawn from the network.
3. **Open**: right-click in the air.

The item shows its energy as a durability bar and in the tooltip.

## Limits and messages

- The Terminal's **chunk must be loaded**, and the block must still exist (`Terminal unreachable`).
- The network must be online (`Network is offline`).
- You need at least 100 FE (`Not enough energy`).
- Not bound yet: `Not bound: Shift + right-click a Terminal`.

The item also exposes the standard Forge item energy capability, so other mods' chargers can charge it.
