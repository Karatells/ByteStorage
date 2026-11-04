package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.item.WirelessTerminalItem;
import com.simpleae.bytestorage.menu.ControllerMenu;
import com.simpleae.bytestorage.util.NetworkStats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Экран Контроллера: вкладки Обзор / Хранилище / Крафт и слот привязки беспроводного терминала. */
public class ControllerScreen extends AbstractContainerScreen<ControllerMenu> {

    private static final NumberFormat FMT = NumberFormat.getInstance(Locale.US);
    private static final String[] TABS = {"overview", "storage", "crafting"};

    private int tab = 0;
    private final List<UiButton> tabButtons = new ArrayList<>();

    public ControllerScreen(ControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 236;
    }

    @Override
    protected void init() {
        super.init();
        tabButtons.clear();
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            UiButton b = new UiButton(leftPos + 8 + i * 80, topPos + 20, 78, 14,
                    () -> Component.translatable("gui.simple_bytestorage.controller.tab." + TABS[index]),
                    UiButton.Icon.NONE, () -> tab = index);
            tabButtons.add(b);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (UiButton b : tabButtons) {
            if (b.mouseClicked(mx, my, button)) {
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        for (int i = 0; i < tabButtons.size(); i++) {
            UiButton b = tabButtons.get(i);
            b.render(g, this.font, mouseX, mouseY);
            if (i == tab) {
                g.fill(leftPos + 8 + i * 80, topPos + 34, leftPos + 86 + i * 80, topPos + 36, GuiStyle.ACCENT);
            }
        }
        this.renderTooltip(g, mouseX, mouseY);
    }

    private static String n(int v) {
        return FMT.format(v);
    }

    private void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(this.font, this.font.plainSubstrByWidth(s, 236), x, y, color, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        NetworkStats s = menu.getStats();
        int led = s.state == 1 ? GuiStyle.LED_GOOD : (s.state == 2 ? GuiStyle.LED_BAD : GuiStyle.LED_WARN);
        GuiStyle.window(g, x, y, imageWidth, imageHeight, this.title, led);

        GuiStyle.display(g, x + 6, y + 38, 244, 84);
        switch (tab) {
            case 0 -> drawOverview(g, s, x + 10, y + 42);
            case 1 -> drawStorage(g, s, x + 10, y + 42);
            default -> drawCrafting(g, s, x + 10, y + 42);
        }

        // Слот привязки беспроводного терминала
        GuiStyle.panel(g, x + 6, y + 124, 244, 26);
        GuiStyle.menuSlot(g, x, y, menu.slots.get(0), GuiStyle.SLOT_CARD, GuiStyle.ACCENT);
        ItemStack w = menu.getWirelessStack();
        if (w.getItem() instanceof WirelessTerminalItem) {
            int energy = WirelessTerminalItem.getEnergy(w);
            text(g, Component.translatable("gui.simple_bytestorage.controller.wireless.bound").getString(), x + 30, y + 128, GuiStyle.TEXT_GOOD);
            GuiStyle.meter(g, x + 30, y + 140, 150, 6, energy / (float) WirelessTerminalItem.CAPACITY);
            text(g, n(energy) + " / " + n(WirelessTerminalItem.CAPACITY) + " FE", x + 184, y + 139, GuiStyle.TEXT_LABEL);
        } else {
            text(g, Component.translatable("gui.simple_bytestorage.controller.wireless.empty").getString(), x + 30, y + 128, GuiStyle.TEXT_MUTED);
            text(g, Component.translatable("gui.simple_bytestorage.controller.wireless.hint").getString(), x + 30, y + 139, GuiStyle.TEXT_MUTED);
        }

        GuiStyle.inventoryLabel(g, this.playerInventoryTitle, x + ControllerMenu.PLAYER_INV_X, y + ControllerMenu.PLAYER_INV_Y - 10);
        GuiStyle.playerSlots(g, x, y, menu.slots, ControllerMenu.PLAYER_START);
    }

    private void drawOverview(GuiGraphics g, NetworkStats s, int x, int y) {
        String[] states = {"offline", "online", "conflict"};
        int stateColor = s.state == 1 ? GuiStyle.TEXT_GOOD : (s.state == 2 ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_WARN);
        text(g, Component.translatable("gui.simple_bytestorage.controller.status").getString() + ": "
                + Component.translatable("gui.simple_bytestorage.controller.state." + states[Math.min(2, Math.max(0, s.state))]).getString()
                + "   " + Component.translatable("gui.simple_bytestorage.controller.nodes", s.nodeTotal).getString(), x, y, stateColor);
        text(g, Component.translatable("gui.simple_bytestorage.controller.energy").getString() + ": " + n(s.energy) + " / " + n(s.maxEnergy)
                + " FE   (-" + n(s.drain) + " FE/t)", x, y + 11, GuiStyle.TEXT_TITLE);
        GuiStyle.meter(g, x, y + 22, 232, 6, s.maxEnergy == 0 ? 0 : s.energy / (float) s.maxEnergy);
        int row = y + 32;
        int shown = 0;
        for (NetworkStats.BlockCount b : s.blocks) {
            if (shown >= 4) break;
            text(g, Component.translatable(b.nameKey()).getString() + " x" + b.count(), x + (shown % 2) * 118, row + (shown / 2) * 11, GuiStyle.TEXT_LABEL);
            shown++;
        }
        if (s.blocks.size() > 4) {
            text(g, "+" + (s.blocks.size() - 4) + " ...", x, row + 22, GuiStyle.TEXT_MUTED);
        }
    }

    private void drawStorage(GuiGraphics g, NetworkStats s, int x, int y) {
        float frac = s.capacityBytes == 0 ? 0 : s.usedBytes / (float) s.capacityBytes;
        text(g, Component.translatable("gui.simple_bytestorage.controller.disks").getString() + ": " + n(s.usedBytes) + " / " + n(s.capacityBytes)
                + " B  (" + Math.round(frac * 100) + "%)  " + Component.translatable("gui.simple_bytestorage.controller.types", s.itemTypes).getString(), x, y, GuiStyle.TEXT_TITLE);
        GuiStyle.meter(g, x, y + 11, 232, 6, frac);
        int row = y + 21;
        int i = 0;
        for (NetworkStats.ItemCount ic : s.topItems) {
            if (i >= 8) break;
            int cx = x + (i % 2) * 118;
            int cy = row + (i / 2) * 14;
            g.renderItem(ic.stack(), cx, cy - 3);
            text(g, n(ic.count()), cx + 18, cy + 1, GuiStyle.TEXT_LABEL);
            i++;
        }
    }

    private void drawCrafting(GuiGraphics g, NetworkStats s, int x, int y) {
        text(g, Component.translatable("gui.simple_bytestorage.controller.patterns", s.patterns).getString() + "   "
                + Component.translatable("gui.simple_bytestorage.controller.assemblers", s.assemblersBusy, s.assemblers).getString() + "   "
                + Component.translatable("gui.simple_bytestorage.controller.buses", s.buses, s.busJobs).getString(), x, y, GuiStyle.TEXT_TITLE);
        int total = Math.max(1, s.assemblers);
        GuiStyle.progress(g, x, y + 11, 232, 6, s.assemblersBusy / (float) total);
        if (s.queue.isEmpty()) {
            text(g, Component.translatable("gui.simple_bytestorage.controller.queue.empty").getString(), x, y + 24, GuiStyle.TEXT_MUTED);
            return;
        }
        int i = 0;
        for (NetworkStats.QueueRow q : s.queue) {
            int cx = x + (i % 2) * 118;
            int cy = y + 21 + (i / 2) * 14;
            g.renderItem(q.output(), cx, cy - 3);
            text(g, "x" + q.crafts() + (q.active() ? " *" : ""), cx + 18, cy + 1, q.active() ? GuiStyle.TEXT_GOOD : GuiStyle.TEXT_LABEL);
            i++;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }
}
