# Simple Byte Storage 1.0.0

First stable release. **Minecraft 1.20.1 · Forge 47.2.0+ · Java 17.**

## Downloads

| File | What it is |
|---|---|
| `simple_bytestorage-1.0.0.jar` | The mod (**required**). |
| `simple_bytestorage_scripting-1.0.0.jar` | Optional addon: Logic Bus + Logic Terminal (JavaScript). Requires the main mod. |

Put the jar(s) into your `mods` folder. The addon must be installed on **both client and server**, like the main mod. JEI is optional.

## Highlights

- **1 item = 1 byte** storage with no limit on item types; cells from 1k to 64k.
- Network from a single controller: touch or use cables. Energy (FE) powered.
- Drives with priority and whitelist; Terminal, Crafting Terminal, and a Wireless Terminal.
- **Autocrafting**: pattern encoding (crafting and processing modes), ordering with a confirmation window, sub-crafts, queue with cancel, Assembler with speed cards, Pattern Bus for machine recipes.
- I/O Bus for automatic import/export.
- JEI "+" button fills the encoder.
- In-game **Guide Book** (en / ru / uk).
- **Scripting addon** (optional): Logic Bus runs JavaScript in a sandbox; bus identifiers let one script control groups of buses; server config with `opsOnly` and limits.

Full documentation: see the project wiki / `docs/wiki`.

## Notes

- If you do not install the scripting addon, its blocks simply do not exist.
- On public servers set `opsOnly = true` in `simple_bytestorage_scripting-common.toml` (scripts have instruction, time, item and API-call limits but no memory limit).
- Removing the scripting addon from a world deletes any placed Logic Buses and Terminals.

---

# Релиз 1.0.0 (кратко)

Первый стабильный релиз Simple Byte Storage для Minecraft 1.20.1 (Forge 47.2.0+). Положите `simple_bytestorage-1.0.0.jar` в папку `mods`. Необязательный аддон `simple_bytestorage_scripting-1.0.0.jar` добавляет Логическую шину и Логический терминал (скрипты на JavaScript); ставится и на клиент, и на сервер. JEI необязателен.
