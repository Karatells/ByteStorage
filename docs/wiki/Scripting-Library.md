# Online Script Library

**Simple Byte Storage: Scripting** includes a built-in **Online Script Library** directly inside the **Logic Terminal → Library** tab. Players can browse community-made scripts, filter by category, inspect in-game item/block icons and version compatibility, and load scripts into the editor in one click.

Default repository: **`https://github.com/Karatells/sbs-scripts`**

---

## Using the Library In-Game

1. Open any **Logic Terminal** connected to an online network and select a Logic Bus.
2. Click the **`Library`** tab (next to `Code` and `Panel`).
3. The client asynchronously fetches `index.json` over HTTPS (cached for 10 minutes; click **`Reload`** at the top-right of the list to force a refresh).
4. **Filter by category** by clicking the category bar (`All`, `Storage`, `Transfer`, `Automation`, `Energy`, `Fluids`, `Display`, `Panels`, `Other`).
5. Hover over any script to view its full description and compatibility badge (`v1.4.0+` in green, or `REQ X.Y.Z` in red if your addon version is too old).
6. **Click a compatible script** to download it. The client verifies its **SHA-256 checksum** against `index.json` and inserts the code into the **Code** editor.

> **Safety note:** Loading a script from the Library **never saves or runs it automatically**. You can always read and adjust the code first, then press **Save** and **Run** yourself.

---

## Server Administration & Disabling the Library

Server administrators have full control via `config/simple_bytestorage_scripting-common.toml`:

```toml
[scripting]
  allowOnlineLibrary = true
  onlineLibraryUrl = "https://raw.githubusercontent.com/Karatells/sbs-scripts/main"
```

- Set **`allowOnlineLibrary = false`** to completely disable the Library tab for all players on your server. The setting is synced from the server to every client (`ScriptStatePacket`).
- Change **`onlineLibraryUrl`** to any HTTPS base URL containing `index.json` if you want to host a private/modpack-specific script library.

### Security guarantees
- **HTTPS only** with redirects disabled (`Redirect.NEVER`), 6-second timeout, and strict response size caps (`index.json` ≤ 512 KB, `script.js` ≤ 32 KB).
- **SHA-256 verification** on every downloaded `script.js`.
- **Path traversal protection** (`..` and leading `/` are rejected).
- **Rhino sandbox**: all scripts execute inside the same deny-all JavaScript sandbox (no Java/filesystem/network access, strict instruction & time budgets).
- **Manual PR review**: every script in the official repository is reviewed before merge.

---

## How to Submit Your Own Script (Pull Request)

Anyone can contribute scripts to **`https://github.com/Karatells/sbs-scripts`**:

### 1. Directory structure
Scripts are organized by author nickname so you can publish multiple scripts without naming conflicts:

```text
scripts/
└── <your_nick>/
    └── <script_id>/
        ├── meta.json
        └── script.js
```

- Both `<your_nick>` and `<script_id>` must match `^[a-z0-9][a-z0-9_-]{1,31}$` (lowercase letters, digits, `_`, `-`).
- Do **not** place scripts directly inside `scripts/`.

### 2. Create `meta.json`

```json
{
  "id": "my_script",
  "name": "My Script Name",
  "author": "YourNick",
  "description": "Short 1-sentence description (max 140 chars)",
  "category": "automation",
  "icon": "minecraft:redstone",
  "tags": ["energy", "redstone"],
  "minModVersion": "1.4.0",
  "maxModVersion": "*",
  "needsControl": false,
  "needsAlerts": false
}
```

#### Field reference

| Field | Required | Rules |
|---|---|---|
| `id` | Yes | Must match `<script_id>` folder name |
| `name` | Yes | 1–48 characters |
| `author` | Yes | Display name (1–32 characters) |
| `description` | Yes | 1–140 characters (shown in tooltip) |
| `category` | Yes | One of: `storage`, `transfer`, `automation`, `energy`, `fluids`, `display`, `ui`, `other` |
| `icon` | Yes | Any Minecraft or mod item/block ID (`namespace:path`, e.g. `minecraft:chest`, `simple_bytestorage:network_display`, `minecraft:water_bucket`). Unknown mod items safely fall back to `Logic Bus`. |
| `tags` | No | Up to 6 short strings (≤ 20 chars each) |
| `minModVersion` | Yes | Minimum addon version required (e.g. `"1.2.0"`, `"1.3.0"`, `"1.4.0"`) |
| `maxModVersion` | Yes | Maximum compatible version (`"*"` or `"1.4.*"`) |
| `needsControl` | No | Set `true` if script calls `stock.set/clear`, `recipes.*`, `net.cancel/pause/resume/setPriority` |
| `needsAlerts` | No | Set `true` if script calls `alert(...)` |

### 3. Write `script.js`
- Valid UTF-8 JavaScript (ES6), **max 16 KB**, clean and readable (**no obfuscation**).
- Must define at least one handler: `tick()`, `onCraft(e)`, `onLowStock(e)`, `onAlert(e)`, `onButton(id)`, or `onMessage(from, text)`.

### 4. Open a Pull Request
1. Fork `Karatells/sbs-scripts`, add your folder, and open a Pull Request.
2. GitHub Actions (`validate-pr.yml`) automatically runs `python3 tools/build_index.py` to validate your JSON, category, icon format, version range, and script size.
3. After manual review and merge to `main`, `build-index.yml` automatically rebuilds `index.json` with SHA-256 checksums, and your script appears in-game for all players.
