# Test plan — Simple Byte Storage

Before every **non-beta** release go through the checklist below in a real game (Minecraft 1.20.1, Forge 47.2.0),
ideally on a dedicated server **and** in single player (a few items behave differently on the logical client).
Automated tests (`./gradlew test`, run by CI on every push) cover only pure logic and resource consistency:
search syntax, order selection / ETA formatting, language files (same keys, every key used in code exists),
version consistency across the four files, forbidden wording, valid model JSON.

Legend: **Expect** is the result that counts as a pass.

## 0. Smoke
- [ ] The game starts with the core mod only. **Expect:** no errors in the log.
- [ ] The game starts with core + Scripting addon. **Expect:** no errors; Logic Bus appears in the creative tab.
- [ ] Open the Guide book: every chapter opens, no `guide.simple_bytestorage...` raw keys. Switch language ru/uk/en.
- [ ] A world made with the previous release loads (controller, drives, patterns, assemblers keep their data).

## 1. Network basics
- [ ] Controller + cable + drive + terminal: network goes ONLINE (LED green). Second controller -> CONFLICT.
- [ ] Break the controller / remove energy -> OFFLINE, terminals show offline.
- [ ] Controller tabs Overview / Storage / Crafting / Recipes / Stock open and show data; no overlapping text.

## 2. Terminals
- [ ] Insert / extract with click, right click (half), Shift (to inventory); counts above 64 use the small font.
- [ ] **Order:** items in stock first, then craft-only items with the word "Craft" instead of the count.
- [ ] Sort buttons (name/count, asc/desc) work inside both groups.
- [ ] Search: `iron`, `@minecraft`, `#ingots`, `>64`, `<10`, `=0`, `$craftable`, `$stock`, `$fav`, and combinations.
- [ ] **Alt + click** marks a favorite (gold corner), favorites are first in their group, survive a game restart.
- [ ] Craft-only item click opens the order dialog; Ctrl+click / middle click on an in-stock craftable item too.
- [ ] Order dialog: lists sub-crafts and missing items, amount buttons work, Start disabled when impossible.
- [ ] Crafting Terminal: the 3x3 grid, result slot, shift-click craft.

## 3. Autocrafting
- [ ] Assembler (with Buffer Cell) crafts a simple recipe from a Pattern; speed cards make it faster.
- [ ] Multi-level recipe (sticks -> tool): sub-crafts are queued in the right order.
- [ ] Pattern Encoding Terminal: crafting-table recipe is written without a Pattern; processing recipe needs a Pattern.
- [ ] Pattern Terminal shows patterns and (with the library) library recipes without the remove button.
- [ ] Pattern Bus + furnace: order of N items, results come back to the network.
- [ ] **Learning mode:** machine next to the bus -> Record a recipe -> put ingredients in by hand -> a pattern appears
      in a free slot; the pattern then works for an order. Test with a furnace (fuel first) and a machine that
      takes all ingredients at once. Check the messages: no free slot / no machine / could not work out.
- [ ] Break an Assembler / Bus mid-craft: nothing is duplicated, buffer content is dropped or returned.

## 4. Controller: queue
- [ ] Queue rows show remaining crafts, progress bar, ETA, priority.
- [ ] + / - change priority; with two queued orders the higher priority is crafted first (when the machine picks the next one).
- [ ] Pause: the running craft finishes, then the order waits; resume continues. Works for Assemblers and Pattern Buses.
- [ ] Cancel removes the order and returns buffer contents to the network.
- [ ] Priorities / pauses survive a world reload.

- [ ] **Reservation:** order a large craft, then try to take its ingredients out by hand -> only the free remainder can be taken; a second order needing the same ingredients reports "missing" instead of stealing; cancelling the first releases them; after all crafts finish everything is extractable again.

## 5. Controller: recipes (needs the Recipe Library)
- [ ] Library installed -> all crafting-table recipes are orderable from terminals; removed -> they disappear.
- [ ] Blacklist: item in a ghost slot -> recipes producing it vanish from terminals/plans. Clear -> they return.
- [ ] Preferred variants: with planks in the preference list, a recipe with a "planks" tag uses that variant.
- [ ] Skip "unpacking" ON: block -> 9 ingots is not craftable; OFF: it is. Log -> 4 planks stays available.
- [ ] Settings persist after reload; ghost slots do not take or give items.

## 6. Controller: stock rules
- [ ] Rule item + minimum: with fewer items, an order is placed within ~3 s; no second order while it is queued.
- [ ] Display `have/minimum` turns green when stocked, red with `!` when it cannot be crafted (no pattern/ingredients/machine).
- [ ] Both pages (rules 1-4 and 5-8) work; amount buttons clamp at 0.
- [ ] Rules persist after reload; a rule with minimum 0 is ignored.

- [ ] **Warnings:** a stock rule for an item with no pattern -> after ~10 s a chat warning (player carries a bound Wireless Terminal) and the tooltip over the red `!` names the reason; an Assembler waiting for missing ingredients > 30 s -> "Stuck" in the Journal + chat warning; warnings do not repeat more often than every 5 minutes.

## 7. Wireless Terminal and cards
- [ ] Bind in the controller slot, charge from the controller; Shift+RMB opens settings; all tabs usable.
- [ ] Auto-Transfer card: restock and pickup obey the whitelist/blacklist filter.
- [ ] Insurance card: items are returned after death/respawn; Tool card swaps a broken tool; Macro card runs a macro (key binding).
- [ ] With the Scripting addon: a Logic Bus whose identifier equals the macro name runs.

## 8. Multiplayer / compatibility
- [ ] Two players use the same terminal / controller GUI at the same time.
- [ ] Client with a different mod version is refused (protocol 9).
- [ ] JEI installed: recipe transfer into the Crafting Terminal works; JEI absent: no crash.

## 9. Before the release
- [ ] CI is green on `Development`; `main` is fast-forwarded to it; version set to the release number in all four places.
- [ ] CHANGELOG and release notes updated, wiki pages match the behaviour.

## Version numbering
`major.minor.patch.build` — the **build** number is global and never resets: it grows by 1 before every push.
After a release `X.Y.Z` development continues as `X.Y.(Z+1).<next build>`; e.g. after `1.1.3.9` and a release `1.1.4`
the first build of the next cycle is `1.1.4.10`.
