# Changelog — Simple Byte Storage

All notable changes to the **Simple Byte Storage** mod will be documented in this file.

## [Unreleased]

### Changed — 1.2.2.33
- **Cable redrawn**: new steel-and-cyan texture (hub with a glowing core, thinner wire with light bands). Where a cable connects to a **block** (terminal, drive, bus, controller…) it now ends in a **plug**: a flange with a glowing cyan rim and a collar, so connections are easy to tell from cable-to-cable links. The plug is part of the cable's hitbox. New item icon.

## [1.2.1] — beta

New Controller design, restyled screens, statistics graphs, stock schedule, Capacitor, script code editor and events. Full notes: `docs/RELEASE_NOTES_1.2.1.md`.

### Added — 1.2.1.32
- **Script code editor** in the Logic Terminal (Scripting addon) replaces the plain text box: line numbers, **syntax highlighting** (keywords, strings, numbers, comments, functions, API objects, properties), mouse and keyboard selection, copy/cut/paste, undo/redo (Ctrl+Z / Ctrl+Y), auto-indent, auto-closing brackets and quotes, Tab / Shift+Tab block indent, Ctrl+/ to comment lines.
- **Autocomplete**: a popup suggests API objects and methods (`net.`, `stock.`, `recipes.`, `journal.`, port methods) and keywords while you type; inside quotes it suggests item ids. Up/Down to choose, Enter/Tab or click to accept, Esc to dismiss, Ctrl+Space to force it.
- New unit tests `CodeEditorLogicTest` for the editor text model, lexer and completion engine.

### Changed — 1.2.1.31
- **All screens restyled** like the Controller: every window header now has an icon for its block, a status LED and a **clickable close cross** (ESC still works); the "Inventory" and section labels have icons; buttons can carry icons (Pattern Bus record, I/O Bus mode, Encode, Assembler cancel). The Terminal's deposit button moved left to make room in the header. New icons: code, search, arrows, play, check, plus. Also applies to the Scripting addon windows.

### Changed — 1.2.1.30
- **Controller screen redesigned** after the design reference: tabs with icons in the left column, a header with a network icon and a close cross, an Overview with a status/energy panel and a network-contents panel (items stored, charge with estimated time left), highlighted Recipe Library slot when active.
- **Fixed overlapping**: the Wireless Terminal and Recipe Library slots no longer collide with the stock-schedule button and the graphs. The graphs moved to a new **Graphs** tab; the tab content area was moved up and compacted.

### Added — 1.2.1.29
- **Capacitor** block: a reserve energy store (500,000 FE) on the network. Refills the Controller below 50% (to 60%), absorbs surplus above 90% (to 85%), accepts energy from adjacent generators, right-click shows the charge. New unit tests `CapacitorLogicTest`.

### Changed — 1.2.1.28
- **Controller screen layout**: the tabs moved from the top into a column on the left (guidebook style); the content, all slots and the player inventory are shifted right and the window is wider.

### Added — 1.2.1.27
- **Saved searches**: three slots under the terminals' sort buttons store search queries (click to apply, empty slot saves the current search, right-click overwrites/clears). Saved in `config/simple_bytestorage_searches.txt`.

### Added — 1.2.1.26
- **Statistics graphs** on the Controller's Overview tab: items stored and energy level over the last 20 minutes (sample every 20 s), plus the three items that rose and fell the most. History is not saved across reloads. New unit tests `StatsHistoryTest`.

### Added — 1.2.1.25
- **Stock rule schedule**: a button on the Controller's Stock tab picks when rules place orders — always, night only, day only, or only when network energy is high (resume >50%, pause <25%). Saved with the Controller. Scripts: `stock.mode()` / `stock.setMode(name)`. New unit tests `StockScheduleTest`.

### Fixed — 1.2.1.24
- Build fix for `NetworkControl.MachineInfo` (record component name clashed with `Object.wait`).

### Added — 1.2.1.23
- **Scripts see machines**: `net.machines()` lists Assemblers and Pattern Buses with busy/pending/wait reason/current output (up to 64). Core API `NetworkControl.machines`.

### Added — 1.2.1.22
- **"Max" button in the order dialog**: when an order is short of ingredients, **Start** turns into **Max: N** (the largest amount that can be crafted now); one click recalculates the plan. Works in the Terminal, Crafting Terminal and Pattern Terminal. The plan packet changed (network protocol is still 10 since 1.2.1.19, which was never released).

### Added — 1.2.1.21
- **Stuck Pattern Buses**: a bus that waits for ingredients for 30 s, or has no machine accepting them for 60 s, is reported like a stuck Assembler (journal "Stuck", chat warning, `onAlert` in scripts).

### Added — 1.2.1.20
- **Scripting events**: optional `onCraft(e)`, `onLowStock(e)`, `onAlert(e)` handlers instead of polling in `tick()`. A script needs `tick()` or at least one handler. Journal events now carry a sequence number (unit tests added) and the core API has `NetworkControl.eventCursor/eventsSince`.

### Added — 1.2.1.19
- **Terminal tooltips** show stored / reserved for orders / in production for the hovered item (Terminal and Crafting Terminal). New packet; network protocol is now 10.

### Added — 1.2.1.18
- **Reservations survive world reloads**: reserved raw materials are saved with the Controller and restored on the first tick after loading. New unit test for the ledger snapshot.

## [1.2.0]

Journal tab, resource reservation, machine power from the network, shortage warnings, and a much larger scripting API. Full notes: `docs/RELEASE_NOTES_1.2.0.md`.

### Added — 1.1.4.17 (Scripting + core API)
- **Аддон скриптів оновлено під нові можливості**: `net.available/reserved/totalReserved`, `net.energy`, `net.stats`, планування `net.canCraft/why/plan`, керування чергою `net.queue/setPriority/pause/resume/cancel`, правила запасу `stock.list/set/clear`, налаштування рецептів `recipes.*`, журнал `journal.write/warn/read` (записи скриптів видно у вкладці «Журнал» контролера), чат-попередження `alert`. Нові опції конфігу `allowControl` і `allowAlerts`; нові ліміти на запуск (4 записи в журнал, 1 alert).
- Ядро: стабільний API `com.simpleae.bytestorage.api.NetworkControl` для аддонів; журнал підтримує записи з текстом.

### Added — 1.1.4.16
- **Попередження про нестачу**: у правила запасу (вкладка «Запас») причина збою показується у підказці над червоним `!` (немає шаблону, бракує інгредієнта з кількістю, немає машини, черги повні). Після 3 невдалих спроб поспіль — повідомлення в чат. Асемблер, що понад 30 с чекає інгредієнти чи місце в сховищі, потрапляє в журнал («Застрягло») і в чат. Отримують гравці з бездротовим терміналом, прив'язаним до цього контролера; не частіше разу на 5 хв на причину.

### Added — 1.1.4.15
- **Резерв ресурсів під замовлення**: сировина, яку замовлення бере зі сховища, бронюється. Термінали, I/O-шини і бездротовий термінал не можуть її забрати; нові замовлення плануються лише з вільного залишку; Асемблери і Шини шаблонів беруть з резерву, і він зменшується. Скасування знімає залишок, при порожній черзі резерви очищаються. Не зберігається після перезавантаження світу. Нові автотести `ReservationLedgerTest`.

### Added — 1.1.4.14
- **Живлення машин від мережі**: Шина шаблонів тепер заряджає сусідні машини з енергобуфером (електропіч тощо) енергією мережі. Новий блок **Енергоінтерфейс** (кабель + редстоун + золотий злиток): підключається кабелем до мережі й заряджає сусідні машини. До 1000 FE/т на пристрій; мережа залишає собі запас, щоб машини її не знеструмили.

### Fixed — 1.1.4.13
- Вкладка «Рецепти» контролера: зникла зайва колонка рамок слотів (рамки слотів вкладки «Запас» малювалися і тут, ліворуч над чорним списком).

### Added — 1.1.4.12
- Контролер: вкладка **«Журнал»** — останні події автокрафту (замовлення, завершені крафти, запуск на шинах, правила запасу, скасування) з часом і прокруткою; однотипні події зливаються. Вікно контролера стало ширшим (288), у російській «Хранилище» -> «Склад».

### Changed — 1.1.4.11
- Початок розробки 1.1.4 (наскрізна нумерація збірок).

## [1.1.3] — 2026-11-05
Об'єднує зміни 1.1.3.1–1.1.3.10 (нижче). Нотатки релізу: docs/RELEASE_NOTES_1.1.3.md.

### Changed — 1.1.3.10
- Нумерація збірок: четверта цифра тепер наскрізна й не скидається при зміні версії (після 1.1.3.9 -> 1.1.4.10).

### Added — 1.1.3.9
- Автотести (`./gradlew test`, у CI на кожен пуш): синтаксис пошуку, вибір замовлення за пріоритетом і формат ETA, мовні файли (однакові ключі, усі ключі з коду існують), узгодженість версій у чотирьох місцях, заборонені формулювання, валідність моделей. Чиста логіка винесена в `util/SearchSyntax` і `util/QueueOrder`.
- `docs/TESTING.md` — чекліст ручного тестування перед нерелізним (не beta) випуском.

### Added — 1.1.3.8
- **Режим навчання Шини шаблонів**: кнопка «Записати рецепт» — один раз вручну прогнати рецепт у сусідній машині, шина сама запише процесний шаблон (витрата → результат) у вільний слот.

### Added — 1.1.3.7
- Термінали: розширений пошук (`@мод`, `#тег`, `>N` `<N` `=N`, `$craftable`, `$stock`, `$fav`; кілька слів — усі мають підійти) і **вибране** (Alt + клік, золотий куточок, вибрані — першими у своїй групі; зберігається в config).

### Added — 1.1.3.6
- Контролер: вкладка **«Запас»** — до 8 правил «тримати не менше N предметів»; мережа сама замовляє нестачу автокрафтом (раз на ~3 с, без дублювання, якщо замовлення вже в черзі). Стан правила показується кольором.

### Added — 1.1.3.5
- Вкладка «Крафт» контролера: рядки черги з прогресом, оцінкою часу, пріоритетом (-9..+9), паузою і скасуванням замовлення. Машини (Асемблери і Шини шаблонів) беруть наступний крафт з найвищим пріоритетом серед не призупинених. Протокол каналу 9.

### Added — 1.1.3.4
- Контролер: вкладка **«Рецепти»** — чорний список рецептів Бібліотеки (за предметом-результатом), бажані варіанти для інгредієнтів-тегів і перемикач «пропускати розпакування» (блок -> 9 злитків за наявності зворотного рецепту; типово увімкнено). Налаштування зберігаються в контролері.

### Changed — 1.1.3.3
- У терміналах спочатку йдуть предмети, що є в мережі, а після них — ті, що можна лише скрафтити; замість кількості для них написано «Крафт» (сортування діє всередині кожної групи).

## [1.1.2] — 2026-11-05 (beta)
Об'єднує зміни 1.1.1.1–1.1.1.8. Нотатки релізу: docs/RELEASE_NOTES_1.1.2.md.

## [1.1.0] — 2026-11-04
Об'єднує зміни 1.0.1.1–1.0.1.7 (нижче). Нотатки релізу: docs/RELEASE_NOTES_1.1.0.md.

### Added — 1.0.1.7
- **Замовлення автокрафту з будь-якого терміналу** (звичайний, крафту): предмети, які мережа вміє крафтити, але яких немає в наявності, показуються затемненими — клік відкриває діалог замовлення; для наявних — Ctrl+клік або СКМ. Протокол каналу 6.
- **Сортування** в терміналах: за ім'ям / за кількістю, за зростанням / спаданням (дві кнопки праворуч від вікна).

### Changed — 1.0.1.6
- Кількість предметів у терміналах (звичайний, крафту) і буфері Збирача малюється меншим шрифтом у правому нижньому куті іконки й не виходить за слот.
- Термінал кодування: звичайні рецепти верстака записуються одразу в мережу без Шаблону; Шаблон потрібен лише для процесних крафтів.

### Fixed — 1.1.1.6
- Рецепти **Бібліотеки рецептів** тепер показуються в Терміналі шаблонів (окремий компактний пакет; × для них вимкнено). Протокол каналу 8.
- Вікно замовлення в звичайних терміналах тепер таке саме, як у Терміналі шаблонів: що буде скрафчено (з підкрафтами), чого бракує, кнопки «Почати / Скасувати»; кількість можна змінювати, план перераховується.

### Added — 1.1.1.2–1.1.1.4
- **Бібліотека рецептів** — дороге покращення Контролера: мережа знає ВСІ рецепти верстака гри без запису шаблонів (без неї — лише записані). Віртуальні шаблони `lib:<id>`, вибір варіантів інгредієнтів за наявністю й закріплення на час крафту.
- **Нові налаштування Бездротового терміналу** з вкладками (Покращення / Перенос / Макрос): 8 слотів карт, фільтр на 18 слотів.
- Нові карти: **страхування** (речі після смерті йдуть у мережу й повертаються при відродженні), **інструментів** (заміна зламаного), **макросу** (клавіша запускає Логічні шини за ID; API `WirelessMacros` для аддонів). Протокол каналу 7.

### Added — 1.0.1.3 / 1.0.1.4
- **Інтерфейс Контролера мережі** (ПКМ): вкладки «Огляд» / «Сховище» / «Крафт» зі статистикою (підключені блоки, енергія, завантаження дисків, найчисленніші предмети, шаблони, Збирачі, Шини, черга крафту). Протокол каналу 5.
- **Слот бездротового терміналу в Контролері**: термінал прив'язується до мережі й заряджається від контролера; Shift+ПКМ по Терміналу більше не прив'язує.
- **Покращення бездротового терміналу**: Shift+ПКМ відкриває налаштування; **Карта автообміну** — автопоповнення предметів у руках із мережі та автозбір підібраних предметів у мережу, фільтр на 9 слотів (білий/чорний список), 2 FE за предмет.
- Довідник: розділи «Інтерфейс контролера» та «Бездротовий термінал».

### Fixed — 1.0.1.2
- **Предмети зникали при вкладанні другого стака в термінал.** Причина: у NBT запису диска `Count` — байт, тож 128+ предметів читалися як «порожній» запис і губилися при наступному завантаженні диска. Тепер кількість береться лише з `ByteCount` (старі дані, де запис ще не перезаписано, відновлюються).
- **Шина шаблонів подавала в машину лише 16 крафтів.** Ліміт «в роботі» було 16 (і лише 4 подачі за цикл); тепер до 256, 16 подач за цикл, а реально подачу обмежує приймальна здатність машини.
- Гайд і вікі: «Pattern Provider» перейменовано на «Pattern Bus» (як у грі); сторінка вікі `Pattern-Bus`.

### Changed — 1.0.1.1 (гілка Development)
- Ідентифікатор шини тепер задається в **інтерфейсі самої Логічної шини** (ПКМ): поле, «Призначити»/«Прибрати», список ID мережі (клік копіює, Shift+клік призначає одразу). Логічний термінал лише показує той самий список. Протокол каналу аддона 3.
- Нова схема версій `основна.мінорна.патч.збірка` (1.0.1.1); розробка в гілці `Development`.
- Автоматичні релізи на кожну збірку прибрано: GitHub Release створюється лише вручну (workflow `Release`).

## [1.0.0] — 2026-11-03
Перший стабільний реліз: усе, що описано нижче (автокрафт, процесні шаблони, Шина шаблонів, I/O-шина, бездротовий термінал, JEI, довідник, аддон Scripting з ідентифікаторами шин). Назва мода — Simple Byte Storage.

### Changed — ідентифікатори Логічних шин (build 9)
- Кожній Логічній шині можна задати ідентифікатор (поле + кнопка «Призначити», праворуч — список усіх ID мережі). У скриптах: `side(dir, id)` (один ендпоінт для всіх шин з ID), `buses(id)`, `busId`. Протокол каналу аддона 2. Оновлено гайд-книгу (en/ru/uk) і вікі.

### Added — документація
- `docs/wiki/`: англомовна вікі для обох модів (інструкції, рецепти, API скриптів, 10 прикладів, адміністрування) та workflow `wiki.yml` для автопублікації в GitHub Wiki.

### Added — Книга-довідник (build 8)
- Предмет «Довідник Simple Byte Storage» (рецепт: книга + кабель): екран з розділами про всі можливості мода (en/ru/uk). Одна книга на обидва мода: аддон Scripting додає в неї розділи про скрипти через `GuideRegistry.register` — без аддона цих розділів немає.

### Added — аддон Simple Byte Storage: Scripting (build 7)
- Окремий необов'язковий мод у каталозі `scripting/` (той самий репозиторій, другий jar): Логічна шина + Логічний термінал, JavaScript через Rhino (вкладений лише в аддон), пісочниця з лімітами, серверний конфіг (enabled/opsOnly/ліміти). Див. `scripting/README.md`.
- Основний мод: `CraftingService.requestCraft(grid, stack, amount)` — стабільна точка входу для аддонів. CI збирає два jar.

### Added — інтеграція з JEI (build 6)
- Кнопка «+» у JEI переносить рецепт у Термінал Кодування Шаблонів: верстак → режим «Верстак», піч/плавильня/коптильня/каменерізка → режим «Процес» (входи + результат). Пакет `EncoderFillPacket`, протокол 4. JEI потрібен лише для компіляції (`compileOnly`), без нього мод працює як раніше.

### Added — підсвічування бракуючого
- У списку шаблонів рядок показує «Бракує: <предмет>», а тултіп шаблону позначає кожен інгредієнт ✓ (є) або ✗ (немає в мережі). Сервер шле побітову маску бракуючих входів замість одного прапорця.

### Added — бездротовий термінал
- Предмет `wireless_terminal`: Shift+ПКМ по Терміналу прив'язує, ПКМ у повітрі відкриває його на будь-якій відстані (той самий світ чи інший, поки чанк завантажено). Відкриття коштує 100 FE; ємність 20 000 FE, заряджається будь-яким зовнішнім зарядником або повторним Shift+кліком по прив'язаному Терміналу (енергія береться з контролера мережі).

### Added — I/O-шина (автодокладка)
- Новий блок `io_bus`: режим «Імпорт» забирає предмети із сусіднього інвентаря в мережу (білий список, порожній = усе), режим «Експорт» підтримує в сусідньому інвентарі задану кількість кожного предмета зі списку (напр. паливо для печі). До 64 предметів за цикл. Рецепт: кабель + лійка + редстоун.

### Added — пріоритети та білі списки накопичувачів
- Кожен Drive має пріоритет вставки (кнопки +/−, Shift = ±10) і білий список із 9 слотів-привидів (порожній = приймає все). Нові предмети йдуть спочатку у накопичувачі з вищим пріоритетом, що їх приймають; витягування не змінилось.

### Added — рецепти
- Рецепти крафту для всіх блоків та предметів мода (залізо, редстоун, скло, алмази), тож мод тепер доступний у виживанні: кабель, контролер, накопичувач, термінали, асемблер, шина шаблонів, чистий шаблон, комірки 1k–64k, карта прискорення.

### Added — карти прискорення
- Предмет `speed_card` і 4 слоти покращень в Асемблері: час крафту = 40 / (1 + карт) тіків (мінімум 4). Покращення зберігаються і випадають при руйнуванні блоку.

### Added — вкладка «Черга» у Терміналі Шаблонів
- Вкладки «Шаблони / Черга»: у черзі видно всі замовлення мережі (Асемблери + Шини), зведені за шаблоном, з позначкою «в роботі» та кнопкою скасування всіх замовлень шаблону.

### Added — вікно підтвердження замовлення
- Кнопка «Замовити» тепер спочатку показує план: які кроки (підкрафти) буде виконано і чого бракує. «Почати» доступна лише коли бракуючого немає. Esc закриває вікно. Новий пакет `CraftPlanPacket`, протокол 3.

### Added — підкрафти (ланцюжки)
- Замовлення автоматично розкладається на підкрафти: якщо бракує проміжного предмета, для якого є шаблон (глобальний або в Шині), він замовляється першим (до 8 рівнів, глибоко-першими). `CraftingService` — планувальник і розподіл між Асемблерами та Шинами. Якщо щось не можна ні взяти, ні скрафтити — замовлення відхиляється з повідомленням.

### Added (build 5) — процесні шаблони, Шина Шаблонів і кабелі
- **Два типи шаблонів.** Звичайний крафт верстака кодується як раніше і потрапляє в глобальний список мережі (Асемблер). Для машин (піч, дробарка з інших модів тощо) у Терміналі Кодування є перемикач режиму «Верстак / Процес»: у процесному режимі сітка — це входи з кількостями (ЛКМ — стак з курсору, ПКМ — +1), слот результату задається вручну. Такий шаблон **не** додається в глобальний список — його кладуть у Шину Шаблонів.
- **шина шаблонів** (`pattern_provider`): 9 слотів для процесних шаблонів. Ставиться впритул до машини; за замовленням з Терміналу Шаблонів бере інгредієнти з мережі, подає їх у машину і забирає готовий результат назад у мережу (черга зберігається в світі, до 16 крафтів «в роботі»).
- **кабель** (`cable`): провід для мережі з візуальними рукавами до з'єднаних нод.
- Термінал Шаблонів показує і процесні шаблони з усіх Шин мережі (мітка [Машина]); замовлення ділиться між шинами. Видалити такий шаблон — дістати його з Шини.

### Reworked (build 4) — єдиний стиль GUI
- Усі шість екранів (Drive, Terminal, Crafting Terminal, Assembler, Pattern Terminal, Pattern Encoding Terminal) перероблено у єдиний темний «hardware» стиль: шліфований метал із фасками, смуга заголовка з бірюзовою лінією та світлодіодом статусу, заглиблені панелі, бірюзовий акцент для активних елементів; зелений/помаранчевий/червоний — лише для статусів.
- `GuiStyle` — єдине джерело палітри та примітивів (вікно, панель, слот, прогрес, індикатор пам'яті, смуга прокрутки); `UiButton` — єдина кнопка (клік обробляється до `super.mouseClicked`).
- Термінали: індикатор заповнення пам'яті замість тексту, поле пошуку у стилі «дисплея», підказка стану; тексти — через lang-файли.
- Крафт-термінал: сітка і результат зсунуті під смугу заголовка.

### Reworked (build 3) — автокрафт: Асемблер, Термінал Шаблонів, Термінал Кодування
- **Шаблон** тепер містить розкладку сітки 3×3 + результат (`IPatternStorage.PatternData`), тому форменні рецепти (кирки, сходи тощо) крафтяться правильно. Ідентичні рецепти не дублюються (ключ = результат + хеш рецепту).
- **Збереження**: глобальний список шаблонів записується в NBT контролера мережі і більше не зникає після перезапуску.
- **Термінал Кодування**: слоти сітки — «привиди» (предмети не витрачаються), результат рахується автоматично за ванільними рецептами; кнопка «Записати» (`clickMenuButton`), кнопка очищення сітки, shift-клік, шаблон можна перезаписати. Закодований шаблон одразу потрапляє в глобальний список. Повідомлення про помилки в action bar.
- **Термінал Шаблонів**: сервер сам синхронізує список (без запитів з клієнта), індикатор «ресурсів достатньо», фільтр «Усі/Готові», пошук за назвою результату, замовлення ЛКМ=1 / Shift=10 / Ctrl=64 крафтів (розподіляється між асемблерами), Shift+ЛКМ по «×» — видалити шаблон.
- **Асемблер**: черга замовлень; з мережі в карточку-буфер копіюються всі інгредієнти → крафт (ванільний рецепт, залишки на кшталт відер теж) → результат лягає в ту ж карточку → вміст повертається в основну мережу. Статус очікування (немає картки / ресурсів / місця), кнопка «Скасувати», стан зберігається в світі.
- **GUI**: усі три екрани викликають `super.render` (з'явились предмет на курсорі, тултіпи слотів, drag), додано повний інвентар гравця + хотбар, рамки слотів автоматично збігаються з позиціями слотів меню, введення в пошуку більше не закривається клавішею «E». Тексти — через lang-файли (ru/en/uk).
- Прибрані пакети `PatternRequest/Refresh/Select/Encode`; додано `PatternOrderPacket`; версію протоколу піднято до 2.

### Fixed
- Ім'я шаблону перевищувало ліміт 32 символи в пакеті синхронізації (клієнт отримував виняток) — ключі до 128 символів.
- `ContainerData` Асемблера на клієнті ніколи не оновлювався (прогрес не відображався).
- Зміни вмісту дисків через мережу (`ByteStorageHandler`) не позначали накопичувач «брудним» — дані могли не потрапити на диск.
- Асемблер/Термінал Кодування не повертали предмети при руйнуванні блоку; Асемблер більше не віддає внутрішній інвентар воронкам.
- Додано loot-таблиці та тег `mineable/pickaxe` для блоків мода.

### Changed
- **GUI за новою схемою** — Асемблер, Глобальний Термінал Шаблонів і Термінал Кодування Шаблонів перезібрані під надані макети:
  - **Асемблер:** картка сховища → стрічка прогресу → слот результату; окремий ряд із 8 слотів внутрішнього буфера; інвентар гравця 2×9.
  - **Термінал Шаблонів:** пошук рецептів, кнопка фільтра, список із іконкою/назвою/кнопкою «Замовити», прокрутка та статус мережі з байтовою пам'яттю.
  - **Термінал Кодування:** сітка 3×3 і результат угорі, чистий і закодований шаблони внизу, з'єднувальні лінії та кнопка «Записати»; інвентар гравця 1×9.
- **Автокрафт** — асемблер отримав серверний тікер, синхронізацію прогресу через `ContainerData`; замовлення шаблону з глобального термінала передається всім асемблерам мережі, а результат не показується в Output до завершення крафту.
- **Мережа** — `SyncPatternDataPacket` тепер також передає вибраний шаблон і статистику `used / capacity` байтів.

### Fixed
- **GUI (Assembler Screen / Assembler Menu)** — виправлено «кривий» інтерфейс Асемблера (`AssemblerScreen` / `AssemblerMenu`):
  - Слот буферної карточки сховища (Storage Cell) накладався на слоти крафтової сітки 3×3 (буфер розміщено за межами сітки — слот `(5, 53)`, сітка зміщена праворуч, cols `x=25..`).
  - Поле вводу назви шаблону (`EditBox`) та кнопка «Старт Крафту» перетинали зону крафтової сітки / слота результату — перенесено в верхній правий квадрат (`EditBox` `x+92, y+2`, кнопка `x+92, y+14`), сітка залишається у лівій частині.
  - Підпис «Буфер» та статус «Крафт в процесі...» / «Очікування шаблону» перенесено в місця без перекриттів зі слотами та сіткою.
- **GUI (Pattern Terminal Screen)** — виправлено накладання тексту і слотів у інтерфейсі Терміналу Шаблонів (`PatternTerminalScreen`):
  - Список шаблонів тепер має чітку ліву панель із тлію (`#20232A`) та окремі слоти для кожного шаблону (висота 22, відступ 2px).
  - Текст назви шаблону та кількість інгредієнтів не накладаються на іконку результату.
  - Додано проріжну смужку прокрутки справа від списку.
  - Кнопки «Вибрати для Крафту» та «Оновити» винесені на правий бік, не закривають список шаблонів.
  - Додано можливість клікнути по рядку шаблону у списку для його вибору.
- **GUI (Pattern Encoding Terminal Screen)** — виправлені накладання у інтерфейсі Терміналу Кодування Шаблонів (`PatternEncodingTerminalScreen`):
  - Висота вікна збільшена з 194 до 222, щоб вмістити усі елементи без накладання.
  - Слот чистого шаблону, сітка інгредієнтів 3×3, слот результату та слот виходу запрограмованого шаблону розміщені з чіткими відступами.
  - Текст підпису («Чистий шаблон», «Інгредієнти», «Результат», «Шаблон (вихід)») не накладається на скели.
  - Кнопка «Закодувати Шаблон» розміщена нижче сітки інгредієнтів.
- **Асемблер (логіка автокрафту)** — виправлено роботу системи автокрафту (`AssemblerBlockEntity`):
  - Інгредієнти під час замовлення крафту тимчасово поміщаються у буферну карточку сховища (Storage Cell), а не просто в сітку.
  - Після завершення (успішного/неуспішного крафту) буфер очищається, стан `isCrafting` коректно скидається.
- **GUI (Crafting Terminal Screen)** — исправлен «кривой» интерфейс терминала крафта (`CraftingTerminalScreen`):
  - Поле поиска (`EditBox`) накладывалось на край крафтовой сетки 3×3 (перекрывало правую колонку и нижнюю строку матрицы). Перенесено в правый верхний угол окна (`x+88, y+6`), как у обычного терминала (`TerminalScreen`).
  - Статистика байтов (`used / max`) рисовалась с ошибочным удвоенным смещением `topPos` и наезжала на первую строку инвентаря игрока (`y+148`). Теперь выводится в той же строке, что и заголовок инвентаря, выровнена по правому краю.
  - Заголовок инвентаря (`inventoryLabelY`) приведён к единому уровню со статистикой (`y=134`).
  - Координаты отрисовки крафтовой сетки, слота результата и слотов инвентаря приведены к точному совпадению с позициями слотов в `CraftingTerminalMenu`.
- **Текстура блока (`crafting_terminal_front.png`)** — текстура терминала крафта не соответствовала стилистике мода (жёлто-янтарная сетка в стиле верстака не сочеталась с тёмным металлом остальных блоков). Переработана в единую палитру мода:
  - корпус — `#36393e` / `#1c1e26` (шлифованный титан/сталь, как у `machine_casing`);
  - экран — `#102438` (тёмный неон, как у обычного терминала);
  - декоративная 3×3 сетка крафта — циан `#00f0ff`;
  - индикатор состояния — оранжевый `#ff8c00` / `#c86400`;
  - рамка и фаски — `#20232a`, `#4b5360`, `#505866`, `#6e7887` (как у терминала, привода и контроллера).

- **GUI (Pattern Terminal Screen)** — виправлені накладання тексту і слотів у інтерфейсі Терміналу Шаблонів (`PatternTerminalScreen`):
  - Список шаблонів тепер має чітку ліву панель із тлію (`#20232A`) та окремі слоти для кожного шаблону (висота 22, відступ 2px).
  - Текст назви шаблону та кількість інгредієнтів не накладаються на іконку результату.
  - Додано проріжну смужку прокрутки справа від списку.
  - Кнопки «Вибрати для Крафту» та «Оновити» винесені на правий бік, не закривають список шаблонів.
- **GUI (Pattern Encoding Terminal Screen)** — виправлені накладання у інтерфейсі Терміналу Кодування Шаблонів (`PatternEncodingTerminalScreen`):
  - Висота вікна збільшена з 194 до 222, щоб вмістити усі елементи без накладання.
  - Слот чистого шаблону, сітка інгредієнтів 3×3, слот результату та слот виходу запрограмованого шаблону розміщені з чіткими відступами.
  - Текст підпису («Чистий шаблон», «Інгредієнти», «Результат», «Шаблон (вихід)») не накладається на скели.
  - Кнопка «Закодувати Шаблон» розміщена нижче сітки інгредієнтів.
- **Текстура предмета шаблону (`pattern_item.png`)** — додана текстура предмета-шаблону крафту у стилі мода.
- **Модель предмета шаблону (`pattern.json`)** — виправлено посилання з `block/cube` на `item/generated`.
- **GUI (Unified Frame)** — винесено узгоджену відривання рамки вікна в спільний хелпер `DriveScreen.renderFrame(...)`:
  - Тепер всі екрани моду (`TerminalScreen`, `DriveScreen`, `CraftingTerminalScreen`, `PatternTerminalScreen`, `PatternEncodingTerminalScreen`, `AssemblerScreen`) використовують один і той самий метод побудови рамки з фасками — дублювання закінчилось.
  - Інтерфейси блоків примінють однаковий дизайн: білий фон (`#C6C6C6`), світла фасок (верх/ліво `#FFFFFF`), темна фасок (низ/право `#555555`), 1px — що робить всі екрани Асемблера, Термінала, Драйва, Термінала Крафту та Кодування Шаблонів узгодженими.
- **Версіонування** — додано номер зборки у версію мода (`1.0.0+build.N`):
  - `build.gradle` та `META-INF/mods.toml` тепер використовують повну версію `1.0.0+build.1`.
  - Після кожної нової збірки змінюйте номер збірки (build.N) у цих двох файлах — так ви зможете відстежити, яка саме збірка за версією.

### Added
- **Система автокрафту (Autocrafting System)** — три новые компоненти для автоматизованого крафту в мережі:
  - **Асемблер (`AssemblerBlock` / `AssemblerBlockEntity`)** — блок крафту, який забирає інгредієнти з віртуального пулу мережі за шаблоном, тимчасно розміщує їх у буферній картці сховища (Storage Cell), виконує крафт за ванильною рецептю та відправляє результат назад у мережу. Має слот буферної картки, крафтову сітку 3×3 та слот результату.
  - **Термінал Шаблонів (`PatternTerminalBlock` / `PatternTerminalBlockEntity`)** — єдиний глобальний блок на всю мережу, який зберігає усі доступні шаблони крафту. Дозволяє переглядати список шаблонів та обирати їх для автокрафту.
  - **Термінал Кодування Шаблонів (`PatternEncodingTerminalBlock` / `PatternEncodingTerminalBlockEntity`)** — інтерфейс для запису рецептів крафту на чисті предмети-шаблони (Pattern Item). Гравець вставляє чистий шаблон, вказує інгредієнти та результат, і термінал записує рецепт у шаблон.
  - **Предмет Шаблону Крафту (`PatternItem`)** — предмет-шаблон, що може бути «чистим» або «програмованим». Програмований шаблон містить збережений рецепт (вихідний предмет + інгредієнти) у NBT.
  - **Сховище шаблонів мережі (`IPatternStorage` / `PatternStorage`)** — централізоване сховище шаблонів на рівні мережі (`IGrid.getPatternStorage()`). Доступне з усіх асемблерів мережі.
  - **Мережеві пакети** — `SyncPatternDataPacket` (S2C), `PatternSelectPacket` (C2S), `PatternRefreshPacket` (C2S) для синхронізації шаблонів та керування автокрафтом.
  - **Локалізація** — нові блоки та предмети додано до `en_us.json` та `ru_ru.json`.
  - **Ресурси** — блокстейти, моделі та текстури для асемблера, терміналу шаблонів, терміналу кодування та предмета шаблону у стилі мода.

## [1.0.0] - 2026-10-29

### Added
- **Core Architecture & Logic**:
  - Implemented `INetworkStorage` byte-based inventory logic where 1 item = 1 byte (`ByteStorageHandler`).
  - Added support for unlimited item types per cell (`ByteCellInventory`).
  - Grid network topology system (`GridNetwork`, `IGridNode`, `GridState`) with FE energy handling.
- **Blocks & BlockEntities**:
  - `NetworkControllerBlock` and `NetworkControllerBlockEntity`: FE energy storage, grid manager, dynamic states (`ONLINE`, `OFFLINE`, `CONFLICT`) with light emissions.
  - `DriveBlock` and `DriveBlockEntity`: 10-slot storage cell rack.
  - `TerminalBlock` and `TerminalBlockEntity`: Terminal interface access point.
- **Items**:
  - `ByteStorageCellItem` for 1k, 4k, 16k, and 64k storage cells (`CellTier`).
  - Implemented dynamic durability bar representing byte usage on storage cells.
  - Informative tooltips showing byte breakdown and capacity.
- **Client & GUI**:
  - `TerminalScreen` with search bar (`EditBox`), item grid rendering, scroll support, and used/total byte metrics.
  - `DriveScreen` for managing inserted storage cells.
  - `CraftingTerminalScreen`: crafting GUI with 3x3 matrix, result slot, virtual pool and player inventory; crafting logic consumes ingredients from the pool and gives the result to the player.
- **Networking**:
  - Client-Server packet communication (`SyncTerminalDataPacket`, `TerminalActionPacket`, `SimpleByteStoragePackets`).
- **Crafting Terminal**:
  - `CraftingTerminalBlock` / `CraftingTerminalBlockEntity` (`crafting_terminal`): terminal with crafting interface (3x3 matrix + result slot).
  - `CraftingTerminalMenu`: server container with crafting matrix, result slot, player inventory and pool integration.
  - `CraftingTerminalScreen`: client GUI for crafting; ingredients are taken from the virtual pool.
  - Crafting execution: recipe lookup via `RecipeType.CRAFTING`, ingredient consumption from the network storage and item delivery to the player.
- **Visual Assets & Textures**:
  - High-detail 16x16 / 32-bit pixel-art textures for all blocks: `machine_casing.png`, `machine_top.png`, `machine_bottom.png`.
  - Dynamic front textures for controller: `controller_front_online.png`, `controller_front_offline.png`, `controller_front_conflict.png`.
  - Drive textures: `drive_front.png` with 10 bay levers/LEDs, `drive_side.png` heatsink fins.
  - Terminal textures: `terminal_front.png` with data screen and keypad.
  - Cell item textures: `cell_1k.png` (green), `cell_4k.png` (blue), `cell_16k.png` (purple), `cell_64k.png` (orange).
  - Blockstate definitions and block/item models for all blocks and items.
- **Localization**:
  - Full English (`en_us.json`) and Russian (`ru_ru.json`) translations.
