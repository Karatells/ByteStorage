# SimpleAE: Scripting (необов'язковий аддон)

Додає **Логічну шину** та **Логічний термінал**. Без цього jar блоків у грі немає, основний мод працює як раніше.
Rhino (JavaScript) вкладено лише в jar аддона.

## Використання
1. Постав Логічний термінал і Логічні шини так, щоб вони з'єднувались з мережею (суміжно/кабелем).
2. ПКМ по терміналу → вибери шину зі списку → пиши скрипт → **Save** → **Run**.
3. Шина бачить сусідні інвентарі (`side("up")`) і мережу (`net`).

## Скрипт
Верхній рівень виконується один раз, `tick()` — кожні N тіків (інтервал у терміналі). Глобальні змінні живуть між тіками.

```js
var chest = side("up");
function tick() {
  if (!chest) return;
  chest.pull("minecraft:iron_ingot");              // інвентар -> мережа
  if (net.count("minecraft:iron_ingot") > 500) {
    log("багато заліза");
  }
  if (net.count("minecraft:stick") < 64) net.craft("minecraft:stick", 64);
}
```

API (предмети — рядком `"mod:item"`, NBT не враховується):
- `net.count(id)`, `net.has(id, n)`, `net.list()`, `net.usedBytes()`, `net.freeBytes()`, `net.craft(id, n)`
- `side("down|up|north|south|west|east")` → ендпоінт або `null`
- ендпоінт: `slots()`, `list()`, `count(id)`, `pull([id], [n])`, `push(id, [n])`, `move(otherEndpoint, [id], [n])`
- `log(...)`, `time()`

Будь-яка помилка або перевищення ліміту зупиняє шину і показується в терміналі.

## Безпека та налаштування сервера
Файл `config/simple_bytestorage_scripting-common.toml`: `enabled`, `opsOnly`, `allowCrafting`, `maxScriptLength`,
`minInterval`, `maxInstructionsPerRun`, `maxMillisPerRun`, `maxItemsPerRun`, `maxApiCallsPerRun`.

Пісочниця: інтерпретатор Rhino, жодного доступу до Java-класів, ліміт інструкцій і часу, ліміт переміщених предметів.
**Обмеження:** немає ліміту пам'яті (зловмисний скрипт теоретично може виділити багато RAM, напр. подвоєнням рядка в циклі),
тому на публічних серверах радимо `opsOnly = true`.
