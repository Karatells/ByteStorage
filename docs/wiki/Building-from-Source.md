# Building from Source

## Requirements

- JDK 17
- Internet access for Gradle (Forge/Minecraft dependencies)

## Layout

One repository, two Gradle projects:

| Project | Directory | Mod id |
|---|---|---|
| Core mod | repository root | `simple_bytestorage` |
| Scripting addon | `scripting/` | `simple_bytestorage_scripting` |

The addon compiles against the core mod's classes (`compileOnly project(':').sourceSets.main.output`) and bundles Rhino 1.7.15 into its own jar. The core jar contains no Rhino.

## Build

```bash
./gradlew build --no-daemon
```

Output (re-obfuscated jars for the game):

- `build/reobfJar/output.jar` — core mod
- `scripting/build/reobfJar/output.jar` — scripting addon

The files in `build/libs/` are the dev (deobfuscated) jars.

To build only the addon: `./gradlew :scripting:build`.

## Versions

Versions have four parts: `<major>.<minor>.<patch>.<build>`, for example `1.0.1.1`. The version is set in each project's `build.gradle` and mirrored in each `mods.toml`. The **fourth number is the build number and is increased before every push**.

## Branches and releases

- Day-to-day development happens on the **`Development`** branch; `main` holds stable code.
- Every push to `main` or `Development` builds both jars and uploads them as workflow artifacts. **No GitHub Release is created automatically.**
- A release is made only on request: *Actions → Release → Run workflow* (pick the branch) or by pushing a tag `v<version>`.

## Continuous integration

`.github/workflows/build.yml` builds on every push to `main` and `Development` and uploads separate artifacts (`simple_bytestorage-mod-jar`, `simple_bytestorage_scripting-addon-jar`, dev jars). `.github/workflows/release.yml` publishes a GitHub Release, manually only.

## Using the core API from another mod

`CraftingService.requestCraft(IGrid, ItemStack, int)` orders a craft. Nodes implement `IGridNode`; see `Logic Bus` in the addon for a complete example. The guide book accepts extra chapters via `GuideRegistry.register(...)` (see [Guide Book](Guide-Book)).
