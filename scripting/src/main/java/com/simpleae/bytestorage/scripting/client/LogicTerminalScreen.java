package com.simpleae.bytestorage.scripting.client;

import com.simpleae.bytestorage.client.gui.GuiStyle;
import com.simpleae.bytestorage.client.gui.UiButton;
import com.simpleae.bytestorage.scripting.net.ScriptActionPacket;
import com.simpleae.bytestorage.scripting.net.ScriptStatePacket;
import com.simpleae.bytestorage.scripting.net.ScriptingPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Редактор скриптів Логічних шин мережі. */
public class LogicTerminalScreen extends Screen {

    private static final int W = 360;
    private static final int H = 232;
    private static final int ROW_H = 16;
    private static final int LIST_ROWS = 12;

    private final BlockPos terminal;
    private int left;
    private int top;

    private MultiLineEditBox editor;
    private final List<UiButton> buttons = new ArrayList<>();

    private ScriptStatePacket state;
    private BlockPos selected;
    private int listScroll = 0;
    private int tickCounter = 0;
    private boolean autoSelected = false;

    public LogicTerminalScreen(BlockPos terminal) {
        super(Component.translatable("block.simple_bytestorage_scripting.logic_terminal"));
        this.terminal = terminal;
    }

    @Override
    protected void init() {
        this.left = (this.width - W) / 2;
        this.top = (this.height - H) / 2;

        String previous = this.editor != null ? this.editor.getValue() : "";
        this.editor = new MultiLineEditBox(this.font, left + 112, top + 20, 242, 118,
                Component.empty(), Component.empty());
        this.editor.setValue(previous);
        this.addRenderableWidget(this.editor);
        this.setInitialFocus(this.editor);

        buttons.clear();
        int by = top + 142;
        buttons.add(new UiButton(left + 112, by, 52, 16,
                () -> Component.translatable("gui.simple_bytestorage_scripting.save"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.SAVE, 0, clampedText()))
                .enabledWhen(this::canAct).primary()
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.save.tip")));
        buttons.add(new UiButton(left + 168, by, 62, 16,
                () -> Component.translatable(isRunning()
                        ? "gui.simple_bytestorage_scripting.stop" : "gui.simple_bytestorage_scripting.run"),
                UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.ENABLE, isRunning() ? 0 : 1, ""))
                .enabledWhen(this::canAct)
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.run.tip")));
        buttons.add(new UiButton(left + 234, by, 52, 16,
                () -> Component.translatable("gui.simple_bytestorage_scripting.reload"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.LIST, 0, ""))
                .enabledWhen(() -> selected != null)
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.reload.tip")));
        buttons.add(new UiButton(left + 292, by, 16, 16, () -> Component.literal("-"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.INTERVAL, Math.max(1, currentInterval() - 10), ""))
                .enabledWhen(this::canAct));
        buttons.add(new UiButton(left + 338, by, 16, 16, () -> Component.literal("+"), UiButton.Icon.NONE,
                () -> send(ScriptActionPacket.INTERVAL, currentInterval() + 10, ""))
                .enabledWhen(this::canAct));

        // Перший запит списку шин
        ScriptingPackets.CHANNEL.sendToServer(new ScriptActionPacket(terminal, ScriptActionPacket.LIST, null, 0, ""));
    }

    // --- Стан ---

    public void apply(ScriptStatePacket packet) {
        this.state = packet;
        this.selected = packet.selected;
        if (packet.withScript) {
            this.editor.setValue(packet.script);
        }
        if (selected == null && !autoSelected && !packet.buses.isEmpty()) {
            autoSelected = true;
            selected = packet.buses.get(0).pos();
            send(ScriptActionPacket.LIST, 0, "");
        }
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
        if (state != null && button == 0) {
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
        if (state != null && mouseX >= left + 6 && mouseX < left + 106) {
            int max = Math.max(0, state.buses.size() - LIST_ROWS);
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
        GuiStyle.window(g, left, top, W, H, this.title, led);

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
                String name = info.pos().getX() + " " + info.pos().getY() + " " + info.pos().getZ();
                g.drawString(this.font, this.font.plainSubstrByWidth(name, 84), left + 19, ry + 3, GuiStyle.TEXT_TITLE, false);
            }
            if (state.buses.isEmpty()) {
                g.drawString(this.font, this.font.plainSubstrByWidth(
                        Component.translatable("gui.simple_bytestorage_scripting.no_buses").getString(), 92),
                        left + 10, top + 26, GuiStyle.TEXT_MUTED, false);
            }
        }

        // Лог / статус
        GuiStyle.display(g, left + 112, top + 162, 242, 64);
        super.render(g, mouseX, mouseY, partialTick);
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
