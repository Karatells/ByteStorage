# Scripting Administration

## Config file

`config/simple_bytestorage_scripting-common.toml`, section `[scripting]`. The **server's** values are what counts.

| Key | Default | Range | Meaning |
|---|---|---|---|
| `enabled` | `true` | | Master switch. `false` stops every Logic Bus (the blocks stay in the world). |
| `opsOnly` | `false` | | `true` = only operators (permission level 2+) and the owner of a single-player/LAN world can save, run, stop or change the interval. Everyone else gets a read-only terminal. |
| `allowCrafting` | `true` | | Allows `net.craft`. |
| `maxScriptLength` | `8000` | 100 – 30000 | Characters per script. |
| `minInterval` | `10` | 1 – 1200 | Minimum ticks between runs. |
| `maxInstructionsPerRun` | `50000` | 1000 – 5,000,000 | JS instruction budget per run. |
| `maxMillisPerRun` | `20` | 1 – 500 | Wall-clock budget per run. |
| `maxItemsPerRun` | `256` | 1 – 100,000 | Items a run may move. |
| `maxApiCallsPerRun` | `200` | 10 – 10,000 | API calls per run. |

<a name="limits"></a>
### Limits

All "per run" values apply to **each** run of `tick()` of **each** bus. A violation stops that bus with an error shown in the terminal. Buses run on the **server thread**, so keep `maxMillisPerRun` small on busy servers; total cost scales with the number of running buses.

## Security model

What the sandbox does:

- Runs in Rhino's **interpreter** mode.
- **No Java access**: a deny-all class filter is set, no `Packages`/`java` globals, and the API returns only plain JS values.
- **No** file, network, or command access.
- Limits for instructions, time, items, API calls, interpreter stack depth, and craft orders.
- A script can only look up buses of **its own network** (`side(dir, id)`, `buses(id)`); it cannot reach other networks.
- Identifiers are sanitised on the server (letters, digits, `_ - .`, max 32).
- Budget errors derive from `Error`, so scripts cannot swallow them with `try`/`catch`.
- Known memory-amplifying built-ins (`String.prototype.repeat`, `padStart`, `padEnd`, `Array.prototype.fill`) are removed.

What it does **not** do:

- **There is no memory limit.** A malicious script can still allocate a lot of memory with ordinary code (for example by repeatedly doubling a string or growing an array) within the instruction budget.
- Any player with access to a Logic Terminal can **read** scripts, even in read-only mode.

**Recommendation for public servers:** set `opsOnly = true`, or don't install the addon at all.

## Permissions summary

| Action | `opsOnly = false` | `opsOnly = true` |
|---|---|---|
| Read scripts and logs | anyone using the terminal | anyone using the terminal |
| Save / run / stop / interval (terminal) and assign IDs (Logic Bus GUI) | anyone | operators (level 2+) and the single-player owner |

The server also verifies that the player is within ~12 blocks of the terminal and that the bus belongs to the terminal's network.

## Not installing / removing the addon

- Without the addon jar, the blocks and items simply don't exist, and nothing else changes.
- If you **remove** the addon from a world that already has Logic Buses, Forge will warn about missing registry entries and those blocks disappear. Remove them first if you care about the contents.
- Clients need the addon too (it adds blocks and a GUI); the network channel versions must match (the addon's network protocol changes between versions, so a client with a different addon version can't join).
