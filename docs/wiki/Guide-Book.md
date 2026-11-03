# Guide Book

The **SimpleAE Guide** is an in-game manual. Craft it with a **Book + Cable** (shapeless), then right-click.

- Chapters on the left, text on the right. Scroll with the mouse wheel, PageUp/PageDown or the arrow keys.
- Available in English, Russian and Ukrainian (follows your game language).
- **One book for both mods.** When the Scripting addon is installed, it appends its own chapters (Start, API, Examples, Admin) to the same book. Without the addon, those chapters don't appear.

## For addon authors

Other mods can add chapters through the client-side registry:

```java
GuideRegistry.register(new GuideRegistry.Chapter(
        "my_chapter",          // unique id
        200,                   // sort order (lower = higher in the list)
        "guide.mymod.title",   // lang key for the title
        "guide.mymod.body"));  // lang key for the body (\n = paragraph, § codes supported)
```

Call it from `FMLClientSetupEvent` (inside `enqueueWork`).
