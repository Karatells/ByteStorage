# Troubleshooting

## The network doesn't work

- Is there **exactly one controller**, and is it **powered**? (`OFFLINE` = no energy, `CONFLICT` = two controllers.)
- Are all devices **touching** each other or linked by cables? Remember the 1-second re-scan.
- Did you connect through a block that is *not* a SimpleAE node? Only SimpleAE devices and cables carry the network.

## Items don't go into the drive

- Is a **whitelist** set on the drive? Empty accepts everything.
- Is the network full (`used / max bytes`)? Add cells.
- A higher-priority drive with a whitelist may be taking the items you expect elsewhere.

## Order button says it can't craft

- Missing base materials — the confirmation window lists them.
- *No Assembler* / *no memory card in the Assembler*: crafting patterns need an Assembler with a buffer cell.
- Processing patterns need a [Pattern Provider](Pattern-Provider) that **holds the pattern** and touches a machine.

## Assembler is stuck

Open it. The GUI shows the reason: no card, not enough resources, card busy or too small, network offline or full.

## Pattern Provider does nothing

- Is the machine adjacent on a face with an item handler?
- Is the machine's input full or its output blocked?
- Is the network online and the pattern a *Processing* pattern?

## Wireless Terminal won't open

Check energy (100 FE needed), that the Terminal's chunk is loaded, and that it still exists and is bound.

## Logic Bus shows `ERROR`

See the table on [Scripting API → Errors](Scripting-API#errors). Fix the script, **Save**, then **Run** again. An error always stops the bus.

## Logic Bus is `running` but nothing happens

- The network may be offline (the bus idles when offline).
- `side("…")` returned `null`: wrong direction, or the neighbour has no inventory.
- The interval might be long: check the interval shown in the terminal (the server enforces `minInterval`).
- `side(dir, id)` returned `null`: no bus has that ID (case-sensitive), or the buses with that ID have no inventory on that side. Compare with the ID list in the terminal and `buses(id)`.
- Use `log(…)` to see what the script sees.

## Build problems

See [Building from Source](Building-from-Source). When opening an issue, attach the Gradle log and the output of `latest.log`.
