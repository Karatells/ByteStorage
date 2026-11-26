# Downloads

**Latest release:** https://github.com/Karatells/ByteStorage/releases/latest
All releases: https://github.com/Karatells/ByteStorage/releases

## Which file do I need?

| File | Required? | Description |
|---|---|---|
| `simple_bytestorage-<version>.jar` | **Yes** | The main mod. |
| `simple_bytestorage_scripting-<version>.jar` | No | The scripting addon (Logic Bus + Logic Terminal). Needs the main mod. |
| `simple_bytestorage_dimension-<version>.jar` | No | The Silicon Plane dimension addon (8 biomes, 4 mobs, endgame machines, 1M cells, Optic Wood Set). Needs the main mod. |

Releases are published only for versions that are announced as releases. Builds of every commit are available only as GitHub Actions artifacts (for testers) and may be unstable.

## Requirements

- Minecraft **1.20.1**
- Forge **47.2.0** or newer
- Java 17
- Optional: JEI (adds the "+" button that fills the Pattern Encoding Terminal)

## Installation

1. Install Forge for Minecraft 1.20.1.
2. Put `simple_bytestorage-<version>.jar` into the `mods` folder. Add the scripting and/or dimension addon jars there too if you want them.
3. On a server, install the same jars on the server **and** on every client.
4. Start the game, then craft the **Guide Book** (book + cable) for an in-game manual.

## Updating

Replace the jar(s) with the newer version. Keep the main mod and any installed addons on the same version.

Removing an optional addon from a world deletes any blocks, items, or dimension data belonging to that addon.

## Versions

| Version | Notes |
|---|---|
| 1.4.0 | Network Interface, Portable/Energy Cells, Cable Facades, Online Script Library, and the new Silicon Plane Dimension addon. |
| 1.3.1 | Bugfix and GUI polish release. |
| 1.0.0 | First stable release. |

Development builds have four parts (`1.4.0.40`); stable releases use three parts (`1.4.0`).
