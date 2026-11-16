# Идеи геймплейных нововведений для Simple Byte Storage

Документ содержит концепты новых игровых механик, блоков и предметов, развивающих ключевую идею **Simple Byte Storage** — хранение по правилу **«1 предмет = 1 байт» без лимита на типы предметов**.

---

## 1. [x] Байт-компрессия и «Архиватор» (Byte Compression)
> *Статус: реализованы [ArchiverBlock.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/ArchiverBlock.java), [ArchiverBlockEntity.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/entity/ArchiverBlockEntity.java), [DataBlobItem.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/item/DataBlobItem.java) (сжатие 64:1 при запасе >= 4096 шт., расход 160 FE, распаковка ПКМ), рецепты, модели, локализация и текстуры `archiver.png` / `data_blob.png`.*

### Суть механики
В текущей системе булыжник, земля и алмазный меч занимают одинаково **1 байт**, что делает хранение миллионов однотипных ресурсов дорогим по ячейкам. **Блок «Архиватор» (Byte Archiver)** сжимает однородные массивы предметов в плотные «архивы».

### Как это работает в игре
- Подключается к сети через кабель.
- Игрок задаёт до 9 предметов (слоты-привиды) или включает авто-режим «сжимать всё, чего > 4096 шт.».
- Архиватор потребляет FE и превращает пачки одинаковых предметов в виртуальные/физические **Сжатые блоки данных (Data Blob)**:
  - **1 уровень сжатия (8:1):** 64 предмета занимают 8 байт (или 1 компактный предмет-архив `8x`, `64x`, `512x`), но **требуют 10 FE/предмет при распаковке** в терминале или автокрафте.
  - **Баланс:** нельзя сжимать предметы с уникальным NBT (броня, инструменты), только стакающиеся ресурсы.
- **Синергия:** отлично сочетается с `Storage Bus` и фермами (булыжник, руда, эссенция).

---

## 2. [x] Квантовый мост и Межпространственные ретрансляторы (Quantum Link)
> *Статус: реализованы [QuantumRelayBlock.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/QuantumRelayBlock.java), [QuantumRelayBlockEntity.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/entity/QuantumRelayBlockEntity.java), [EntangledCrystalItem.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/item/EntangledCrystalItem.java), интеграция в `NetworkControllerBlockEntity.scanNetworkTopology()` (межпространственный BFS), рецепты, модели, локализация и текстуры `quantum_relay.png` / `entangled_crystal.png`.*

### Суть механики
Сейчас вся сеть должна быть соединена физическими кабелями впритул, а `NetworkControllerBlockEntity` работает в пределах одного связного графа. **Квантовый ретранслятор (Quantum Relay)** позволяет соединять удалённые базы и разные измерения (Незер, Энд) в единую сеть.

### Как это работает в игре
- **Блок `Quantum Relay` + предмет `Entangled Crystal` (Запутанный кристалл):**
  - Вставьте два кристалла в два разных ретранслятора и свяжите их на **Кодировщике** (или Shift+ПКМ одним кристаллом по другому).
  - Один ретранслятор подключается к основной сети с Контроллером, второй — на удалённой ферме или в другом измерении.
- **Баланс и стоимость:**
  - Постоянное потребление энергии: **50 FE/т** в одном измерении, **250 FE/т** между измерениями (требуется `Capacitor` для сглаживания пиков).
  - Ограничение пропускной способности: передача до `N` байт/тик (увеличивается картами `SpeedCardItem`).
- **Визуал и звук:** пульсирующее циановое ядро (`#00f0ff`), переходящее в красный (`#dc2800`) при обрыве связи или нехватке энергии.

---

## 3. [x] Жидкостные и Энергетические байт-ячейки (Fluid & Energy Bytes)
> *Статус: реализованы [FluidCellItem.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/item/FluidCellItem.java) (`fluid_cell_1k..64k`, 1000 mB = 1 байт без лимита на типы жидкостей, установка в `DriveBlockEntity`), [FluidIoBusBlock.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/FluidIoBusBlock.java), [FluidIoBusBlockEntity.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/entity/FluidIoBusBlockEntity.java), рецепты, модели, локализация и текстуры `fluid_cell_*.png` / `fluid_io_bus.png`.*

### Суть механики
Расширение принципа **«1 единица = N байт»** на жидкости (Forge Fluid) и энергию, чтобы игрок мог хранить воду, лаву, нефть и жидкие металлы в тех же накопителях `Drive`.

### Детали реализации и баланса
- **Жидкостная байт-ячейка (`fluid_cell_1k` .. `fluid_cell_1m`):**
  - Правило: **1 ведро (1000 mB) = 1 байт** (или **250 mB = 1 байт** для более точного учёта). Без лимита на типы жидкостей!
  - Использует те же накопители `DriveBlockEntity` и отображается в отдельном фильтре терминала (иконка капли).
- **Жидкостная I/O-шина и Шина хранения:** импорт/экспорт жидкостей в баки, трубы и машины других модов.
- **Жидкостный автокрафт:** `PatternEncodingTerminal` позволяет кодировать шаблоны с вёдрами/жидкостями, а `PatternProviderBlockEntity` подаёт жидкость в соседний механизм напрямую через `IFluidHandler`.

---

## 4. [x] Система «Умных карт» и Верстак форматирования (частично реализовано)
> *Статус:*
> - [x] **`Cell Workbench` (Верстак ячеек):** реализованы [CellWorkbenchBlock.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/CellWorkbenchBlock.java), [CellWorkbenchBlockEntity.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/entity/CellWorkbenchBlockEntity.java), [CellWorkbenchMenu.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/menu/CellWorkbenchMenu.java), [CellWorkbenchScreen.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/client/gui/CellWorkbenchScreen.java), индивидуальный фильтр `CellFilter`/`CellBlacklist` в `ByteCellInventory` и `ByteStorageHandler`, рецепт, модель, локализация и текстура `cell_workbench.png`.
> - [x] **Карта уничтожения излишков (`void_card` / `VoidCardItem`):** реализована с текстурой, рецептом, слотом улучшения в `DriveBlockEntity` / `DriveMenu` / `DriveScreen` и логикой в `ByteStorageHandler`.
> - [x] **Карта блокировки (`lock_card` / `LockCardItem`):** реализована (переводит `Drive` в режим «только чтение»).

### Суть механики
Повышение гибкости самих байт-ячеек, чтобы игрок мог настраивать поведение конкретной ячейки, а не только всего накопителя `Drive`.

### Новые предметы и блок
1. **Верстак ячеек (`Cell Workbench`):**
   - Позволяет записать в саму ячейку **белый/чёрный список** (до 18 слотов), **фильтр по моду** (`@thermal`, `@create`) или **фильтр по тегу** (`#forge:ores`).
   - Функция **«Перенос/Дефрагментация»**: перекладывает предметы из одной ячейки в другую или освобождает мелкие ячейки.
2. **Карта уничтожения излишков (`Void Overflow Card`):**
   - Вставляется в `Drive` или в слот улучшения ячейки.
   - Если ячейка (с настроенным фильтром) заполнена на 100%, новые предметы этого типа принимаются сетью и **уничтожаются**, вместо того чтобы забивать фермы и трубы.
3. **Карта «Только чтение» / «Заморозка» (`Lock Card`):**
   - Блокирует вставку новых предметов в ячейку, разрешая только извлечение (удобно перед апгрейдом или очисткой старого диска).

---

## 5. [x] Продвинутый автокрафт: Распределённые CPU и Сопроцессоры (частично)
> *Статус:*
> - [x] **Блок `Crafting Co-Processor`:** реализованы [CraftingCoprocessorBlock.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/CraftingCoprocessorBlock.java), [CraftingCoprocessorBlockEntity.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/entity/CraftingCoprocessorBlockEntity.java), ускорение крафта в `AssemblerBlockEntity`, рецепт, модель, локализация и текстура `crafting_coprocessor.png`.
> - [x] Древовидный предпросмотр заказа: добавлена иерархическая древовидная разметка (`├`, `└`, `▶`) и подсветка финального крафта в [CraftPlanView.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/client/gui/CraftPlanView.java).
> - [x] Балансировка нагрузки между `PatternProviderBlockEntity` уже работает в `CraftingService.enqueue`.

### Суть механики
Сейчас `AssemblerBlockEntity` имеет собственную очередь, а `PatternProviderBlockEntity` работает независимо. Добавление **Вычислительного узла (Crafting Node / Co-Processor)** превращает автокрафт в управляемую мультиблочную/сетевую систему.

### Как это работает в игре
- **Блок «Сопроцессор крафта» (`Crafting Co-Processor`):**
  - Каждый установленный в сети сопроцессор позволяет `Assembler` и `PatternProvider` запускать **дополнительные параллельные ветки подкрафтов** одного крупного заказа.
- **«Умное распределение» (Round-Robin) для `PatternProviderBlockEntity`:**
  - Если у игрока 4 печи с 4 Шинами шаблонов с одинаковым шаблоном (железная руда → слиток), заказ на 256 слитков автоматически делится поровну между всеми 4 печами (по 64 на каждую).
- **Предпросмотр дерева крафта (Crafting Tree):**
  - В окне заказа терминала можно переключиться в древовидный вид, чтобы видеть узкие места (какой ресурс задерживает весь заказ).

---

## 6. [x] События сети и «Аварийный протокол» (частично реализовано)
> *Статус:*
> - [x] **`Security Terminal` (Терминал безопасности):** реализованы [SecurityTerminalBlock.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/SecurityTerminalBlock.java), [SecurityTerminalBlockEntity.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/entity/SecurityTerminalBlockEntity.java), проверка доступа в `TerminalBlock`, рецепт, модель, локализация и текстура `security_terminal_front.png`.
> - [x] Режим энергосбережения сети (`Eco Mode`): реализован в [IGrid.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/api/network/IGrid.java), [GridNetwork.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/network/GridNetwork.java), [NetworkControllerBlockEntity.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/entity/NetworkControllerBlockEntity.java) (при заряде < 10% приостанавливает `IoBus`, `FluidIoBus`, `Archiver`, сохраняя доступ к терминалам).
> - [x] **Полевой сканер сети (`network_analyzer` / `NetworkAnalyzerItem`):** реализован с текстурой `network_analyzer.png`, рецептом, локализацией и диагностикой узла/сети при ПКМ.

### Суть механики
Сделать управление крупной сетью более тактическим и интерактивным, особенно в мультиплеере и сборках с техномодами.

### Новые элементы
1. **Терминал безопасности (`Security Terminal`):**
   - Привязывается к Контроллеру. Владелец сети добавляет игроков с правами:
     - *Просмотр и взятие предметов*
     - *Заказ автокрафта*
     - *Изменение шаблонов и правил запаса*
     - *Редактирование скриптов `LogicTerminal`*
   - Защищает `WirelessTerminalItem` от использования чужими игроками.
2. **Перегрузка сети и «Байт-дефрагментатор»:**
   - При резком падении энергии ниже 10% или при перегрузке (слишком много активных шин без `Capacitor`) сеть переходит в режим **Энергосбережения** (отключает I/O-шины и автокрафт, но сохраняет доступ к терминалам для извлечения ресурсов).
3. **Полевой сканер сети (`Network Analyzer`):**
   - Ручной инструмент: ПКМ по любому блоку сети показывает:
     - ID сети, её статус, текущее потребление FE/т каждого блока;
     - если блок отключён или застрял — подсвечивает проблемный кабель/машину контуром в мире на 5 секунд.

---

## 7. [x] Новые возможности для аддона `scripting` (частично реализовано)
> *Статус:*
> - [x] **Редстоун API (`redstone.get(dir)` / `redstone.set(dir, power)`):** реализовано в `LogicBusBlock`, `LogicBusBlockEntity`, `BusContext` и `Api`.
> - [x] **Персистентное хранилище (`store.get` / `store.set` / `store.remove` / `store.clear`):** реализовано с сохранением в NBT `LogicBusBlockEntity`.
> - [x] Монитор сети (`network_display` + `display.set(line, text)` / `display.clear()`): реализованы [NetworkDisplayBlock.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/NetworkDisplayBlock.java), [NetworkDisplayBlockEntity.java](file:///src/ByteStorage/src/main/java/com/simpleae/bytestorage/block/entity/NetworkDisplayBlockEntity.java), интеграция в `Api.java`, рецепт, модели, локализация и текстура `network_display_front.png`.

Чтобы **Логическая шина (`LogicBus`)** стала настоящим «мозгом» базы:
1. **Взаимодействие с редстоуном:** `side.redstone(dir)` / `side.setRedstone(dir, power)` — скрипты смогут читать и выдавать аналоговый сигнал (0–15), управляя реакторами, фермами и воротами без громоздких схем.
2. **Хранение состояния между перезагрузками (`memory` API):** небольшой персистентный объект `store.get(key)` / `store.set(key, val)` (лимит, например, 4 КБ JSON на шину), сохраняемый в NBT `LogicBusBlockEntity` — счётчики, статистика, режимы работы базы.
3. **Голографический монитор (`Network Display`):** декоративный плоский экран 1×1 или мультиблочный 2×2/3×2, на который `LogicBus` может выводить текст, прогресс-бары крафта, уровень энергии и список ресурсов прямо в мире без открытия GUI.

---

## 8. [x] Игровые испытания и достижения (Advancements)
> *Статус: реализовано в `data/simple_bytestorage/advancements/` (`root`, `first_byte`, `storage_network`, `factory_started`, `megabyte`) с переводами `en_us`, `ru_ru`, `uk_ua`.*

Добавление ветки достижений, обучающей игрока без необходимости постоянно открывать гайд-бук:
- **«Первый байт»** — скрафтить `cell_1k` и поместить её в `Drive`.
- **«Без ограничений»** — заполнить одну ячейку 50+ разными типами предметов.
- **«Завод запущен»** — выполнить первый автокрафт через `Assembler` или `Pattern Bus`.
- **«Мегабайт»** — достичь 1 048 576 байт суммарной ёмкости сети (`cell_1m`).
- **«Архитектор логики»** — запустить первый рабочий скрипт в `LogicBus`.
