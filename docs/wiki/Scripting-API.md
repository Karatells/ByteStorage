# Scripting API

Scripts are JavaScript (Rhino, interpreter mode). Most of ES5 and a good part of ES6 work (`var`, `function`, regular expressions, `JSON`, `Math`, arrays, strings). If you hit an unsupported ES6 feature, use the plain ES5 form.

**There is no access to Java.** Scripts can only use the objects documented here.

## Conventions

- **Item ids** are strings: `"minecraft:iron_ingot"`. NBT is ignored (items are matched by type). An unknown id throws `unknown item: <id>`.
- **Counts** are integers. Optional counts default to "as many as possible"; negative or `NaN` values become `0`.
- Methods that move items return **how many items were moved**.
- Moving is limited by the per-run budgets (see [Administration](Scripting-Administration#limits)).

## Globals

| Name | Description |
|---|---|
| `net` | The network (see below). |
| `side(dir)` | The inventory next to **this** bus, or `null`. |
| `side(dir, id)` | The inventories next to **every bus with that ID**, as one endpoint, or `null`. |
| `buses(id)` | How many buses of the network have that ID. |
| `busId` | The ID of this bus (`""` if none). Refreshed on every run. |
| `log(...)` | Writes a line to the terminal log (16 lines kept, 160 characters each). Arguments are joined by spaces. |
| `time()` | Game time in ticks. |

## `net`

| Method | Returns | Description |
|---|---|---|
| `net.count(id)` | number | Total amount of the item in the network. |
| `net.has(id, n)` | boolean | `true` if at least `n` (default 1) are stored. |
| `net.list()` | array | Every stack: `[{slot, id, count}, …]` (max 512 entries). |
| `net.usedBytes()` | number | Bytes in use. |
| `net.freeBytes()` | number | Free bytes. |
| `net.craft(id, n)` | number | Orders autocrafting of `n` items. Returns the number of **crafts** accepted into queues (0 = no pattern, missing ingredients, or no machine). Needs [Autocrafting](Autocrafting) to be set up. |

If the bus has no network storage available, methods that need it throw `network is offline` (which stops the bus).

## `side(dir)` and `side(dir, id)`

`dir` is one of `"down"`, `"up"`, `"north"`, `"south"`, `"west"`, `"east"`, relative to the Logic Bus the inventory is read from.

- `side(dir)` — the neighbour of **this** bus on that side.
- `side(dir, id)` — the neighbour on that side of **every Logic Bus in the network whose ID is `id`** (this bus included, if it has that ID). All matching inventories are merged into **one endpoint**: slots are numbered one after another, `push` fills the inventories in turn (buses are ordered by position), `pull` and `list` cover all of them.

Both return `null` if no matching inventory exists (no bus has that ID, or nothing with an inventory sits on that side). Network devices (cables, terminals, …) are not inventories.

```js
var own     = side("up");               // above this bus
var furnace = side("north", "smelters"); // north of ALL buses called "smelters"
```

> The inventory you get is the one the block exposes **to the bus's face**. Machines such as furnaces expose different slots on different faces (for example the top feeds input, the bottom gives output). Place the bus on the face you need.

## `buses(id)` and `busId`

```js
log("I am", busId || "(no id)");
if (buses("smelters") === 0) log("no smelter buses!");
```

`buses(id)` is a cheap way to check that a group exists. It counts buses in the script's own network.

### Endpoint

An endpoint is the same object whether it comes from one bus or from a group.

| Method | Returns | Description |
|---|---|---|
| `ep.slots()` | number | Number of slots. |
| `ep.list()` | array | Non-empty slots: `[{slot, id, count}, …]`. |
| `ep.count(id)` | number | How many of the item the inventory holds. |
| `ep.pull([id], [n])` | number | Inventory → network. `id` omitted or `"*"` = any item. |
| `ep.push(id, [n])` | number | Network → inventory. `id` is required. |
| `ep.move(other, [id], [n])` | number | Inventory → another endpoint (directly, not through the network). `other` must come from `side()`. |

## Budgets per run

Every run of `tick()` gets a fresh budget:

- **Instructions** and **wall-clock time**
- **Items moved**
- **API calls**: `side`, `buses`, `net.count/has/list/craft`, `ep.list/count/pull/push/move` each use one call. `ep.slots`, `net.usedBytes`, `net.freeBytes`, `log`, `time` are free.
- **Craft calls**: at most 4 `net.craft` calls per run.

Exceeding a budget stops the bus with an error such as `instruction limit exceeded`. Items moved are clamped silently; `pull`/`push` simply return a smaller number once the item budget is used.

## Removed built-ins

To avoid one-call memory bombs, `String.prototype.repeat`, `padStart`, `padEnd` and `Array.prototype.fill` are removed.

## Errors

| Message | Cause |
|---|---|
| `script must define function tick()` | No `tick` function at top level. |
| `SyntaxError… (line N)` | The script doesn't parse. |
| `unknown item: x` | Bad item id. |
| `side(): expected down/up/north/south/west/east` | Bad direction. |
| `side(...)` returns `null` and later `Cannot call method … of null` | No bus has that ID, or nothing on that side. Check with `buses(id)` and `if (ep)`. |
| `move(): first argument must be an endpoint from side()` | Wrong argument. |
| `network is offline` | Network lost power or the bus is detached. |
| `net.craft is disabled by the server admin` | `allowCrafting = false`. |
| `instruction limit exceeded` / `time limit exceeded` | Script used too much CPU. |
| `API call limit exceeded` / `craft call limit exceeded` | Too many calls in one run. |

`try`/`catch` can handle ordinary script errors (like a bad item id). Budget violations **cannot** be caught.
