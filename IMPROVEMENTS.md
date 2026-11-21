# Анализ проекта Simple Byte Storage и идеи по улучшению

Документ составлен по результатам анализа кодовой базы проекта **Simple Byte Storage** (Minecraft 1.20.1 Forge, основной мод + аддон `scripting`, версия `1.2.1`).  
Ниже отмечен статус реализации каждого пункта (`[x]` — реализовано, `[ ]` — не реализовано / в планах).

---

## 1. Сильные стороны текущего проекта

- **Чёткая и чистая архитектура:** логика разделена на `api`, `block/entity`, `storage`, `network`, `menu`, `client/gui`, `util` и отдельный подпроект `scripting`.
- **Выделение чистой логики для тестов:** `SearchSyntax`, `QueueOrder`, `ReservationLedger`, `CapacitorLogic`, `JournalLog`, `StatsHistory`, `StockSchedule`, а также лексер/буфер/автодополнение редактора кода (`util/editor`) вынесены из Minecraft-классов и покрыты JUnit 5 тестами.
- **Проверка ресурсов в CI (`ResourcesTest`):** автоматическая сверка ключей локализации (`en_us`, `ru_ru`, `uk_ua`), проверка используемых в коде ключей, консистентности 4-компонентных версий и валидности JSON-моделей.
- **Безопасность сетевых пакетов и песочницы скриптов:** серверные обработчики пакетов (например, `ControllerQueuePacket`, `TerminalService`) проверяют `stillValid`, дистанцию и права (`ScriptingConfig.canEdit`), а Rhino-песочница (`SandboxFactory`) использует интерпретатор (`-1`), `ClassShutter`, лимит глубины стека, инструкций и времени.

---

## 2. Производительность и оптимизация

- [x] **2.1. Кэширование `ICellInventory` в `DriveBlockEntity`:**
  - В `IByteStorageCell` добавлен метод `getCellInventory(ItemStack, boolean autoFlush)`, а в `DriveBlockEntity` реализовано кэширование `ICellInventory` по слотам с инвалидацией при изменении слота (`onContentsChanged`) и гарантированным сбросом в NBT (`flush()`) при извлечении диска, изменении содержимого и сохранении чанка (`saveAdditional`).
- [x] **2.2. Оптимизация `ByteCellInventory` (индекс `O(1)` и отложенная сериализация NBT):**
  - Внутренний список заменён на `LinkedHashMap<ItemKey, ItemStack>` с кэшированием списка `getStoredItems()` и быстрым `containsItem()`.
  - Добавлен режим `autoFlush = false` с флагом `dirty` и методом `flush()`, чтобы не пересобирать весь `ListTag` при каждой операции в накопителе.
- [ ] **2.3. Вынос содержимого ячеек из NBT предмета в `SavedData` мира по UUID:**
  - Отложено (требует миграции существующих дисков в мирах игроков).
- [x] **2.4. Оптимизация `ByteStorageHandler`:**
  - `ItemKey` использует ссылку на `CompoundTag` при поиске без лишнего `.copy()`.
  - `markDrivesDirty(Set<IDriveHost>)` помечает только реально изменённые накопители.
  - `cachedTotalUsed` обновляется инкрементально без полного пересчёта.
  - `extractItem()` при наличии резервов проверяет кэшированный пул `getAllItems()` вместо двойного полного обхода дисков.
  - `extractRaw()` извлекает сначала из накопителей с **наименьшим** приоритетом.
- [x] **2.5. Защита от подгрузки чанков и событийное сканирование топологии (`NetworkControllerBlockEntity`):**
  - В `scanNetworkTopology()` добавлена проверка `level.isLoaded(neighborPos)` перед `level.getBlockEntity(neighborPos)`.
  - Частота фонового BFS снижена с 20 тиков до 100 тиков и добавлен флаг `topologyDirty` (`markTopologyDirty()`).
  - [ ] Полное удаление `CableBlockEntity` у прямых кабелей (отложено как крупный рефакторинг).

---

## 3. Геймплей и функциональные улучшения

- [x] **3.1. Новый блок: Шина хранения (Storage Bus):**
  - Реализованы [StorageBusBlock.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/StorageBusBlock.java), [StorageBusBlockEntity.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/entity/StorageBusBlockEntity.java), [StorageBusMenu.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/menu/StorageBusMenu.java), [StorageBusScreen.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/client/gui/StorageBusScreen.java), регистрация, рецепт, лут-таблица, модели, локализация (`en_us`/`ru_ru`/`uk_ua`) и текстура `storage_bus.png` в стиле `io_bus.png`.
  - Подключает соседние инвентари к виртуальному пулу сети как `IDriveHost` с поддержкой приоритета (`-999..+999`) и фильтра (Белый / Чёрный список).
- [x] **3.2. Улучшения для `IoBusBlockEntity` / `IoBusMenu` / `IoBusScreen`:**
  - Добавлены 4 слота под карты ускорения (`SpeedCardItem`), уменьшающие интервал цикла и увеличивающие объём переноса за цикл (с выпадением карт при ломании блока).
  - Добавлен переключатель **Белый / Чёрный список** (`Allow` / `Deny`) для режима импорта.
  - Добавлено **управление редстоуном** (`Always` / `High` / `Low`).
  - Добавлены переводы для `en_us`, `ru_ru`, `uk_ua`.
- [x] **3.3. Режим блокировки для `PatternProviderBlockEntity` (Шина шаблонов):**
  - Добавлена кнопка **Blocking Mode** (`Block: ON/OFF`) в `PatternProviderScreen` и `PatternProviderMenu`, сохранение в NBT и проверка `isMachineBusy()` перед подачей следующего крафта (игнорирующая ожидаемые результаты из `expected`).
  - Добавлены переводы для `en_us`, `ru_ru`, `uk_ua`.
- [x] **3.4. Инструменты для ячеек и хранения (частично):**
  - [x] Добавлены новые тиры байт-ячеек: **256k** (`cell_256k`, 262 144 байта) и **1M** (`cell_1m`, 1 048 576 байт) в `CellTier`, регистрация, рецепты, модели, локализация и текстуры `cell_256k.png` / `cell_1m.png` в стиле существующих ячеек.
  - [ ] Верстак форматирования ячеек (Cell Workbench) и карта уничтожения излишков (Void Overflow Card) — не реализованы.
- [x] **3.5. Сохранение `StatsHistory` (частично):**
  - [x] Серии графиков `StatsHistory` (`StatsTotals`, `StatsEnergy`) теперь сохраняются и восстанавливаются в NBT контроллера (`NetworkControllerBlockEntity`) и покрыты тестом `StatsHistoryTest.restoreSeriesRoundTrip`.
  - [ ] Дальность беспроводного терминала и Терминал безопасности — не реализованы.

---

## 4. Аддон `scripting`

- [x] **4.1. Модульные тесты для `scripting`:**
  - В `scripting/build.gradle` подключён JUnit 5, добавлен `SandboxTest` (проверка нормального выполнения, лимита инструкций, лимита времени и блокировки доступа к Java через `ClassShutter`).
- [x] **4.2. Усиление защиты песочницы:**
  - В `SandboxFactory` снижен `instructionObserverThreshold` с `1000` до `250` для более быстрой остановки тяжёлых циклов.
  - [ ] Ограничение на размер создаваемых строк/массивов внутри Rhino — не реализовано.

---

## 5. Инженерное качество, тесты и сборка

- [ ] Расширение покрытия юнит-тестами (`CraftingService`, `GridNetwork`, `ByteCellInventory` с мок/лёгкими обёртками).
- [ ] Обновление `build.gradle` для устранения предупреждений о совместимости с Gradle 9.0.
- [ ] Унификация языка Javadoc и комментариев в коде.
