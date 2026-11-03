package com.simpleae.bytestorage.scripting.client;

import com.simpleae.bytestorage.client.gui.GuiStyle;
import com.simpleae.bytestorage.client.gui.UiButton;
import com.simpleae.bytestorage.scripting.net.BusActionPacket;
import com.simpleae.bytestorage.scripting.net.BusStatePacket;
import com.simpleae.bytestorage.scripting.net.ScriptingPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Інтерфейс Логічної шини: ідентифікатор ЦІЄЇ шини та список усіх ідентифікаторів мережі. */
public class LogicBusScreen extends Screen {

    private static final int W = 232;
    private static final int H = 178;
    private static final int ID_ROWS = 10;

    private final BlockPos pos;
    private int left;
    private int top;
    private EditBox idBox;
    private final List<UiButton> buttons = new ArrayList<>();

    private BusStatePacket state;
    private String lastServerId = null;
    private int listScroll = 0;
    private int tickCounter = 0;

    public LogicBusScreen(BlockPos pos) {
        super(Component.translatable("block.simple_bytestorage_scripting.logic_bus"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        this.left = (this.width - W) / 2;
        this.top = (this.height - H) / 2;

        String previous = this.idBox != null ? this.idBox.getValue() : "";
        this.idBox = new EditBox(this.font, left + 122, top + 34, 104, 16, Component.empty());
        this.idBox.setMaxLength(32);
        this.idBox.setFilter(text -> text.matches("[A-Za-z0-9_.\\-]*"));
        this.idBox.setHint(Component.translatable("gui.simple_bytestorage_scripting.id.hint"));
        this.idBox.setValue(previous);
        this.addRenderableWidget(this.idBox);
        this.setInitialFocus(this.idBox);

        buttons.clear();
        buttons.add(new UiButton(left + 122, top + 54, 50, 16,
                () -> Component.translatable("gui.simple_bytestorage_scripting.id.set"), UiButton.Icon.NONE,
                () -> assign(idBox.getValue()))
                .enabledWhen(this::canAct).primary()
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.id.set.tip")));
        buttons.add(new UiButton(left + 176, top + 54, 50, 16,
                () -> Component.translatable("gui.simple_bytestorage_scripting.id.clear"), UiButton.Icon.NONE,
                () -> {
                    idBox.setValue("");
                    assign("");
                })
                .enabledWhen(this::canAct)
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.id.clear.tip")));

        send(BusActionPacket.REFRESH, "");
    }

    public void apply(BusStatePacket packet) {
        this.state = packet;
        // Поле перезаписуємо лише коли ID на сервері змінився (щоб не псувати введення)
        if (lastServerId == null || !packet.ownId.equals(lastServerId)) {
            idBox.setValue(packet.ownId);
            lastServerId = packet.ownId;
        }
    }

    private boolean canAct() {
        return state != null && state.modEnabled && state.canEdit;
    }

    private void assign(String id) {
        send(BusActionPacket.SET_ID, id);
    }

    private void send(int action, String text) {
        ScriptingPackets.CHANNEL.sendToServer(new BusActionPacket(pos, action, text));
    }

    @Override
    public void tick() {
        super.tick();
        if (++tickCounter % 20 == 0) {
            send(BusActionPacket.REFRESH, "");
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (UiButton b : buttons) {
            if (b.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (state != null && button == 0) {
            int ix = left + 6;
            int iy = top + 34;
            if (mouseX >= ix && mouseX < ix + 106 && mouseY >= iy && mouseY < iy + ID_ROWS * 10) {
                int index = listScroll + (int) ((mouseY - iy) / 10);
                if (index >= 0 && index < state.ids.size()) {
                    String id = state.ids.get(index).id();
                    idBox.setValue(id);
                    if (hasShiftDown() && canAct()) {
                        assign(id); // Shift + клік — призначити одразу
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (state != null && mouseX < left + 116) {
            int max = Math.max(0, state.ids.size() - ID_ROWS);
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        boolean online = state != null && state.online && state.modEnabled;
        int led = state == null ? GuiStyle.LED_WARN : (online ? GuiStyle.LED_GOOD : GuiStyle.LED_BAD);
        GuiStyle.window(g, left, top, W, H, this.title, led);

        // Список ID мережі
        GuiStyle.label(g, Component.translatable("gui.simple_bytestorage_scripting.id.list"), left + 6, top + 22);
        GuiStyle.display(g, left + 5, top + 33, 108, ID_ROWS * 10 + 2);
        if (state != null) {
            if (state.ids.isEmpty()) {
                g.drawString(this.font, this.font.plainSubstrByWidth(
                        Component.translatable("gui.simple_bytestorage_scripting.id.none").getString(), 98),
                        left + 9, top + 37, GuiStyle.TEXT_MUTED, false);
            }
            for (int row = 0; row < ID_ROWS; row++) {
                int index = listScroll + row;
                if (index >= state.ids.size()) {
                    break;
                }
                BusStatePacket.IdCount entry = state.ids.get(index);
                int ry = top + 34 + row * 10;
                boolean mine = entry.id().equals(state.ownId);
                if (mine) {
                    g.fill(left + 6, ry, left + 112, ry + 10, GuiStyle.ACCENT_FAINT);
                }
                String count = "x" + entry.count();
                g.drawString(this.font, this.font.plainSubstrByWidth(entry.id(), 78), left + 9, ry + 1,
                        mine ? GuiStyle.ACCENT : GuiStyle.TEXT_TITLE, false);
                g.drawString(this.font, count, left + 109 - this.font.width(count), ry + 1, GuiStyle.TEXT_MUTED, false);
            }
        }

        // Праворуч: поле ID цієї шини
        GuiStyle.label(g, Component.translatable("gui.simple_bytestorage_scripting.id.field"), left + 122, top + 22);
        super.render(g, mouseX, mouseY, partialTick);
        for (UiButton b : buttons) {
            b.render(g, this.font, mouseX, mouseY);
        }

        // Статус і підказки
        int y = top + 78;
        if (state == null) {
            g.drawString(this.font, Component.translatable("gui.simple_bytestorage_scripting.loading"), left + 122, y,
                    GuiStyle.TEXT_MUTED, false);
        } else {
            String line;
            int color;
            if (!state.modEnabled) {
                line = Component.translatable("gui.simple_bytestorage_scripting.disabled").getString();
                color = GuiStyle.TEXT_BAD;
            } else if (!state.online) {
                line = Component.translatable("gui.simple_bytestorage_scripting.offline").getString();
                color = GuiStyle.TEXT_BAD;
            } else if (!state.canEdit) {
                line = Component.translatable("gui.simple_bytestorage_scripting.readonly").getString();
                color = GuiStyle.TEXT_WARN;
            } else {
                line = state.status.isEmpty()
                        ? Component.translatable(state.running ? "gui.simple_bytestorage_scripting.status.running"
                                : "gui.simple_bytestorage_scripting.status.idle").getString()
                        : state.status;
                color = state.status.startsWith("ERROR") ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_GOOD;
            }
            for (FormattedCharSequence part : this.font.split(Component.literal(line), 104)) {
                g.drawString(this.font, part, left + 122, y, color, false);
                y += 10;
                if (y > top + 108) {
                    break;
                }
            }
        }
        int hy = top + 112;
        for (FormattedCharSequence part : this.font.split(
                Component.translatable("gui.simple_bytestorage_scripting.id.bus_hint"), 104)) {
            g.drawString(this.font, part, left + 122, hy, GuiStyle.TEXT_MUTED, false);
            hy += 10;
        }
        for (UiButton b : buttons) {
            b.renderTooltip(g, this.font, mouseX, mouseY);
        }
    }
}
