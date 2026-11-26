package com.simpleae.bytestorage.scripting.client;

import com.simpleae.bytestorage.client.gui.CodeEditor;
import com.simpleae.bytestorage.client.gui.GuiStyle;
import com.simpleae.bytestorage.client.gui.PixelIcons;
import com.simpleae.bytestorage.client.gui.UiButton;
import com.simpleae.bytestorage.scripting.SimpleAeScripting;
import com.simpleae.bytestorage.scripting.net.ScriptActionPacket;
import com.simpleae.bytestorage.scripting.net.ScriptStatePacket;
import com.simpleae.bytestorage.scripting.net.ScriptingPackets;
import com.simpleae.bytestorage.scripting.script.UiElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Логический терминал в стиле компактной IDE:
 * - Левая панель активности (Explorer / Code / Panel / Scripts / Libraries)
 * - Сворачиваемый проводник (Логические шины + ID сети)
 * - Верхняя панель действий (Save / Run / Interval / Templates / Reload / Fullscreen)
 * - Просторное рабочее пространство (редактор кода, интерактивная панель или онлайн-библиотека)
 * - Сворачиваемая консоль вывода/ошибок и нижняя строка состояния
 */
public class LogicTerminalScreen extends Screen {

    private static final int ACTIVITY_W = 22;
    private static final int SIDEBAR_W = 112;
    private static final int HEADER_H = 17;
    private static final int TOOLBAR_H = 20;
    private static final int STATUSBAR_H = 14;
    private static final int CONSOLE_OPEN_H = 54;
    private static final int CONSOLE_COLLAPSED_H = 14;

    private static final int VIEW_CODE = 0;
    private static final int VIEW_PANEL = 1;
    private static final int VIEW_SCRIPTS = 2;
    private static final int VIEW_LIBS = 3;

    private static final int BUS_ROW_H = 14;
    private static final int ID_ROW_H = 11;
    private static final int LIB_HEADER_H = 16;
    private static final int LIB_ROW_H = 28;

    private final BlockPos terminal;
    private int left;
    private int top;
    private int winW;
    private int winH;

    // Вычисляемая геометрия рабочей области
    private int sideX;
    private int mainX;
    private int mainW;
    private int workY;
    private int workH;
    private int consoleY;
    private int consoleH;
    private int toolbarButtonsLeftX;
    private int intervalCenterX;

    private CodeEditor editor;
    private final List<UiButton> buttons = new ArrayList<>();

    private ScriptStatePacket state;
    private BlockPos selected;
    private String lastSyncedScript = "";
    private int busScroll = 0;
    private int idScroll = 0;
    private int tickCounter = 0;
    private boolean autoSelected = false;
    private int templateIdx = 0;
    private long savedFlashUntil = 0L;

    // Состояние IDE
    private int activeView = VIEW_CODE;
    private boolean sidebarOpen = true;
    private boolean consoleOpen = true;

    private int panelScroll = 0;
    private int panelContentHeight = 0;

    private int libraryScroll = 0;
    private int libraryCatIdx = 0;
    private List<ScriptLibraryIndex.Entry> libraryEntries = null;
    private List<ScriptLibraryIndex.LibraryEntry> libraryModules = null;
    private String libraryError = null;
    private boolean libraryLoading = false;
    private String libraryStatus = null;

    private record Cell(int x, int y, int w, int h, UiElement element) {
    }

    public LogicTerminalScreen(BlockPos terminal) {
        super(Component.translatable("block.simple_bytestorage_scripting.logic_terminal"));
        this.terminal = terminal;
    }

    private void computeLayout() {
        this.winW = Math.max(360, Math.min(620, this.width - 20));
        this.winH = Math.max(220, Math.min(340, this.height - 16));
        this.left = (this.width - winW) / 2;
        this.top = (this.height - winH) / 2;

        this.sideX = left + 3 + ACTIVITY_W;
        int effSideW = sidebarOpen ? SIDEBAR_W : 0;
        this.mainX = sideX + effSideW + (sidebarOpen ? 2 : 1);
        this.mainW = (left + winW - 4) - mainX;

        this.workY = top + HEADER_H + TOOLBAR_H;
        boolean showConsole = (activeView == VIEW_CODE || activeView == VIEW_PANEL);
        this.consoleH = showConsole ? (consoleOpen ? CONSOLE_OPEN_H : CONSOLE_COLLAPSED_H) : 0;
        this.consoleY = top + winH - STATUSBAR_H - consoleH - 2;
        this.workH = Math.max(60, (showConsole ? consoleY - 2 : top + winH - STATUSBAR_H - 3) - workY);
    }

    @Override
    protected void init() {
        computeLayout();

        String previous = this.editor != null ? this.editor.getValue() : "";
        this.editor = new CodeEditor(this.font, mainX, workY, mainW, workH);
        this.editor.setCompletion(ApiCatalog.engine(), ApiCatalog.GLOBALS);
        this.editor.setMaxLength(state == null ? 8000 : Math.min(state.maxLength, 30000));
        this.editor.setValue(previous);
        this.addRenderableWidget(this.editor);
        syncEditorBoundsAndVisibility();
        rebuildToolbarButtons();

        if (state == null) {
            ScriptingPackets.CHANNEL.sendToServer(new ScriptActionPacket(terminal, ScriptActionPacket.LIST, null, 0, ""));
        }
    }

    private void syncEditorBoundsAndVisibility() {
        if (this.editor == null) return;
        this.editor.setX(mainX);
        this.editor.setY(workY);
        this.editor.setWidth(mainW);
        this.editor.setHeight(workH);
        boolean show = (activeView == VIEW_CODE);
        this.editor.visible = show;
        this.editor.active = show;
        if (!show) {
            this.editor.setFocused(false);
            this.setFocused(null);
        } else {
            this.setInitialFocus(this.editor);
        }
    }

    private void rebuildToolbarButtons() {
        buttons.clear();
        int ty = top + HEADER_H + 2;
        int rx = left + winW - 5;

        // Справа налево с динамической шириной под любую локализацию (EN/RU/UK):
        // [Save] [Run/Stop] [- 20t +] [Tpl] [Reload] [⛶]
        int fsW = 16;
        rx -= fsW;
        buttons.add(new UiButton(rx, ty, fsW, 15, () -> Component.empty(), UiButton.Icon.NONE, this::openFullscreen)
                .icon("arrows")
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.fullscreen.tip")));

        rx -= 2;
        int relW = Math.max(40, this.font.width(Component.translatable("gui.simple_bytestorage_scripting.reload")) + 10);
        rx -= relW;
        buttons.add(new UiButton(rx, ty, relW, 15,
                () -> Component.translatable("gui.simple_bytestorage_scripting.reload"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.LIST, 0, ""))
                .enabledWhen(() -> selected != null)
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.reload.tip")));

        rx -= 2;
        int tplW = 24;
        rx -= tplW;
        buttons.add(new UiButton(rx, ty, tplW, 15,
                () -> Component.literal("Tpl"), UiButton.Icon.NONE, this::applyNextTemplate)
                .enabledWhen(this::canAct)
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.ide.tpl_tip")));

        // Интервал: [-] 20t [+]
        rx -= 3;
        int stepW = 13;
        rx -= stepW;
        buttons.add(new UiButton(rx, ty, stepW, 15, () -> Component.literal("+"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.INTERVAL, currentInterval() + 10, ""))
                .enabledWhen(this::canAct)
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.ide.interval_tip")));
        int gapW = 24;
        this.intervalCenterX = rx - gapW / 2;
        rx -= gapW;
        rx -= stepW;
        buttons.add(new UiButton(rx, ty, stepW, 15, () -> Component.literal("-"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.INTERVAL, Math.max(1, currentInterval() - 10), ""))
                .enabledWhen(this::canAct)
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.ide.interval_tip")));

        rx -= 3;
        int runW = Math.max(36, Math.max(
                this.font.width(Component.translatable("gui.simple_bytestorage_scripting.run")),
                this.font.width(Component.translatable("gui.simple_bytestorage_scripting.stop"))) + 10);
        rx -= runW;
        buttons.add(new UiButton(rx, ty, runW, 15,
                () -> Component.translatable(isRunning()
                        ? "gui.simple_bytestorage_scripting.stop" : "gui.simple_bytestorage_scripting.run"),
                UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.ENABLE, isRunning() ? 0 : 1, ""))
                .enabledWhen(this::canAct)
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.run.tip")));

        rx -= 2;
        int saveW = Math.max(40, this.font.width(Component.translatable("gui.simple_bytestorage_scripting.save")) + 10);
        rx -= saveW;
        buttons.add(new UiButton(rx, ty, saveW, 15,
                () -> Component.translatable("gui.simple_bytestorage_scripting.save"), UiButton.Icon.NONE,
                this::saveCurrentScript)
                .enabledWhen(this::canAct).primary()
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.save.tip")));

        this.toolbarButtonsLeftX = rx;
    }

    private void relayout() {
        computeLayout();
        syncEditorBoundsAndVisibility();
        rebuildToolbarButtons();
    }

    // --- Состояние ---

    public void apply(ScriptStatePacket packet) {
        this.state = packet;
        this.selected = packet.selected;
        this.editor.setMaxLength(Math.min(packet.maxLength, 30000));
        if (packet.withScript) {
            this.lastSyncedScript = packet.script;
            this.editor.setValue(packet.script);
        }
        if (selected == null && !autoSelected && !packet.buses.isEmpty()) {
            autoSelected = true;
            selected = packet.buses.get(0).pos();
            send(ScriptActionPacket.LIST, 0, "");
        }
        ScriptStatePacket.BusInfo info = selectedInfo();
        if (info != null && info.status().startsWith("ERROR") && !consoleOpen) {
            consoleOpen = true;
            relayout();
        }
    }

    private Map<String, Integer> idCounts() {
        Map<String, Integer> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (state != null) {
            for (ScriptStatePacket.BusInfo info : state.buses) {
                if (!info.id().isEmpty()) {
                    map.merge(info.id(), 1, Integer::sum);
                }
            }
        }
        return map;
    }

    private ScriptStatePacket.BusInfo selectedInfo() {
        if (state == null || selected == null) {
            return null;
        }
        for (ScriptStatePacket.BusInfo info : state.buses) {
            if (info.pos().equals(selected)) {
                return info;
            }
        }
        return null;
    }

    private boolean canAct() {
        return state != null && state.modEnabled && state.canEdit && selected != null;
    }

    private boolean isRunning() {
        ScriptStatePacket.BusInfo info = selectedInfo();
        return info != null && info.enabled();
    }

    private boolean isDirty() {
        return this.editor != null && !this.editor.getValue().equals(lastSyncedScript);
    }

    private int currentInterval() {
        ScriptStatePacket.BusInfo info = selectedInfo();
        return info == null ? 20 : info.interval();
    }

    private String clampedText() {
        String text = editor.getValue();
        int max = state == null ? 8000 : Math.min(state.maxLength, 30000);
        return text.length() > max ? text.substring(0, max) : text;
    }

    private void saveCurrentScript() {
        if (!canAct()) return;
        String text = clampedText();
        lastSyncedScript = text;
        savedFlashUntil = System.currentTimeMillis() + 1800;
        send(ScriptActionPacket.SAVE, 0, text);
    }

    private void selectView(int view) {
        this.activeView = view;
        this.libraryScroll = 0;
        relayout();
        if ((view == VIEW_SCRIPTS || view == VIEW_LIBS)
                && state != null && state.allowOnlineLibrary
                && libraryEntries == null && !libraryLoading) {
            loadOnlineLibrary(false);
        }
    }

    private void loadOnlineLibrary(boolean force) {
        if (state == null || !state.allowOnlineLibrary) return;
        this.libraryLoading = true;
        this.libraryError = null;
        this.libraryStatus = null;
        OnlineScriptLibrary.fetchIndex(state.onlineLibraryUrl, force,
                data -> {
                    this.libraryLoading = false;
                    this.libraryEntries = data.scripts();
                    this.libraryModules = data.libraries();
                },
                err -> {
                    this.libraryLoading = false;
                    this.libraryError = err;
                });
    }

    private List<UiElement> panelElements() {
        return state == null ? List.of() : state.panel;
    }

    private String currentCategory() {
        return ScriptLibraryIndex.CATEGORIES.get(libraryCatIdx % ScriptLibraryIndex.CATEGORIES.size());
    }

    private List<ScriptLibraryIndex.Entry> filteredScripts() {
        return ScriptLibraryIndex.filterByCategory(libraryEntries, currentCategory());
    }

    private List<ScriptLibraryIndex.LibraryEntry> filteredLibs() {
        return ScriptLibraryIndex.filterLibrariesByCategory(libraryModules, currentCategory());
    }

    private int libraryVisibleRows() {
        return Math.max(1, (workH - LIB_HEADER_H - 12) / LIB_ROW_H);
    }

    private static String jsAliasForLibrary(String libId) {
        int slash = libId.lastIndexOf('/');
        String raw = slash >= 0 ? libId.substring(slash + 1) : libId;
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String alias = sb.toString();
        if (alias.isEmpty() || Character.isDigit(alias.charAt(0))) {
            alias = "lib_" + alias;
        }
        return alias;
    }

    private void insertLibraryInclude(ScriptLibraryIndex.LibraryEntry lib) {
        String existing = this.editor.getValue();
        String needle = "include(\"" + lib.id() + "\")";
        if (!existing.contains(needle) && !existing.contains("include('" + lib.id() + "')")) {
            String stmt = "var " + jsAliasForLibrary(lib.id()) + " = include(\"" + lib.id() + "\");\n";
            this.editor.setValue(stmt + existing);
        }
        this.libraryStatus = null;
        selectView(VIEW_CODE);
    }

    private static ItemStack resolveIcon(String iconId) {
        ResourceLocation rl = iconId == null ? null : ResourceLocation.tryParse(iconId);
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) {
            item = SimpleAeScripting.LOGIC_BUS_ITEM.get();
        }
        return new ItemStack(item);
    }

    private void applyNextTemplate() {
        ScriptTemplates.Template tpl = ScriptTemplates.ALL.get(templateIdx);
        templateIdx = (templateIdx + 1) % ScriptTemplates.ALL.size();
        this.editor.setValue(tpl.code());
        selectView(VIEW_CODE);
    }

    private void openFullscreen() {
        int max = state == null ? 8000 : Math.min(state.maxLength, 30000);
        this.minecraft.setScreen(new ScriptEditorScreen(this, editor.getValue(), max, canAct()));
    }

    void saveFromFullscreen(String text) {
        editor.setValue(text);
        saveCurrentScript();
    }

    void restoreFromFullscreen(String text) {
        editor.setValue(text);
    }

    private void send(int action, int intArg, String text) {
        ScriptingPackets.CHANNEL.sendToServer(new ScriptActionPacket(terminal, action, selected, intArg, text));
    }

    @Override
    public void tick() {
        super.tick();
        if (++tickCounter % 20 == 0) {
            send(ScriptActionPacket.REFRESH, 0, "");
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- Рендер ---

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        boolean online = state != null && state.online && state.modEnabled;
        int led = state == null ? GuiStyle.LED_WARN : (online ? GuiStyle.LED_GOOD : GuiStyle.LED_BAD);
        ScriptStatePacket.BusInfo sel = selectedInfo();
        Component headerTitle = sel == null
                ? this.title
                : Component.literal(this.title.getString() + " — " + (sel.id().isEmpty() ? coords(sel.pos()) : sel.id()));
        GuiStyle.window(g, left, top, winW, winH, headerTitle, led, "code");

        drawActivityBar(g, mouseX, mouseY);
        if (sidebarOpen) {
            drawExplorerSidebar(g, mouseX, mouseY);
        }
        drawTopToolbar(g);

        // Редактор (если открыта вкладка Code)
        super.render(g, mouseX, mouseY, partialTick);

        if (activeView == VIEW_PANEL) {
            drawPanelView(g, mouseX, mouseY);
        } else if (activeView == VIEW_SCRIPTS || activeView == VIEW_LIBS) {
            drawLibraryView(g, mouseX, mouseY, activeView == VIEW_LIBS);
        }

        if (consoleH > 0) {
            drawConsole(g, mouseX, mouseY);
        }
        drawStatusBar(g);

        for (UiButton b : buttons) {
            b.render(g, this.font, mouseX, mouseY);
        }

        // Текст интервала между "-" и "+"
        String interval = currentInterval() + "t";
        g.drawString(this.font, interval, intervalCenterX - this.font.width(interval) / 2, top + HEADER_H + 6,
                GuiStyle.TEXT_TITLE, false);

        for (UiButton b : buttons) {
            b.renderTooltip(g, this.font, mouseX, mouseY);
        }
        drawActivityTooltips(g, mouseX, mouseY);
        drawExplorerTooltips(g, mouseX, mouseY);
    }

    // --- Левая панель активности (как в VS Code) ---

    private static final String[] ACT_ICONS = {"chip", "code", "monitor", "doc", "box"};
    private static final String[] ACT_TIPS = {
            "gui.simple_bytestorage_scripting.ide.act.explorer",
            "gui.simple_bytestorage_scripting.ide.act.code",
            "gui.simple_bytestorage_scripting.ide.act.panel",
            "gui.simple_bytestorage_scripting.ide.act.scripts",
            "gui.simple_bytestorage_scripting.ide.act.libs"
    };

    private int actButtonY(int idx) {
        return top + HEADER_H + 3 + idx * 24;
    }

    private void drawActivityBar(GuiGraphics g, int mouseX, int mouseY) {
        int ax = left + 3;
        int ay = top + HEADER_H;
        int ah = winH - HEADER_H - STATUSBAR_H - 2;
        g.fill(ax, ay, ax + ACTIVITY_W, ay + ah, 0xFF14181F);
        g.fill(ax + ACTIVITY_W - 1, ay, ax + ACTIVITY_W, ay + ah, 0xFF283240);

        for (int i = 0; i < ACT_ICONS.length; i++) {
            int by = actButtonY(i);
            boolean active = (i == 0 && sidebarOpen) || (i > 0 && activeView == i - 1);
            boolean hover = mouseX >= ax && mouseX < ax + ACTIVITY_W - 1 && mouseY >= by && mouseY < by + 22;
            if (active || hover) {
                g.fill(ax + 1, by, ax + ACTIVITY_W - 1, by + 22, active ? 0xFF1C2936 : 0xFF1A222C);
            }
            if (active && i > 0) {
                g.fill(ax, by + 2, ax + 2, by + 20, GuiStyle.ACCENT);
            } else if (active) {
                g.fill(ax, by + 5, ax + 2, by + 17, GuiStyle.TEXT_GOOD);
            }
            int color = active ? GuiStyle.ACCENT : (hover ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_MUTED);
            PixelIcons.draw(g, ACT_ICONS[i], ax + 6, by + 6, color, GuiStyle.TEXT_TITLE);
            // Индикатор наличия элементов панели
            if (i == 2 && !panelElements().isEmpty()) {
                g.fill(ax + 15, by + 4, ax + 18, by + 7, GuiStyle.TEXT_GOOD);
            }
        }
    }

    private void drawActivityTooltips(GuiGraphics g, int mouseX, int mouseY) {
        int ax = left + 3;
        if (mouseX < ax || mouseX >= ax + ACTIVITY_W) return;
        for (int i = 0; i < ACT_TIPS.length; i++) {
            int by = actButtonY(i);
            if (mouseY >= by && mouseY < by + 22) {
                g.renderTooltip(this.font, Component.translatable(ACT_TIPS[i]), mouseX, mouseY);
                return;
            }
        }
    }

    // --- Проводник (левая панель: шины + ID сети) ---

    private int sidebarHeight() {
        return winH - HEADER_H - STATUSBAR_H - 2;
    }

    private int busSectionHeight() {
        int totalH = sidebarHeight();
        return Math.max(56, (totalH * 62) / 100);
    }

    private void drawExplorerSidebar(GuiGraphics g, int mouseX, int mouseY) {
        int sy = top + HEADER_H;
        int sh = sidebarHeight();
        g.fill(sideX, sy, sideX + SIDEBAR_W, sy + sh, 0xFF171C24);
        g.fill(sideX + SIDEBAR_W - 1, sy, sideX + SIDEBAR_W, sy + sh, 0xFF283240);

        // Секция BUSES
        int busCount = state == null ? 0 : state.buses.size();
        g.fill(sideX, sy, sideX + SIDEBAR_W - 1, sy + 13, 0xFF1D242E);
        g.drawString(this.font,
                this.font.plainSubstrByWidth(
                        Component.translatable("gui.simple_bytestorage_scripting.ide.buses").getString() + " (" + busCount + ")",
                        SIDEBAR_W - 8),
                sideX + 4, sy + 3, GuiStyle.TEXT_LABEL, false);

        int busAreaY = sy + 14;
        int busAreaH = busSectionHeight() - 14;
        int busRows = Math.max(1, busAreaH / BUS_ROW_H);

        if (state != null) {
            if (state.buses.isEmpty()) {
                g.drawString(this.font, this.font.plainSubstrByWidth(
                        Component.translatable("gui.simple_bytestorage_scripting.no_buses").getString(), SIDEBAR_W - 10),
                        sideX + 5, busAreaY + 4, GuiStyle.TEXT_MUTED, false);
            } else {
                for (int row = 0; row < busRows; row++) {
                    int index = busScroll + row;
                    if (index >= state.buses.size()) break;
                    ScriptStatePacket.BusInfo info = state.buses.get(index);
                    int ry = busAreaY + row * BUS_ROW_H;
                    boolean isSel = info.pos().equals(selected);
                    boolean hover = mouseX >= sideX && mouseX < sideX + SIDEBAR_W - 1
                            && mouseY >= ry && mouseY < ry + BUS_ROW_H;
                    if (isSel) {
                        g.fill(sideX + 1, ry, sideX + SIDEBAR_W - 2, ry + BUS_ROW_H - 1, GuiStyle.ACCENT_FAINT);
                        g.fill(sideX, ry, sideX + 2, ry + BUS_ROW_H - 1, GuiStyle.ACCENT);
                    } else if (hover) {
                        g.fill(sideX + 1, ry, sideX + SIDEBAR_W - 2, ry + BUS_ROW_H - 1, 0x20FFFFFF);
                    }
                    int dot = info.status().startsWith("ERROR") ? GuiStyle.LED_BAD
                            : (info.enabled() ? GuiStyle.LED_GOOD : GuiStyle.LED_WARN);
                    g.fill(sideX + 5, ry + 4, sideX + 9, ry + 8, dot);
                    boolean hasId = !info.id().isEmpty();
                    String name = hasId ? info.id() : coords(info.pos());
                    g.drawString(this.font, this.font.plainSubstrByWidth(name, SIDEBAR_W - 18),
                            sideX + 13, ry + 3, hasId ? GuiStyle.ACCENT : GuiStyle.TEXT_TITLE, false);
                }
            }
        }

        // Секция NETWORK IDs
        int idHeadY = sy + busSectionHeight();
        List<Map.Entry<String, Integer>> ids = new ArrayList<>(idCounts().entrySet());
        g.fill(sideX, idHeadY, sideX + SIDEBAR_W - 1, idHeadY + 13, 0xFF1D242E);
        g.drawString(this.font,
                this.font.plainSubstrByWidth(
                        Component.translatable("gui.simple_bytestorage_scripting.id.list").getString() + " (" + ids.size() + ")",
                        SIDEBAR_W - 8),
                sideX + 4, idHeadY + 3, GuiStyle.TEXT_LABEL, false);

        int idAreaY = idHeadY + 14;
        int idAreaH = (sy + sh) - idAreaY - 2;
        int idRows = Math.max(1, idAreaH / ID_ROW_H);
        if (ids.isEmpty()) {
            g.drawString(this.font, this.font.plainSubstrByWidth(
                    Component.translatable("gui.simple_bytestorage_scripting.id.none").getString(), SIDEBAR_W - 10),
                    sideX + 5, idAreaY + 3, GuiStyle.TEXT_MUTED, false);
        } else {
            ScriptStatePacket.BusInfo cur = selectedInfo();
            for (int row = 0; row < idRows; row++) {
                int index = idScroll + row;
                if (index >= ids.size()) break;
                Map.Entry<String, Integer> e = ids.get(index);
                int ry = idAreaY + row * ID_ROW_H;
                boolean hover = mouseX >= sideX && mouseX < sideX + SIDEBAR_W - 1
                        && mouseY >= ry && mouseY < ry + ID_ROW_H;
                if (hover) {
                    g.fill(sideX + 1, ry, sideX + SIDEBAR_W - 2, ry + ID_ROW_H, 0x20FFFFFF);
                }
                boolean mine = cur != null && e.getKey().equals(cur.id());
                String cnt = "x" + e.getValue();
                int cntW = this.font.width(cnt);
                g.drawString(this.font, this.font.plainSubstrByWidth(e.getKey(), SIDEBAR_W - cntW - 12),
                        sideX + 5, ry + 1, mine ? GuiStyle.ACCENT : GuiStyle.TEXT_TITLE, false);
                g.drawString(this.font, cnt, sideX + SIDEBAR_W - cntW - 5, ry + 1, GuiStyle.TEXT_MUTED, false);
            }
        }
    }

    private void drawExplorerTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (!sidebarOpen || state == null) return;
        int sy = top + HEADER_H;
        int busAreaY = sy + 14;
        int busAreaH = busSectionHeight() - 14;
        if (mouseX >= sideX && mouseX < sideX + SIDEBAR_W - 1 && mouseY >= busAreaY && mouseY < busAreaY + busAreaH) {
            int row = (mouseY - busAreaY) / BUS_ROW_H;
            int idx = busScroll + row;
            if (idx >= 0 && idx < state.buses.size()) {
                ScriptStatePacket.BusInfo info = state.buses.get(idx);
                String text = (info.id().isEmpty() ? "" : info.id() + "  ") + coords(info.pos());
                g.renderTooltip(this.font, Component.literal(text), mouseX, mouseY);
            }
        }
    }

    // --- Верхняя панель инструментов ---

    private void drawTopToolbar(GuiGraphics g) {
        int ty = top + HEADER_H;
        g.fill(mainX, ty, left + winW - 3, ty + TOOLBAR_H - 1, 0xFF1A2029);
        g.fill(mainX, ty + TOOLBAR_H - 1, left + winW - 3, ty + TOOLBAR_H, 0xFF283240);

        String crumb;
        int crumbColor = GuiStyle.TEXT_TITLE;
        switch (activeView) {
            case VIEW_PANEL -> {
                crumb = Component.translatable("gui.simple_bytestorage_scripting.tab.panel").getString()
                        + (panelElements().isEmpty() ? "" : " (" + panelElements().size() + ")");
                crumbColor = GuiStyle.ACCENT;
            }
            case VIEW_SCRIPTS -> {
                crumb = Component.translatable("gui.simple_bytestorage_scripting.ide.act.scripts").getString();
                crumbColor = GuiStyle.ACCENT;
            }
            case VIEW_LIBS -> {
                crumb = Component.translatable("gui.simple_bytestorage_scripting.ide.act.libs").getString();
                crumbColor = GuiStyle.TEXT_GOOD;
            }
            default -> {
                ScriptStatePacket.BusInfo sel = selectedInfo();
                String base = sel == null ? "script.js" : ((sel.id().isEmpty() ? "bus" : sel.id()) + ".js");
                crumb = base + (isDirty() ? " *" : "");
                if (System.currentTimeMillis() < savedFlashUntil) {
                    crumb = base + " ✓";
                    crumbColor = GuiStyle.TEXT_GOOD;
                }
            }
        }
        int maxCrumbW = toolbarButtonsLeftX - (mainX + 10);
        if (maxCrumbW >= 24) {
            g.drawString(this.font, this.font.plainSubstrByWidth(crumb, maxCrumbW), mainX + 5, ty + 6, crumbColor, false);
        }
    }

    // --- Интерактивная панель (ui.*) ---

    private List<Cell> layoutPanel() {
        List<Cell> cells = new ArrayList<>();
        int width = mainW - 10;
        int y = 0;
        int bx = 0;
        boolean inRow = false;
        for (UiElement e : panelElements()) {
            if (e.kind() == UiElement.BUTTON) {
                int w = Math.max(30, this.font.width(e.text()) + 12);
                if (!inRow || bx + w > width) {
                    if (inRow) y += 19;
                    bx = 0;
                    inRow = true;
                }
                cells.add(new Cell(bx, y, w, 16, e));
                bx += w + 4;
                continue;
            }
            if (inRow) {
                y += 19;
                inRow = false;
                bx = 0;
            }
            int h = e.kind() == UiElement.ITEM ? 18 : (e.kind() == UiElement.BAR ? 13 : 11);
            cells.add(new Cell(0, y, width, h, e));
            y += h + 2;
        }
        panelContentHeight = y + (inRow ? 19 : 0);
        return cells;
    }

    private void drawPanelView(GuiGraphics g, int mouseX, int mouseY) {
        GuiStyle.display(g, mainX, workY, mainW, workH);
        List<Cell> cells = layoutPanel();
        if (cells.isEmpty()) {
            int y = workY + 8;
            for (FormattedCharSequence line : this.font.split(
                    Component.translatable("gui.simple_bytestorage_scripting.panel.empty"), mainW - 16)) {
                g.drawString(this.font, line, mainX + 8, y, GuiStyle.TEXT_MUTED, false);
                y += 10;
            }
            return;
        }
        panelScroll = Math.max(0, Math.min(panelScroll, Math.max(0, panelContentHeight - (workH - 8))));
        boolean live = canAct() && isRunning();
        g.enableScissor(mainX + 1, workY + 1, mainX + mainW - 1, workY + workH - 1);
        for (Cell cell : cells) {
            int x = mainX + 5 + cell.x();
            int y = workY + 5 + cell.y() - panelScroll;
            if (y + cell.h() < workY || y > workY + workH) continue;
            UiElement e = cell.element();
            int color = e.color() == 0 ? 0 : 0xFF000000 | e.color();
            switch (e.kind()) {
                case UiElement.TEXT -> g.drawString(this.font, this.font.plainSubstrByWidth(e.text(), cell.w()), x, y + 1,
                        color == 0 ? GuiStyle.TEXT_TITLE : color, false);
                case UiElement.BAR -> {
                    int labelW = Math.min(120, cell.w() / 3);
                    g.drawString(this.font, this.font.plainSubstrByWidth(e.text(), labelW - 4), x, y + 2, GuiStyle.TEXT_LABEL, false);
                    int bx = x + labelW;
                    int bw = cell.w() - labelW;
                    g.fill(bx, y, bx + bw, y + 11, GuiStyle.PANEL_EDGE_DARK);
                    g.fill(bx + 1, y + 1, bx + bw - 1, y + 10, GuiStyle.PANEL_BG);
                    float fraction = Math.max(0f, Math.min(1f, e.value() / (float) e.max()));
                    int fill = (int) ((bw - 2) * fraction);
                    g.fill(bx + 1, y + 1, bx + 1 + fill, y + 10, color == 0 ? GuiStyle.ACCENT_DIM : color);
                    String value = e.value() + "/" + e.max();
                    g.drawString(this.font, value, bx + (bw - this.font.width(value)) / 2, y + 2, GuiStyle.TEXT_TITLE, true);
                }
                case UiElement.ITEM -> {
                    ResourceLocation id = ResourceLocation.tryParse(e.id());
                    Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
                    if (item != null && id != null && ForgeRegistries.ITEMS.containsKey(id)) {
                        g.renderItem(new ItemStack(item), x, y);
                    }
                    g.drawString(this.font, this.font.plainSubstrByWidth(e.text(), cell.w() - 22), x + 20, y + 5,
                            color == 0 ? GuiStyle.TEXT_TITLE : color, false);
                }
                case UiElement.BUTTON -> {
                    boolean hover = live && mouseX >= x && mouseX < x + cell.w() && mouseY >= y && mouseY < y + cell.h()
                            && mouseY >= workY && mouseY < workY + workH;
                    g.fill(x, y, x + cell.w(), y + cell.h(), GuiStyle.PANEL_EDGE_DARK);
                    g.fill(x + 1, y + 1, x + cell.w() - 1, y + cell.h() - 1,
                            hover ? GuiStyle.ACCENT_DIM : GuiStyle.WINDOW_BG);
                    g.fill(x + 1, y + 1, x + cell.w() - 1, y + 2, GuiStyle.BEVEL_LIGHT);
                    int tc = !live ? GuiStyle.TEXT_MUTED : (color == 0 ? GuiStyle.TEXT_TITLE : color);
                    g.drawString(this.font, this.font.plainSubstrByWidth(e.text(), cell.w() - 6),
                            x + (cell.w() - Math.min(cell.w() - 6, this.font.width(e.text()))) / 2, y + 4, tc, false);
                }
                default -> {
                }
            }
        }
        g.disableScissor();
        if (!live) {
            g.drawString(this.font, this.font.plainSubstrByWidth(
                    Component.translatable("gui.simple_bytestorage_scripting.panel.stopped").getString(), mainW - 10),
                    mainX + 5, workY + workH - 11, GuiStyle.TEXT_WARN, false);
        }
    }

    // --- Онлайн-библиотека (Скрипты и JS-библиотеки) ---

    private void drawLibraryView(GuiGraphics g, int mouseX, int mouseY, boolean libsMode) {
        GuiStyle.display(g, mainX, workY, mainW, workH);

        if (state == null || !state.allowOnlineLibrary) {
            int y = workY + 10;
            for (FormattedCharSequence line : this.font.split(
                    Component.translatable("gui.simple_bytestorage_scripting.library.disabled"), mainW - 16)) {
                g.drawString(this.font, line, mainX + 8, y, GuiStyle.TEXT_WARN, false);
                y += 10;
            }
            return;
        }
        if (libraryLoading) {
            g.drawString(this.font, Component.translatable("gui.simple_bytestorage_scripting.library.loading"),
                    mainX + 8, workY + 10, GuiStyle.TEXT_MUTED, false);
            return;
        }
        if (libraryError != null) {
            g.drawString(this.font, this.font.plainSubstrByWidth(libraryError, mainW - 16),
                    mainX + 8, workY + 10, GuiStyle.TEXT_BAD, false);
            g.drawString(this.font, Component.translatable("gui.simple_bytestorage_scripting.library.retry"),
                    mainX + 8, workY + 24, GuiStyle.ACCENT, false);
            return;
        }

        // Шапка библиотеки: переключатель [Scripts | Libs], фильтр категории, кнопка Refresh
        int hy = workY + 2;
        int scriptCount = libraryEntries == null ? 0 : libraryEntries.size();
        int libCount = libraryModules == null ? 0 : libraryModules.size();

        int tab1W = 68;
        int tab2W = 64;
        boolean sHover = mouseX >= mainX + 2 && mouseX < mainX + 2 + tab1W && mouseY >= hy && mouseY < hy + LIB_HEADER_H - 1;
        g.fill(mainX + 2, hy, mainX + 2 + tab1W, hy + LIB_HEADER_H - 1,
                !libsMode ? GuiStyle.ACCENT_DIM : (sHover ? GuiStyle.ACCENT_FAINT : GuiStyle.PANEL_BG));
        g.drawString(this.font, this.font.plainSubstrByWidth(
                Component.translatable("gui.simple_bytestorage_scripting.library.mode.scripts").getString() + " (" + scriptCount + ")",
                tab1W - 6), mainX + 5, hy + 4, !libsMode ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_LABEL, false);

        int tab2X = mainX + 4 + tab1W;
        boolean lHover = mouseX >= tab2X && mouseX < tab2X + tab2W && mouseY >= hy && mouseY < hy + LIB_HEADER_H - 1;
        g.fill(tab2X, hy, tab2X + tab2W, hy + LIB_HEADER_H - 1,
                libsMode ? 0xFF1A5A3A : (lHover ? GuiStyle.ACCENT_FAINT : GuiStyle.PANEL_BG));
        g.drawString(this.font, this.font.plainSubstrByWidth(
                Component.translatable("gui.simple_bytestorage_scripting.library.mode.libs").getString() + " (" + libCount + ")",
                tab2W - 6), tab2X + 3, hy + 4, libsMode ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_LABEL, false);

        int catX = tab2X + tab2W + 4;
        String refTxt = Component.translatable("gui.simple_bytestorage_scripting.reload").getString();
        int refW = Math.max(44, this.font.width(refTxt) + 8);
        int refX = mainX + mainW - refW - 2;
        int catW = Math.max(36, refX - catX - 3);
        int visCount = libsMode ? filteredLibs().size() : filteredScripts().size();
        String catName = Component.translatable("gui.simple_bytestorage_scripting.cat." + currentCategory()).getString();
        boolean cHover = mouseX >= catX && mouseX < catX + catW && mouseY >= hy && mouseY < hy + LIB_HEADER_H - 1;
        g.fill(catX, hy, catX + catW, hy + LIB_HEADER_H - 1, cHover ? GuiStyle.ACCENT_FAINT : GuiStyle.PANEL_BG);
        g.drawString(this.font, this.font.plainSubstrByWidth(catName + " (" + visCount + ")", catW - 8),
                catX + 4, hy + 4, GuiStyle.ACCENT, false);

        boolean rHover = mouseX >= refX && mouseX < refX + refW && mouseY >= hy && mouseY < hy + LIB_HEADER_H - 1;
        g.fill(refX, hy, refX + refW, hy + LIB_HEADER_H - 1, rHover ? GuiStyle.ACCENT_FAINT : GuiStyle.PANEL_BG);
        g.drawString(this.font, this.font.plainSubstrByWidth(refTxt, refW - 6),
                refX + 4, hy + 4, GuiStyle.TEXT_LABEL, false);

        if (visCount == 0) {
            g.drawString(this.font, Component.translatable(libsMode
                    ? "gui.simple_bytestorage_scripting.library.empty_libs"
                    : "gui.simple_bytestorage_scripting.library.empty"),
                    mainX + 8, workY + LIB_HEADER_H + 8, GuiStyle.TEXT_MUTED, false);
            return;
        }

        int rows = libraryVisibleRows();
        String curVer = OnlineScriptLibrary.currentModVersion();
        int listY = workY + LIB_HEADER_H + 3;

        if (libsMode) {
            List<ScriptLibraryIndex.LibraryEntry> visible = filteredLibs();
            ScriptLibraryIndex.LibraryEntry hovered = null;
            for (int row = 0; row < rows; row++) {
                int idx = libraryScroll + row;
                if (idx >= visible.size()) break;
                ScriptLibraryIndex.LibraryEntry lib = visible.get(idx);
                int y = listY + row * LIB_ROW_H;
                boolean compat = ScriptLibraryIndex.isCompatible(curVer, lib.minModVersion(), lib.maxModVersion());
                boolean hover = mouseX >= mainX + 3 && mouseX < mainX + mainW - 3 && mouseY >= y && mouseY < y + LIB_ROW_H - 2;
                g.fill(mainX + 3, y, mainX + mainW - 3, y + LIB_ROW_H - 2, hover ? 0xFF1A2D3D : 0xFF111C28);
                if (hover) {
                    hovered = lib;
                    g.fill(mainX + 3, y, mainX + 5, y + LIB_ROW_H - 2, GuiStyle.TEXT_GOOD);
                }
                g.renderItem(resolveIcon(lib.icon()), mainX + 6, y + 5);
                String badge = compat ? "+ include()" : ("REQ " + lib.minModVersion());
                int badgeW = this.font.width(badge);
                g.drawString(this.font, this.font.plainSubstrByWidth(lib.name(), mainW - badgeW - 38),
                        mainX + 26, y + 3, compat ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_MUTED, false);
                g.drawString(this.font, badge, mainX + mainW - badgeW - 7, y + 3,
                        compat ? GuiStyle.TEXT_GOOD : GuiStyle.TEXT_BAD, false);
                String line2 = lib.id() + (lib.exports().isEmpty() ? "" : "  ·  " + String.join(", ", lib.exports()));
                g.drawString(this.font, this.font.plainSubstrByWidth(line2, mainW - 34),
                        mainX + 26, y + 14, GuiStyle.TEXT_LABEL, false);
            }
            if (hovered != null) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal(hovered.name()).withStyle(ChatFormatting.AQUA));
                tip.add(Component.literal("include(\"" + hovered.id() + "\")").withStyle(ChatFormatting.YELLOW));
                if (!hovered.description().isEmpty()) {
                    tip.add(Component.literal(hovered.description()).withStyle(ChatFormatting.GRAY));
                }
                if (!hovered.exports().isEmpty()) {
                    tip.add(Component.literal("Exports: " + String.join(", ", hovered.exports())).withStyle(ChatFormatting.DARK_AQUA));
                }
                boolean compat = ScriptLibraryIndex.isCompatible(curVer, hovered.minModVersion(), hovered.maxModVersion());
                tip.add(Component.translatable(compat
                        ? "gui.simple_bytestorage_scripting.library.click_include"
                        : "gui.simple_bytestorage_scripting.library.incompatible", hovered.minModVersion())
                        .withStyle(compat ? ChatFormatting.GREEN : ChatFormatting.RED));
                g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
            }
        } else {
            List<ScriptLibraryIndex.Entry> visible = filteredScripts();
            ScriptLibraryIndex.Entry hovered = null;
            for (int row = 0; row < rows; row++) {
                int idx = libraryScroll + row;
                if (idx >= visible.size()) break;
                ScriptLibraryIndex.Entry e = visible.get(idx);
                int y = listY + row * LIB_ROW_H;
                boolean compat = ScriptLibraryIndex.isCompatible(curVer, e.minModVersion(), e.maxModVersion());
                boolean hover = mouseX >= mainX + 3 && mouseX < mainX + mainW - 3 && mouseY >= y && mouseY < y + LIB_ROW_H - 2;
                g.fill(mainX + 3, y, mainX + mainW - 3, y + LIB_ROW_H - 2, hover ? 0xFF1A2D3D : 0xFF111C28);
                if (hover) {
                    hovered = e;
                    g.fill(mainX + 3, y, mainX + 5, y + LIB_ROW_H - 2, GuiStyle.ACCENT);
                }
                g.renderItem(resolveIcon(e.icon()), mainX + 6, y + 5);
                String badge = compat ? ("v" + e.minModVersion() + "+") : ("REQ " + e.minModVersion());
                int badgeW = this.font.width(badge);
                g.drawString(this.font, this.font.plainSubstrByWidth(e.name(), mainW - badgeW - 38),
                        mainX + 26, y + 3, compat ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_MUTED, false);
                g.drawString(this.font, badge, mainX + mainW - badgeW - 7, y + 3,
                        compat ? GuiStyle.TEXT_GOOD : GuiStyle.TEXT_BAD, false);
                String eCat = Component.translatable("gui.simple_bytestorage_scripting.cat." + e.category()).getString();
                String sub = e.author() + " · " + eCat + " — " + e.description();
                g.drawString(this.font, this.font.plainSubstrByWidth(sub, mainW - 34),
                        mainX + 26, y + 14, GuiStyle.TEXT_MUTED, false);
            }
            if (hovered != null) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal(hovered.name()).withStyle(ChatFormatting.AQUA));
                if (!hovered.description().isEmpty()) {
                    tip.add(Component.literal(hovered.description()).withStyle(ChatFormatting.GRAY));
                }
                boolean compat = ScriptLibraryIndex.isCompatible(curVer, hovered.minModVersion(), hovered.maxModVersion());
                tip.add(Component.translatable(compat
                        ? "gui.simple_bytestorage_scripting.library.click_load"
                        : "gui.simple_bytestorage_scripting.library.incompatible", hovered.minModVersion())
                        .withStyle(compat ? ChatFormatting.GREEN : ChatFormatting.RED));
                g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
            }
        }

        if (libraryStatus != null) {
            g.drawString(this.font, this.font.plainSubstrByWidth(libraryStatus, mainW - 12),
                    mainX + 6, workY + workH - 11, GuiStyle.ACCENT, false);
        }
    }

    // --- Сворачиваемая консоль ---

    private void drawConsole(GuiGraphics g, int mouseX, int mouseY) {
        GuiStyle.display(g, mainX, consoleY, mainW, consoleH);
        // Заголовок консоли
        boolean headHover = mouseX >= mainX && mouseX < mainX + mainW && mouseY >= consoleY && mouseY < consoleY + 13;
        g.fill(mainX + 1, consoleY + 1, mainX + mainW - 1, consoleY + 13, headHover ? 0xFF1B2735 : 0xFF141D28);
        String title = Component.translatable("gui.simple_bytestorage_scripting.ide.console").getString();
        g.drawString(this.font, title, mainX + 5, consoleY + 3, GuiStyle.TEXT_LABEL, false);

        // Текущий статус шины справа от заголовка
        ScriptStatePacket.BusInfo info = selectedInfo();
        String status = info == null ? "" : info.status();
        if (!status.isEmpty()) {
            int scol = status.startsWith("ERROR") ? GuiStyle.TEXT_BAD
                    : (status.startsWith("loading") ? GuiStyle.ACCENT : GuiStyle.TEXT_GOOD);
            int sx = mainX + 8 + this.font.width(title) + 6;
            g.drawString(this.font, this.font.plainSubstrByWidth("• " + status, mainW - (sx - mainX) - 20),
                    sx, consoleY + 3, scol, false);
        }

        String toggle = consoleOpen ? "v" : "^";
        g.drawString(this.font, toggle, mainX + mainW - 10, consoleY + 3, GuiStyle.TEXT_MUTED, false);

        if (!consoleOpen) return;

        int x = mainX + 5;
        int y = consoleY + 16;
        int w = mainW - 10;
        if (state == null) {
            g.drawString(this.font, Component.translatable("gui.simple_bytestorage_scripting.loading"), x, y, GuiStyle.TEXT_MUTED, false);
            return;
        }
        if (!state.modEnabled) {
            g.drawString(this.font, this.font.plainSubstrByWidth(
                    Component.translatable("gui.simple_bytestorage_scripting.disabled").getString(), w), x, y, GuiStyle.TEXT_BAD, false);
            return;
        }
        if (!state.online) {
            g.drawString(this.font, this.font.plainSubstrByWidth(
                    Component.translatable("gui.simple_bytestorage_scripting.offline").getString(), w), x, y, GuiStyle.TEXT_BAD, false);
            return;
        }
        int lines = Math.max(0, (consoleY + consoleH - 3 - y) / 9);
        int from = Math.max(0, state.log.size() - lines);
        for (int i = from; i < state.log.size(); i++) {
            String line = state.log.get(i);
            int col = line.startsWith("ERROR") ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_LABEL;
            g.drawString(this.font, this.font.plainSubstrByWidth("> " + line, w), x, y, col, false);
            y += 9;
        }
    }

    // --- Нижняя строка состояния IDE ---

    private void drawStatusBar(GuiGraphics g) {
        int sy = top + winH - STATUSBAR_H - 2;
        g.fill(left + 3, sy, left + winW - 3, sy + STATUSBAR_H, 0xFF131922);
        g.fill(left + 3, sy, left + winW - 3, sy + 1, 0xFF283240);

        boolean online = state != null && state.online && state.modEnabled;
        int dotCol = state == null ? GuiStyle.LED_WARN : (online ? GuiStyle.LED_GOOD : GuiStyle.LED_BAD);
        g.fill(left + 7, sy + 5, left + 11, sy + 9, dotCol);

        ScriptStatePacket.BusInfo sel = selectedInfo();
        String leftText = !online
                ? Component.translatable("gui.simple_bytestorage_scripting.offline").getString()
                : (sel == null ? "Online" : coords(sel.pos()));
        g.drawString(this.font, leftText, left + 15, sy + 3, GuiStyle.TEXT_LABEL, false);

        String mid = Component.translatable("gui.simple_bytestorage_scripting.editor.pos",
                editor.cursorLine(), editor.cursorColumn(), editor.length(), editor.maxLength()).getString();
        int midX = left + 15 + this.font.width(leftText) + 14;
        g.drawString(this.font, mid, midX, sy + 3, GuiStyle.TEXT_MUTED, false);

        String hint = "Ctrl+S · Ctrl+Space · Ctrl+E";
        int hw = this.font.width(hint);
        if (midX + this.font.width(mid) + 12 + hw < left + winW - 8) {
            g.drawString(this.font, hint, left + winW - 8 - hw, sy + 3, GuiStyle.TEXT_MUTED, false);
        }
    }

    // --- Ввод ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Крестик закрытия в шапке окна
        if (button == 0) {
            int[] close = GuiStyle.closeButtonArea();
            if (close != null && mouseX >= close[0] && mouseX < close[0] + close[2]
                    && mouseY >= close[1] && mouseY < close[1] + close[3]) {
                this.onClose();
                return true;
            }
        }
        for (UiButton b : buttons) {
            if (b.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (button == 0) {
            // Левая панель активности
            int ax = left + 3;
            if (mouseX >= ax && mouseX < ax + ACTIVITY_W) {
                for (int i = 0; i < ACT_ICONS.length; i++) {
                    int by = actButtonY(i);
                    if (mouseY >= by && mouseY < by + 22) {
                        if (i == 0) {
                            sidebarOpen = !sidebarOpen;
                            relayout();
                        } else {
                            selectView(i - 1);
                        }
                        return true;
                    }
                }
            }

            // Заголовок консоли (свернуть / развернуть)
            if (consoleH > 0 && mouseX >= mainX && mouseX < mainX + mainW
                    && mouseY >= consoleY && mouseY < consoleY + 13) {
                consoleOpen = !consoleOpen;
                relayout();
                return true;
            }

            // Проводник: шины и ID
            if (sidebarOpen && state != null && mouseX >= sideX && mouseX < sideX + SIDEBAR_W - 1) {
                int sy = top + HEADER_H;
                int busAreaY = sy + 14;
                int busAreaH = busSectionHeight() - 14;
                if (mouseY >= busAreaY && mouseY < busAreaY + busAreaH) {
                    int index = busScroll + (int) ((mouseY - busAreaY) / BUS_ROW_H);
                    if (index >= 0 && index < state.buses.size()) {
                        selected = state.buses.get(index).pos();
                        send(ScriptActionPacket.LIST, 0, "");
                        return true;
                    }
                }
                int idAreaY = sy + busSectionHeight() + 14;
                if (mouseY >= idAreaY && mouseY < sy + sidebarHeight()) {
                    List<String> ids = new ArrayList<>(idCounts().keySet());
                    int index = idScroll + (int) ((mouseY - idAreaY) / ID_ROW_H);
                    if (index >= 0 && index < ids.size()) {
                        for (ScriptStatePacket.BusInfo info : state.buses) {
                            if (info.id().equals(ids.get(index))) {
                                selected = info.pos();
                                send(ScriptActionPacket.LIST, 0, "");
                                break;
                            }
                        }
                        return true;
                    }
                }
            }

            if (activeView == VIEW_PANEL && clickPanel(mouseX, mouseY)) {
                return true;
            }
            if ((activeView == VIEW_SCRIPTS || activeView == VIEW_LIBS)
                    && clickLibrary(mouseX, mouseY, activeView == VIEW_LIBS)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickPanel(double mouseX, double mouseY) {
        if (mouseX < mainX || mouseX >= mainX + mainW || mouseY < workY || mouseY >= workY + workH) {
            return false;
        }
        if (canAct() && isRunning()) {
            for (Cell cell : layoutPanel()) {
                if (cell.element().kind() != UiElement.BUTTON) continue;
                int x = mainX + 5 + cell.x();
                int y = workY + 5 + cell.y() - panelScroll;
                if (mouseX >= x && mouseX < x + cell.w() && mouseY >= y && mouseY < y + cell.h()) {
                    send(ScriptActionPacket.UI_BUTTON, 0, cell.element().id());
                    break;
                }
            }
        }
        return true;
    }

    private boolean clickLibrary(double mouseX, double mouseY, boolean libsMode) {
        if (mouseX < mainX || mouseX >= mainX + mainW || mouseY < workY || mouseY >= workY + workH) {
            return false;
        }
        if (state == null || !state.allowOnlineLibrary) return true;
        if (libraryError != null) {
            loadOnlineLibrary(true);
            return true;
        }
        int hy = workY + 2;
        if (mouseY >= hy && mouseY < hy + LIB_HEADER_H) {
            int tab1W = 68;
            int tab2W = 64;
            int tab2X = mainX + 4 + tab1W;
            String refTxt = Component.translatable("gui.simple_bytestorage_scripting.reload").getString();
            int refW = Math.max(44, this.font.width(refTxt) + 8);
            int refX = mainX + mainW - refW - 2;
            if (mouseX < mainX + 2 + tab1W) {
                selectView(VIEW_SCRIPTS);
            } else if (mouseX >= tab2X && mouseX < tab2X + tab2W) {
                selectView(VIEW_LIBS);
            } else if (mouseX >= refX) {
                loadOnlineLibrary(true);
            } else {
                libraryCatIdx = (libraryCatIdx + 1) % ScriptLibraryIndex.CATEGORIES.size();
                libraryScroll = 0;
            }
            return true;
        }
        if (libraryLoading || !canAct()) return true;
        int listY = workY + LIB_HEADER_H + 3;
        int row = (int) ((mouseY - listY) / LIB_ROW_H);
        int idx = libraryScroll + row;
        int rows = libraryVisibleRows();
        if (row < 0 || row >= rows) return true;

        String curVer = OnlineScriptLibrary.currentModVersion();
        if (libsMode) {
            List<ScriptLibraryIndex.LibraryEntry> visible = filteredLibs();
            if (idx >= 0 && idx < visible.size()) {
                ScriptLibraryIndex.LibraryEntry lib = visible.get(idx);
                if (!ScriptLibraryIndex.isCompatible(curVer, lib.minModVersion(), lib.maxModVersion())) {
                    libraryStatus = "Requires mod " + lib.minModVersion() + "+";
                    return true;
                }
                insertLibraryInclude(lib);
            }
            return true;
        }
        List<ScriptLibraryIndex.Entry> visible = filteredScripts();
        if (idx >= 0 && idx < visible.size()) {
            ScriptLibraryIndex.Entry e = visible.get(idx);
            if (!ScriptLibraryIndex.isCompatible(curVer, e.minModVersion(), e.maxModVersion())) {
                libraryStatus = "Requires mod " + e.minModVersion() + "+";
                return true;
            }
            libraryStatus = "Downloading " + e.id() + "...";
            OnlineScriptLibrary.fetchScript(state.onlineLibraryUrl, e,
                    code -> {
                        this.libraryStatus = null;
                        this.editor.setValue(code);
                        selectView(VIEW_CODE);
                    },
                    err -> this.libraryStatus = err);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if ((activeView == VIEW_SCRIPTS || activeView == VIEW_LIBS)
                && mouseX >= mainX && mouseX < mainX + mainW && mouseY >= workY && mouseY < workY + workH) {
            int total = activeView == VIEW_LIBS ? filteredLibs().size() : filteredScripts().size();
            int max = Math.max(0, total - libraryVisibleRows());
            libraryScroll = Math.max(0, Math.min(max, libraryScroll - (int) Math.signum(delta)));
            return true;
        }
        if (activeView == VIEW_PANEL
                && mouseX >= mainX && mouseX < mainX + mainW && mouseY >= workY && mouseY < workY + workH) {
            int max = Math.max(0, panelContentHeight - (workH - 8));
            panelScroll = Math.max(0, Math.min(max, panelScroll - (int) Math.signum(delta) * 12));
            return true;
        }
        if (sidebarOpen && state != null && mouseX >= sideX && mouseX < sideX + SIDEBAR_W) {
            int splitY = top + HEADER_H + busSectionHeight();
            if (mouseY < splitY) {
                int busRows = Math.max(1, (busSectionHeight() - 14) / BUS_ROW_H);
                int max = Math.max(0, state.buses.size() - busRows);
                busScroll = Math.max(0, Math.min(max, busScroll - (int) Math.signum(delta)));
            } else {
                int idRows = Math.max(1, (sidebarHeight() - busSectionHeight() - 16) / ID_ROW_H);
                int max = Math.max(0, idCounts().size() - idRows);
                idScroll = Math.max(0, Math.min(max, idScroll - (int) Math.signum(delta)));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_S && canAct()) {
            saveCurrentScript();
            return true;
        }
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_E) {
            openFullscreen();
            return true;
        }
        if (activeView == VIEW_CODE && this.editor.isFocused() && this.editor.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static String coords(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
