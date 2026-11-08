package com.simpleae.bytestorage.scripting.client;

import com.simpleae.bytestorage.client.gui.GuiStyle;
import com.simpleae.bytestorage.client.gui.UiButton;
import com.simpleae.bytestorage.scripting.net.ScriptActionPacket;
import com.simpleae.bytestorage.scripting.net.ScriptStatePacket;
import com.simpleae.bytestorage.scripting.script.UiElement;
import com.simpleae.bytestorage.scripting.net.ScriptingPackets;
import net.minecraft.client.gui.GuiGraphics;
import com.simpleae.bytestorage.client.gui.CodeEditor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Редактор скриптів Логічних шин мережі. */
public class LogicTerminalScreen extends Screen {

    private static final int W = 466;
    private static final int H = 232;
    private static final int ROW_H = 16;
    private static final int LIST_ROWS = 12;
    private static final int ID_ROWS = 9;
    private static final int ID_X = 362;

    private final BlockPos terminal;
    private int left;
    private int top;

    private CodeEditor editor;
    private int idScroll = 0;
    private final List<UiButton> buttons = new ArrayList<>();

    private ScriptStatePacket state;
    private BlockPos selected;
    private int listScroll = 0;
    private int tickCounter = 0;
    private boolean autoSelected = false;
    private boolean panelTab = false;
    private int panelScroll = 0;
    private int panelContentHeight = 0;

    private static final int AREA_X = 112;
    private static final int AREA_Y = 34;
    private static final int AREA_W = 242;
    private static final int AREA_H = 104;

    private record Cell(int x, int y, int w, int h, UiElement element) {
    }

    public LogicTerminalScreen(BlockPos terminal) {
        super(Component.translatable("block.simple_bytestorage_scripting.logic_terminal"));
        this.terminal = terminal;
    }

    @Override
    protected void init() {
        this.left = (this.width - W) / 2;
        this.top = (this.height - H) / 2;

        String previous = this.editor != null ? this.editor.getValue() : "";
        this.editor = new CodeEditor(this.font, left + AREA_X, top + AREA_Y, AREA_W, AREA_H);
        this.editor.setCompletion(ApiCatalog.engine(), ApiCatalog.GLOBALS);
        this.editor.setMaxLength(state == null ? 8000 : Math.min(state.maxLength, 30000));
        this.editor.setValue(previous);
        this.addRenderableWidget(this.editor);
        this.setInitialFocus(this.editor);
        this.editor.visible = !panelTab;
        this.editor.active = !panelTab;

        buttons.clear();
        int by = top + 142;
        buttons.add(new UiButton(left + 112, by, 44, 16,
                () -> Component.translatable("gui.simple_bytestorage_scripting.save"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.SAVE, 0, clampedText()))
                .enabledWhen(this::canAct).primary()
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.save.tip")));
        buttons.add(new UiButton(left + 160, by, 54, 16,
                () -> Component.translatable(isRunning()
                        ? "gui.simple_bytestorage_scripting.stop" : "gui.simple_bytestorage_scripting.run"),
                UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.ENABLE, isRunning() ? 0 : 1, ""))
                .enabledWhen(this::canAct)
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.run.tip")));
        buttons.add(new UiButton(left + 218, by, 44, 16,
                () -> Component.translatable("gui.simple_bytestorage_scripting.reload"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.LIST, 0, ""))
                .enabledWhen(() -> selected != null)
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.reload.tip")));
        buttons.add(new UiButton(left + 266, by, 22, 16, () -> Component.empty(), UiButton.Icon.NONE,
                this::openFullscreen)
                .icon("arrows")
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.fullscreen.tip")));
        buttons.add(new UiButton(left + 292, by, 16, 16, () -> Component.literal("-"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.INTERVAL, Math.max(1, currentInterval() - 10), ""))
                .enabledWhen(this::canAct));
        buttons.add(new UiButton(left + 338, by, 16, 16, () -> Component.literal("+"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.INTERVAL, currentInterval() + 10, ""))
                .enabledWhen(this::canAct));

        // Перший запит списку шин (при перебудові вікна стан і текст зберігаються)
        if (state == null) {
            ScriptingPackets.CHANNEL.sendToServer(new ScriptActionPacket(terminal, ScriptActionPacket.LIST, null, 0, ""));
        }
    }

    // --- Стан ---

    public void apply(ScriptStatePacket packet) {
        this.state = packet;
        this.selected = packet.selected;
        this.editor.setMaxLength(Math.min(packet.maxLength, 30000));
        if (packet.withScript) {
            this.editor.setValue(packet.script);
        }
        if (selected == null && !autoSelected && !packet.buses.isEmpty()) {
            autoSelected = true;
            selected = packet.buses.get(0).pos();
            send(ScriptActionPacket.LIST, 0, "");
        }
    }

    /** Унікальні ідентифікатори мережі з кількістю шин. */
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

    private int currentInterval() {
        ScriptStatePacket.BusInfo info = selectedInfo();
        return info == null ? 20 : info.interval();
    }

    private String clampedText() {
        String text = editor.getValue();
        int max = state == null ? 8000 : Math.min(state.maxLength, 30000);
        return text.length() > max ? text.substring(0, max) : text;
    }

    private void setPanelTab(boolean panel) {
        this.panelTab = panel;
        this.editor.visible = !panel;
        this.editor.active = !panel;
        if (panel) {
            this.editor.setFocused(false);
            this.setFocused(null);
        } else {
            this.setInitialFocus(this.editor);
        }
    }

    private List<UiElement> panelElements() {
        return state == null ? List.of() : state.panel;
    }

    /** Раскладка панели скрипта: текст/полосы/предметы — по строке, кнопки идут в ряд и переносятся. */
    private List<Cell> layoutPanel() {
        List<Cell> cells = new ArrayList<>();
        int width = AREA_W - 8;
        int y = 0;
        int bx = 0;
        boolean inRow = false;
        for (UiElement e : panelElements()) {
            if (e.kind() == UiElement.BUTTON) {
                int w = Math.max(30, this.font.width(e.text()) + 12);
                if (!inRow || bx + w > width) {
                    if (inRow) {
                        y += 19;
                    }
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

    private void drawTabs(GuiGraphics g, int mouseX, int mouseY) {
        String[] labels = {
                Component.translatable("gui.simple_bytestorage_scripting.tab.code").getString(),
                Component.translatable("gui.simple_bytestorage_scripting.tab.panel").getString()
                        + (panelElements().isEmpty() ? "" : " (" + panelElements().size() + ")")};
        for (int i = 0; i < 2; i++) {
            int tx = left + AREA_X + i * 70;
            int ty = top + 20;
            boolean active = (i == 1) == panelTab;
            boolean hover = mouseX >= tx && mouseX < tx + 68 && mouseY >= ty && mouseY < ty + 13;
            g.fill(tx, ty, tx + 68, ty + 13, active ? GuiStyle.ACCENT_DIM : (hover ? GuiStyle.ACCENT_FAINT : GuiStyle.PANEL_BG));
            g.fill(tx, ty + 12, tx + 68, ty + 13, active ? GuiStyle.ACCENT : GuiStyle.PANEL_EDGE_LIGHT);
            g.drawString(this.font, this.font.plainSubstrByWidth(labels[i], 64), tx + 4, ty + 3,
                    active ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_LABEL, false);
        }
    }

    private void drawPanel(GuiGraphics g, int mouseX, int mouseY) {
        int rx = left + AREA_X;
        int ry = top + AREA_Y;
        GuiStyle.display(g, rx, ry, AREA_W, AREA_H);
        List<Cell> cells = layoutPanel();
        if (cells.isEmpty()) {
            int y = ry + 6;
            for (net.minecraft.util.FormattedCharSequence line : this.font.split(
                    Component.translatable("gui.simple_bytestorage_scripting.panel.empty"), AREA_W - 12)) {
                g.drawString(this.font, line, rx + 6, y, GuiStyle.TEXT_MUTED, false);
                y += 10;
            }
            return;
        }
        panelScroll = Math.max(0, Math.min(panelScroll, Math.max(0, panelContentHeight - (AREA_H - 8))));
        boolean live = canAct() && isRunning();
        g.enableScissor(rx + 1, ry + 1, rx + AREA_W - 1, ry + AREA_H - 1);
        for (Cell cell : cells) {
            int x = rx + 4 + cell.x();
            int y = ry + 4 + cell.y() - panelScroll;
            if (y + cell.h() < ry || y > ry + AREA_H) {
                continue;
            }
            UiElement e = cell.element();
            int color = e.color() == 0 ? 0 : 0xFF000000 | e.color();
            switch (e.kind()) {
                case UiElement.TEXT -> g.drawString(this.font, this.font.plainSubstrByWidth(e.text(), cell.w()), x, y + 1,
                        color == 0 ? GuiStyle.TEXT_TITLE : color, false);
                case UiElement.BAR -> {
                    int labelW = 96;
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
                    var item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
                    if (item != null && id != null && ForgeRegistries.ITEMS.containsKey(id)) {
                        g.renderItem(new ItemStack(item), x, y);
                    }
                    g.drawString(this.font, this.font.plainSubstrByWidth(e.text(), cell.w() - 22), x + 20, y + 5,
                            color == 0 ? GuiStyle.TEXT_TITLE : color, false);
                }
                case UiElement.BUTTON -> {
                    boolean hover = live && mouseX >= x && mouseX < x + cell.w() && mouseY >= y && mouseY < y + cell.h()
                            && mouseY >= ry && mouseY < ry + AREA_H;
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
        if (panelContentHeight > AREA_H - 8) {
            int trackH = AREA_H - 4;
            int thumbH = Math.max(8, trackH * (AREA_H - 8) / panelContentHeight);
            int thumbY = ry + 2 + (trackH - thumbH) * panelScroll / Math.max(1, panelContentHeight - (AREA_H - 8));
            g.fill(rx + AREA_W - 4, ry + 2, rx + AREA_W - 2, ry + AREA_H - 2, 0xFF101822);
            g.fill(rx + AREA_W - 4, thumbY, rx + AREA_W - 2, thumbY + thumbH, GuiStyle.ACCENT_DIM);
        }
        if (!live) {
            g.drawString(this.font, this.font.plainSubstrByWidth(
                    Component.translatable("gui.simple_bytestorage_scripting.panel.stopped").getString(), AREA_W - 8),
                    rx + 4, ry + AREA_H - 11, GuiStyle.TEXT_WARN, false);
        }
    }

    private boolean clickPanel(double mouseX, double mouseY) {
        int rx = left + AREA_X;
        int ry = top + AREA_Y;
        if (mouseX < rx || mouseX >= rx + AREA_W || mouseY < ry || mouseY >= ry + AREA_H) {
            return false;
        }
        if (canAct() && isRunning()) {
            for (Cell cell : layoutPanel()) {
                if (cell.element().kind() != UiElement.BUTTON) {
                    continue;
                }
                int x = rx + 4 + cell.x();
                int y = ry + 4 + cell.y() - panelScroll;
                if (mouseX >= x && mouseX < x + cell.w() && mouseY >= y && mouseY < y + cell.h()) {
                    send(ScriptActionPacket.UI_BUTTON, 0, cell.element().id());
                    break;
                }
            }
        }
        return true;
    }

    private void openFullscreen() {
        int max = state == null ? 8000 : Math.min(state.maxLength, 30000);
        this.minecraft.setScreen(new ScriptEditorScreen(this, editor.getValue(), max, canAct()));
    }

    /** Збереження з повноекранного редактора. */
    void saveFromFullscreen(String text) {
        editor.setValue(text);
        send(ScriptActionPacket.SAVE, 0, clampedText());
    }

    /** Повернення з повноекранного редактора: текст переходить у мале поле. */
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

    // --- Ввід ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (UiButton b : buttons) {
            if (b.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (button == 0) {
            for (int i = 0; i < 2; i++) {
                int tx = left + AREA_X + i * 70;
                if (mouseX >= tx && mouseX < tx + 68 && mouseY >= top + 20 && mouseY < top + 33) {
                    setPanelTab(i == 1);
                    return true;
                }
            }
            if (panelTab && clickPanel(mouseX, mouseY)) {
                return true;
            }
        }
        if (state != null && button == 0) {
            int ix = left + ID_X;
            int iy = top + 31;
            if (mouseX >= ix && mouseX < ix + 96 && mouseY >= iy && mouseY < iy + ID_ROWS * 10) {
                List<String> ids = new ArrayList<>(idCounts().keySet());
                int index = idScroll + (int) ((mouseY - iy) / 10);
                if (index >= 0 && index < ids.size()) {
                    // Клік по ID — вибрати першу шину з цим ідентифікатором
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
            int lx = left + 6;
            int ly = top + 20;
            if (mouseX >= lx && mouseX < lx + 100 && mouseY >= ly && mouseY < ly + LIST_ROWS * ROW_H) {
                int index = listScroll + (int) ((mouseY - ly) / ROW_H);
                if (index >= 0 && index < state.buses.size()) {
                    selected = state.buses.get(index).pos();
                    send(ScriptActionPacket.LIST, 0, "");
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (panelTab && mouseX >= left + AREA_X && mouseX < left + AREA_X + AREA_W
                && mouseY >= top + AREA_Y && mouseY < top + AREA_Y + AREA_H) {
            int max = Math.max(0, panelContentHeight - (AREA_H - 8));
            panelScroll = Math.max(0, Math.min(max, panelScroll - (int) Math.signum(delta) * 12));
            return true;
        }
        if (state != null && mouseX >= left + ID_X) {
            int max = Math.max(0, idCounts().size() - ID_ROWS);
            idScroll = Math.max(0, Math.min(max, idScroll - (int) Math.signum(delta)));
            return true;
        }
        if (state != null && mouseX >= left + 6 && mouseX < left + 106) {
            int max = Math.max(0, state.buses.size() - LIST_ROWS);
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Редактор первым получает клавиши (ESC закрывает подсказки, а не окно)
        if (!panelTab && hasControlDown() && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_E) {
            openFullscreen();
            return true;
        }
        if (this.editor.isFocused() && this.editor.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        GuiStyle.window(g, left, top, W, H, this.title, led, "code");

        // Список шин
        GuiStyle.display(g, left + 6, top + 20, 100, LIST_ROWS * ROW_H + 2);
        if (state != null) {
            for (int row = 0; row < LIST_ROWS; row++) {
                int index = listScroll + row;
                if (index >= state.buses.size()) {
                    break;
                }
                ScriptStatePacket.BusInfo info = state.buses.get(index);
                int ry = top + 21 + row * ROW_H;
                if (info.pos().equals(selected)) {
                    g.fill(left + 7, ry, left + 105, ry + ROW_H - 1, GuiStyle.ACCENT_FAINT);
                }
                int dot = info.status().startsWith("ERROR") ? GuiStyle.LED_BAD
                        : (info.enabled() ? GuiStyle.LED_GOOD : GuiStyle.LED_WARN);
                g.fill(left + 10, ry + 4, left + 15, ry + 9, dot);
                boolean hasId = !info.id().isEmpty();
                String name = hasId ? info.id() : coords(info.pos());
                g.drawString(this.font, this.font.plainSubstrByWidth(name, 84), left + 19, ry + 3,
                        hasId ? GuiStyle.ACCENT : GuiStyle.TEXT_TITLE, false);
            }
            if (state.buses.isEmpty()) {
                g.drawString(this.font, this.font.plainSubstrByWidth(
                        Component.translatable("gui.simple_bytestorage_scripting.no_buses").getString(), 92),
                        left + 10, top + 26, GuiStyle.TEXT_MUTED, false);
            }
        }

        drawIdPanel(g);

        // Лог / статус
        GuiStyle.display(g, left + 112, top + 162, 242, 64);
        super.render(g, mouseX, mouseY, partialTick);
        drawTabs(g, mouseX, mouseY);
        if (panelTab) {
            drawPanel(g, mouseX, mouseY);
        }
        drawStatus(g);

        for (UiButton b : buttons) {
            b.render(g, this.font, mouseX, mouseY);
        }
        // Інтервал між кнопками «-» і «+»
        String interval = currentInterval() + "t";
        g.drawString(this.font, interval, left + 300 + (30 - this.font.width(interval)) / 2, top + 146,
                GuiStyle.TEXT_TITLE, false);
        for (UiButton b : buttons) {
            b.renderTooltip(g, this.font, mouseX, mouseY);
        }
        // Підказка з координатами шини під курсором
        if (state != null && mouseX >= left + 6 && mouseX < left + 106
                && mouseY >= top + 21 && mouseY < top + 21 + LIST_ROWS * ROW_H) {
            int index = listScroll + (mouseY - top - 21) / ROW_H;
            if (index >= 0 && index < state.buses.size()) {
                ScriptStatePacket.BusInfo info = state.buses.get(index);
                String text = (info.id().isEmpty() ? "" : info.id() + "  ") + coords(info.pos());
                g.renderTooltip(this.font, Component.literal(text), mouseX, mouseY);
            }
        }
    }

    private static String coords(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    /** Права панель: ідентифікатори мережі та поле для призначення. */
    private void drawIdPanel(GuiGraphics g) {
        GuiStyle.label(g, Component.translatable("gui.simple_bytestorage_scripting.id.list"), left + ID_X, top + 21);
        GuiStyle.display(g, left + ID_X - 1, top + 30, 98, ID_ROWS * 10 + 2);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(idCounts().entrySet());
        if (entries.isEmpty()) {
            g.drawString(this.font, this.font.plainSubstrByWidth(
                    Component.translatable("gui.simple_bytestorage_scripting.id.none").getString(), 90),
                    left + ID_X + 3, top + 34, GuiStyle.TEXT_MUTED, false);
        }
        ScriptStatePacket.BusInfo current = selectedInfo();
        for (int row = 0; row < ID_ROWS; row++) {
            int index = idScroll + row;
            if (index >= entries.size()) {
                break;
            }
            Map.Entry<String, Integer> entry = entries.get(index);
            int ry = top + 31 + row * 10;
            boolean mine = current != null && entry.getKey().equals(current.id());
            String count = "x" + entry.getValue();
            g.drawString(this.font, this.font.plainSubstrByWidth(entry.getKey(), 66), left + ID_X + 3, ry + 1,
                    mine ? GuiStyle.ACCENT : GuiStyle.TEXT_TITLE, false);
            g.drawString(this.font, count, left + ID_X + 93 - this.font.width(count), ry + 1, GuiStyle.TEXT_MUTED, false);
        }
        int hy = top + 30 + ID_ROWS * 10 + 8;
        for (net.minecraft.util.FormattedCharSequence line : this.font.split(
                Component.translatable("gui.simple_bytestorage_scripting.id.terminal_hint"), 96)) {
            g.drawString(this.font, line, left + ID_X, hy, GuiStyle.TEXT_MUTED, false);
            hy += 10;
        }
    }

    private void drawStatus(GuiGraphics g) {
        int x = left + 116;
        int y = top + 166;
        int w = 234;
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
        if (!state.canEdit) {
            g.drawString(this.font, this.font.plainSubstrByWidth(
                    Component.translatable("gui.simple_bytestorage_scripting.readonly").getString(), w), x, y, GuiStyle.TEXT_WARN, false);
            y += 10;
        } else {
            ScriptStatePacket.BusInfo info = selectedInfo();
            String status = info == null ? "" : info.status();
            int color = status.startsWith("ERROR") ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_GOOD;
            if (!status.isEmpty()) {
                g.drawString(this.font, this.font.plainSubstrByWidth(status, w), x, y, color, false);
                y += 10;
            }
        }
        // Останні рядки логу (скільки вміститься)
        int lines = Math.max(0, (top + 162 + 64 - 4 - y) / 10);
        int from = Math.max(0, state.log.size() - lines);
        for (int i = from; i < state.log.size(); i++) {
            g.drawString(this.font, this.font.plainSubstrByWidth(state.log.get(i), w), x, y, GuiStyle.TEXT_LABEL, false);
            y += 10;
        }
    }
}
