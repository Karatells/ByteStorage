# Scripting Examples

Paste an example into the Logic Terminal, press **Save**, then **Run**. Examples marked *(IDs)* need buses with identifiers: select the bus, type the ID in the right-hand panel and press **Set ID**. Directions (`"up"`, `"north"`, …) are relative to the Logic Bus; change them to match your build. Item ids are 1.20.1 ids.

## 1. Import everything from a chest

```js
var chest = side("up");

function tick() {
  if (chest) chest.pull();      // any item, as many as the budget allows
}
```

## 2. Import only some items

```js
var chest = side("up");
var wanted = ["minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond"];

function tick() {
  if (!chest) return;
  for (var i = 0; i < wanted.length; i++) {
    chest.pull(wanted[i]);
  }
}
```

## 3. Keep a machine stocked

```js
var machine = side("north");

// Top up `id` in `ep` until it holds `amount`.
function ensure(ep, id, amount) {
  var have = ep.count(id);
  return have < amount ? ep.push(id, amount - have) : 0;
}

function tick() {
  if (!machine) return;
  ensure(machine, "minecraft:coal", 16);
}
```

## 4. Stock keeper with autocrafting and a cooldown

Without a cooldown the script would order the same craft again and again while the first order is still running.

```js
var targets = [
  { id: "minecraft:stick",    min: 64,  batch: 64 },
  { id: "minecraft:torch",    min: 128, batch: 64 },
  { id: "minecraft:iron_block", min: 8, batch: 4 }
];
var lastOrder = {};            // id -> game time of the last order
var COOLDOWN = 20 * 60;        // 1 minute

function tick() {
  for (var i = 0; i < targets.length; i++) {
    var t = targets[i];
    var last = lastOrder[t.id];
    if (last !== undefined && time() - last < COOLDOWN) continue;
    if (net.count(t.id) < t.min) {
      var crafts = net.craft(t.id, t.batch);
      if (crafts > 0) {
        lastOrder[t.id] = time();
        log("ordered", t.batch, t.id);
      } else {
        log("cannot craft", t.id);
      }
    }
  }
}
```

## 5. Sort a chest: ores and wood to separate chests, the rest to the network

```js
var input = side("up");
var ores  = side("west");
var wood  = side("east");

function tick() {
  if (!input) return;
  var stacks = input.list();
  for (var i = 0; i < stacks.length; i++) {
    var id = stacks[i].id;
    if (ores && /_ore$|^minecraft:raw_/.test(id)) {
      input.move(ores, id);
    } else if (wood && /_log$|_planks$/.test(id)) {
      input.move(wood, id);
    } else {
      input.pull(id);          // everything else goes into the network
    }
  }
}
```

## 6. Overflow protection

When free space gets low, dump junk into an overflow chest so the network never fills up.

```js
var overflow = side("down");
var junk = ["minecraft:cobblestone", "minecraft:dirt", "minecraft:gravel",
            "minecraft:cobbled_deepslate", "minecraft:rotten_flesh"];
var alarm = false;

function tick() {
  var low = net.freeBytes() < 2000;
  if (low !== alarm) {                 // log only when the state changes
    alarm = low;
    log(low ? "LOW SPACE: " + net.freeBytes() + " bytes free" : "space OK");
  }
  if (!low || !overflow) return;
  for (var i = 0; i < junk.length; i++) {
    overflow.push(junk[i], 256);
  }
}
```

## 7. Round-robin feeding of two machines

Global variables survive between runs, so a counter can alternate targets.

```js
var machines = [side("east"), side("west")];
var turn = 0;

function tick() {
  var target = machines[turn % machines.length];
  turn++;
  if (target) target.push("minecraft:raw_iron", 8);
}
```

## 8. Throughput meter

Logs how many items per second flow into the network.

```js
var ITEM = "minecraft:iron_ingot";
var lastCount = null;
var lastTime = 0;

function tick() {
  var now = net.count(ITEM);
  var t = time();
  if (lastCount !== null && t > lastTime) {
    var perSecond = (now - lastCount) / ((t - lastTime) / 20);
    log(ITEM, perSecond.toFixed(2) + "/s");
  }
  lastCount = now;
  lastTime = t;
}
```

## 9. Balance two chests

Evens out a specific item between two chests by moving items directly from the fuller one to the emptier one.

```js
var a = side("west");
var b = side("east");
var ID = "minecraft:cobblestone";

function tick() {
  if (!a || !b) return;
  var ca = a.count(ID), cb = b.count(ID);
  var diff = Math.floor((ca - cb) / 2);
  if (diff > 0) a.move(b, ID, diff);
  else if (diff < 0) b.move(a, ID, -diff);
}
```

## 10. Self-diagnosing script

Wrap the work in `try`/`catch` to keep running through ordinary errors (such as a mistyped item id) and report them.

```js
var chest = side("up");

function tick() {
  try {
    chest.pull("minecraft:iorn_ingot");   // typo: throws "unknown item"
  } catch (e) {
    log("problem:", e.message);
  }
}
```

Budget errors (instruction, time, call limits) cannot be caught, they stop the bus.

---

# Examples with identifiers

## 11. Feed a group of furnaces (IDs)

Give every bus that sits next to a furnace the ID `smelters`. **One** script on any bus (even one without the ID) feeds all of them. `push` fills them in order.

```js
function tick() {
  var furnaces = side("north", "smelters");   // north of every "smelters" bus
  if (!furnaces) return;
  furnaces.push("minecraft:raw_iron", 16);
  furnaces.push("minecraft:coal", 8);
}
```

## 12. Many input chests, many output chests (IDs)

Buses with ID `inbox` have chests on top, buses with ID `outbox` have chests below.

```js
function tick() {
  var inbox  = side("up", "inbox");
  var outbox = side("down", "outbox");
  if (inbox)  inbox.pull();                        // collect everything into the network
  if (outbox) outbox.push("minecraft:cobblestone", 64);
}
```

## 13. Sorting by group (IDs)

```js
var sources = side("up", "sort_in");
var ores    = side("up", "sort_ores");
var wood    = side("up", "sort_wood");

function tick() {
  if (!sources) return;
  var stacks = sources.list();
  for (var i = 0; i < stacks.length; i++) {
    var id = stacks[i].id;
    if (ores && /_ore$|^minecraft:raw_/.test(id))      sources.move(ores, id);
    else if (wood && /_log$|_planks$/.test(id))        sources.move(wood, id);
  }
}
```

## 14. Status report for several groups (IDs)

```js
var groups = ["smelters", "inbox", "outbox"];

function tick() {
  var parts = [];
  for (var i = 0; i < groups.length; i++) {
    parts.push(groups[i] + "=" + buses(groups[i]));
  }
  log("buses:", parts.join(" "));
}
```

## 15. One script, behaviour chosen by ID (IDs)

The same script text can be pasted into several buses; `busId` tells each one who it is.

```js
function tick() {
  var me = side("up");
  if (!me) return;
  if (busId === "importer")      me.pull();
  else if (busId === "coal")     me.push("minecraft:coal", 32);
  else                           log("no role for", busId || "(no id)");
}
```

## 16. Fail-safe group access (IDs)

```js
function group(dir, id) {
  var ep = side(dir, id);
  if (!ep) log("group", id, "not found on side", dir, "(" + buses(id) + " buses)");
  return ep;
}

function tick() {
  var ep = group("up", "furnaces");
  if (ep) ep.push("minecraft:coal", 8);
}
```
