# Simple Byte Storage 1.1.2 (beta)

**Minecraft 1.20.1 · Forge 47.2.0+ · Java 17.** This is a **beta**: the new features were checked by the build only and need testing in play. Please report problems.

## Downloads

| File | What it is |
|---|---|
| `simple_bytestorage-1.1.2.jar` | The mod (**required**). |
| `simple_bytestorage_scripting-1.1.2.jar` | Optional addon: Logic Bus + Logic Terminal (JavaScript). Requires the main mod. |

Install on **both client and server**. The network protocol changed — client and server must run the same version.

## What's new since 1.1.0

- **Recipe Library** — an expensive Network Controller upgrade (4 netherite ingots, 4 diamond blocks, nether star). The network then knows **every crafting-table recipe** of the game (vanilla and modded) without encoding Patterns; they can be ordered from any terminal and used as sub-crafts, and they are listed in the Pattern Terminal. Without it only the recipes you encoded are known.
- **Wireless Terminal settings redesign** — tabs (Upgrades / Transfer / Macro), 8 upgrade slots, 18-slot filter with whitelist/blacklist.
- **New Wireless Terminal cards**:
  - **Insurance Card** — on death your items go into the network and are returned when you respawn.
  - **Tool Card** — a broken tool is replaced from the network.
  - **Macro Card** — a hotkey (default V) runs Logic Buses by identifier (needs the Scripting addon). Addons can hook in through the new `WirelessMacros` API.
- **Unified craft plan window** — ordering from the Terminal and Crafting Terminal now shows the same plan as the Pattern Terminal: what will be crafted (sub-crafts included), what is missing, Start / Cancel, with an adjustable amount.
- The Release workflow can also publish to CurseForge (optional) and supports beta releases.

## Known limitations

- The Tool Card replaces a broken tool with the first matching item of the same kind (enchantments and durability are not matched).
- In the Pattern Terminal, tag ingredients of library recipes show their first variant (the ✓/✗ marks use the item the network actually picked).
- Special (non shaped/shapeless) crafting recipes are not part of the Recipe Library.

Full documentation: project wiki / `docs/wiki`. Full history: `CHANGELOG.md`.
