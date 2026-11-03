# Scripting Overview

**SimpleAE: Scripting** is an **optional addon**, shipped as a separate jar built from the same repository. It requires SimpleAE: ByteStorage.

- **Not installed** → the Logic Bus and Logic Terminal do not exist in the game at all.
- **Installed** → two new blocks, a creative tab, a server config, and new chapters in the [Guide Book](Guide-Book).

The addon bundles **Rhino** (a JavaScript engine for Java). Rhino is *only* inside the addon jar; the core mod never contains it.

## Blocks

| Block | Role |
|---|---|
| **Logic Bus** | Runs one JavaScript script. Sees its six neighbouring inventories and the network. |
| **Logic Terminal** | Opens the script editor for every Logic Bus connected to the same network. |

Both are normal network nodes: they must connect to an online controller (touching or via cables).

## Workflow

1. Place the Logic Terminal and one or more Logic Buses on the network.
2. Put each Bus next to the inventory (chest, furnace, machine) it should control.
3. Right-click the Terminal. Pick a Bus from the list on the left.
4. Write the script in the editor, then press **Save**.
5. Press **Run**. The status line and the log below the editor show what happens.

Buttons:

| Button | Action |
|---|---|
| Save | Stores the script on the bus. **Saving stops the bus**, press Run to start it. |
| Run / Stop | Starts or stops the script. |
| Reload | Discards edits and reloads the script stored on the bus. |
| − / + | Changes the run interval by 10 ticks. |
| Set ID | Assigns the identifier typed in the ID field to the selected bus (see below). |

The terminal must be within ~12 blocks of the player for actions to be accepted. If `opsOnly` is on and you aren't allowed, the terminal is read-only.

## Identifiers

Every Logic Bus can have an **identifier** (ID). A script always runs on one bus, but with an ID it can reach the inventories next to **other** buses of the same network, selected by name.

- Allowed characters: letters, digits, `_`, `-`, `.`. Maximum 32 characters. Anything else is removed. IDs are case-sensitive. An empty ID means "no ID".
- **IDs need not be unique.** Several buses with the same ID form a **group**: `side("up", "smelters")` returns **one endpoint** that covers the inventories above *all* buses with the ID `smelters`.
- A script can only reach buses of **its own network**.

### In the terminal

The right-hand side of the Logic Terminal is the ID panel:

- **Identifiers**: a list of every ID that already exists in this network, with the number of buses using it (`x3` = three buses). The ID of the selected bus is highlighted. **Click an ID** to copy it into the field below.
- **Bus ID field** and **Set ID** button: type an ID and press the button to assign it to the selected bus. An empty field **removes** the ID.
- In the bus list on the left, a bus with an ID is shown by its ID (in colour), otherwise by its coordinates. Hover a row to see ID and coordinates.

Changing an ID does not stop the bus and doesn't need a re-save. The script sees the new value on its next run (`busId` is refreshed every run; `side(…, id)` looks buses up each call).

## Execution model

```js
// 1. The top level runs ONCE when the bus starts (compile + init).
var chest = side("up");
var counter = 0;

// 2. tick() runs every <interval> ticks.
function tick() {
  counter++;
}
```

- A script **must define `function tick()`**, otherwise the bus reports an error.
- **Global variables persist** between runs, but are reset when the script is saved, the world reloads, or the bus is restarted (the top level runs again).
- The default interval is 20 ticks (1 s). The server enforces a minimum (default 10 ticks); max 1,200.
- A bus runs only while it is **enabled**, its network is **online**, and scripting is **enabled** in the config.
- Any error, or exceeding a limit, **stops the bus**, sets the status to `ERROR: <reason>` and writes it to the log.

## Status values

| Status | Meaning |
|---|---|
| *(empty)* | Never started |
| `saved` | Script saved, bus stopped |
| `running` | Active |
| `stopped` | Stopped by the player |
| `ERROR: …` | Stopped by an error (syntax error, runtime error, limit exceeded) with line number where available |

## Persistence

Script text, identifier, enabled flag and interval are stored in the block entity's NBT. Script variables and the log are **not** persisted. After a world restart an enabled bus recompiles and continues.
