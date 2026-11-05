# Simple Byte Storage 1.1.3

**Minecraft 1.20.1 · Forge 47.2.0+ · Java 17.**

## Downloads

| File | What it is |
|---|---|
| `simple_bytestorage-1.1.3.jar` | The mod (**required**). |
| `simple_bytestorage_scripting-1.1.3.jar` | Optional addon: Logic Bus + Logic Terminal (JavaScript). Requires the main mod. |

Install on **both client and server**. The network protocol changed (now 9) — client and server must run the same version.

## What's new since 1.1.2

- **Terminals**: items in stock are always listed first, then items that can only be crafted, marked with the word **Craft** instead of a count. Sorting works inside each group.
- **Extended search**: several words (all must match), `@mod`, `#tag`, `>100` / `<100` / `=100`, `$craftable`, `$stock`, `$fav`. **Alt + click** marks a favorite (gold corner); favorites come first in their group.
- **Controller — Recipes tab** (needs the Recipe Library): recipe blacklist, preferred ingredient variants for tags, and a "skip unpacking" switch (block -> 9 ingots) that is on by default.
- **Controller — Crafting tab**: the order queue shows progress, estimated time and priority; per order you can raise/lower the **priority**, **pause** it or **cancel** it.
- **Controller — Stock tab**: up to 8 "keep at least N" rules; the network orders the missing amount by itself.
- **Pattern Bus learning mode**: press *Record a recipe*, run the recipe in the machine by hand once, and the bus writes a processing pattern.
- Automated tests run on every build; a manual test plan lives in `docs/TESTING.md`.

## Notes and limitations

- Priority/pause apply when a machine picks its next craft: a craft that is already running always finishes first.
- Order time estimates are based on Assembler craft time; orders on Pattern Buses show no estimate.
- Learning mode records one output item per pattern; put fuel/energy into the machine before recording.
- Resource reservation for queued orders is not implemented yet.

Full documentation: project wiki / `docs/wiki`. Full history: `CHANGELOG.md`.
