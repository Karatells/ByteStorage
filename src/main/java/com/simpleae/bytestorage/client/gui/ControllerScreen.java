package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.item.WirelessTerminalItem;
import com.simpleae.bytestorage.menu.ControllerMenu;
import com.simpleae.bytestorage.network.packet.ControllerQueuePacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
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
    private static final String[] TABS = {"overview", "storage", "crafting", "recipes", "stock", "journal"};

    private static final int JOURNAL_TAB = 5;
    private static final int QUEUE_ROWS = 4;
    private static final int ROW_H = 14;
    private int tab = 0;
    private final List<UiButton> rowButtons = new ArrayList<>();
    private final List<UiButton> stockButtons = new ArrayList<>();
    private UiButton pageButton;
    private UiButton skipButton;
    private final List<UiButton> tabButtons = new ArrayList<>();

    public ControllerScreen(ControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 288;
        this.imageHeight = 270;
    }

    @Override
    protected void init() {
        super.init();
        tabButtons.clear();
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            UiButton b = new UiButton(leftPos + 8 + i * 46, topPos + 20, 45, 14,
                    () -> Component.translatable("gui.simple_bytestorage.controller.tab." + TABS[index]),
                    UiButton.Icon.NONE, () -> { tab = index; menu.setTab(index); });
            tabButtons.add(b);
        }
        rowButtons.clear();
        for (int r = 0; r < QUEUE_ROWS; r++) {
            final int row = r;
            int by = topPos + 42 + 14 + r * ROW_H - 2;
            int bx = leftPos + 10 + 202;
            rowButtons.add(new UiButton(bx, by, 12, 12, () -> Component.literal("+"), UiButton.Icon.NONE,
                    () -> queueAction(row, ControllerQueuePacket.PRIORITY_UP)).tooltip(Component.translatable("gui.simple_bytestorage.controller.queue.prio_up")));
            rowButtons.add(new UiButton(bx + 14, by, 12, 12, () -> Component.literal("-"), UiButton.Icon.NONE,
                    () -> queueAction(row, ControllerQueuePacket.PRIORITY_DOWN)).tooltip(Component.translatable("gui.simple_bytestorage.controller.queue.prio_down")));
            rowButtons.add(new UiButton(bx + 28, by, 16, 12, () -> Component.literal(isPaused(row) ? ">" : "||"), UiButton.Icon.NONE,
                    () -> queueAction(row, ControllerQueuePacket.TOGGLE_PAUSE)).tooltip(Component.translatable("gui.simple_bytestorage.controller.queue.pause")));
            rowButtons.add(new UiButton(bx + 46, by, 12, 12, () -> Component.empty(), UiButton.Icon.CROSS,
                    () -> queueAction(row, ControllerQueuePacket.CANCEL)).tooltip(Component.translatable("gui.simple_bytestorage.controller.queue.cancel")));
        }
        stockButtons.clear();
        String[] labels = {"-100", "-10", "-1", "+1", "+10", "+100"};
        for (int r = 0; r < ControllerMenu.STOCK_ROWS; r++) {
            for (int a = 0; a < labels.length; a++) {
                final int row = r;
                final int action = a;
                final String label = labels[a];
                stockButtons.add(new UiButton(leftPos + 10 + 68 + a * 25, topPos + ControllerMenu.STOCK_Y + r * 18 + 1, 24, 14,
                        () -> Component.literal(label), UiButton.Icon.NONE, () -> {
                            int rule = menu.getStockPage() * ControllerMenu.STOCK_ROWS + row;
                            if (this.minecraft != null && this.minecraft.gameMode != null) {
                                this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                                        ControllerMenu.STOCK_BUTTON_BASE + rule * ControllerMenu.STOCK_DELTAS.length + action);
                            }
                        }).enabledWhen(() -> menu.slots.get(ControllerMenu.STOCK_START
                        + menu.getStockPage() * ControllerMenu.STOCK_ROWS + row).hasItem()));
            }
        }
        pageButton = new UiButton(leftPos + 262, topPos + ControllerMenu.STOCK_Y + 1, 18, 14,
                () -> Component.literal(String.valueOf(menu.getStockPage() + 1)), UiButton.Icon.NONE,
                () -> menu.setStockPage(1 - menu.getStockPage()))
                .tooltip(Component.translatable("gui.simple_bytestorage.controller.stock.page"));
        journalUp = new UiButton(leftPos + 262, topPos + 44, 18, 14, () -> Component.literal("^"), UiButton.Icon.NONE,
                () -> journalOffset = Math.max(0, journalOffset - JOURNAL_PAGE))
                .tooltip(Component.translatable("gui.simple_bytestorage.controller.journal.newer"));
        journalDown = new UiButton(leftPos + 262, topPos + 98, 18, 14, () -> Component.literal("v"), UiButton.Icon.NONE,
                () -> journalOffset += JOURNAL_PAGE)
                .tooltip(Component.translatable("gui.simple_bytestorage.controller.journal.older"));
        skipButton = new UiButton(leftPos + 10, topPos + 103, 264, 13,
                () -> Component.translatable(menu.isSkipDecompress() ? "gui.simple_bytestorage.controller.recipes.skip.on" : "gui.simple_bytestorage.controller.recipes.skip.off"),
                UiButton.Icon.NONE, () -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                }).tooltip(Component.translatable("gui.simple_bytestorage.controller.recipes.skip.tip"));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (UiButton b : tabButtons) {
            if (b.mouseClicked(mx, my, button)) {
                return true;
            }
        }
        for (int i = 0; i < visibleRows() * 4; i++) {
            if (rowButtons.get(i).mouseClicked(mx, my, button)) {
                return true;
            }
        }
        if (tab == JOURNAL_TAB && (journalUp.mouseClicked(mx, my, button) || journalDown.mouseClicked(mx, my, button))) {
            return true;
        }
        if (tab == ControllerMenu.STOCK_TAB) {
            if (pageButton.mouseClicked(mx, my, button)) {
                return true;
            }
            for (UiButton b : stockButtons) {
                if (b.mouseClicked(mx, my, button)) {
                    return true;
                }
            }
        }
        if (tab == ControllerMenu.RECIPES_TAB && skipButton.mouseClicked(mx, my, button)) {
            return true;
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
                g.fill(leftPos + 8 + i * 46, topPos + 34, leftPos + 53 + i * 46, topPos + 36, GuiStyle.ACCENT);
            }
        }
        for (int i = 0; i < visibleRows() * 4; i++) {
            rowButtons.get(i).render(g, this.font, mouseX, mouseY);
        }
        if (tab == JOURNAL_TAB) {
            journalUp.render(g, this.font, mouseX, mouseY);
            journalDown.render(g, this.font, mouseX, mouseY);
            journalUp.renderTooltip(g, this.font, mouseX, mouseY);
            journalDown.renderTooltip(g, this.font, mouseX, mouseY);
        }
        if (tab == ControllerMenu.STOCK_TAB) {
            pageButton.render(g, this.font, mouseX, mouseY);
            pageButton.renderTooltip(g, this.font, mouseX, mouseY);
            for (UiButton b : stockButtons) {
                b.render(g, this.font, mouseX, mouseY);
            }
        }
        if (tab == ControllerMenu.RECIPES_TAB) {
            skipButton.render(g, this.font, mouseX, mouseY);
            skipButton.renderTooltip(g, this.font, mouseX, mouseY);
        }
        for (int i = 0; i < visibleRows() * 4; i++) {
            rowButtons.get(i).renderTooltip(g, this.font, mouseX, mouseY);
        }
        this.renderTooltip(g, mouseX, mouseY);
    }

    private boolean isPaused(int row) {
        List<NetworkStats.QueueRow> rows = menu.getStats().queue;
        return row < rows.size() && rows.get(row).paused();
    }

    private int visibleRows() {
        return tab == 2 ? Math.min(QUEUE_ROWS, menu.getStats().queue.size()) : 0;
    }

    private static String n(int v) {
        return FMT.format(v);
    }

    private void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(this.font, this.font.plainSubstrByWidth(s, 268), x, y, color, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        NetworkStats s = menu.getStats();
        int led = s.state == 1 ? GuiStyle.LED_GOOD : (s.state == 2 ? GuiStyle.LED_BAD : GuiStyle.LED_WARN);
        GuiStyle.window(g, x, y, imageWidth, imageHeight, this.title, led);

        GuiStyle.display(g, x + 6, y + 38, 276, 80);
        switch (tab) {
            case 0 -> drawOverview(g, s, x + 10, y + 42);
            case 1 -> drawStorage(g, s, x + 10, y + 42);
            case 2 -> drawCrafting(g, s, x + 10, y + 42);
            case 3 -> drawRecipes(g, x, y);
            case 4 -> drawStock(g, s, x, y);
            default -> drawJournal(g, s, x + 10, y + 42);
        }

        // Слот привязки беспроводного терминала
        GuiStyle.panel(g, x + 6, y + 120, 276, 26);
        GuiStyle.menuSlot(g, x, y, menu.slots.get(0), GuiStyle.SLOT_CARD, GuiStyle.ACCENT);
        ItemStack w = menu.getWirelessStack();
        if (w.getItem() instanceof WirelessTerminalItem) {
            int energy = WirelessTerminalItem.getEnergy(w);
            text(g, Component.translatable("gui.simple_bytestorage.controller.wireless.bound").getString(), x + 30, y + 124, GuiStyle.TEXT_GOOD);
            GuiStyle.meter(g, x + 30, y + 136, 100, 6, energy / (float) WirelessTerminalItem.CAPACITY);
            text(g, n(energy) + " / " + n(WirelessTerminalItem.CAPACITY) + " FE", x + 136, y + 135, GuiStyle.TEXT_LABEL);
        } else {
            text(g, Component.translatable("gui.simple_bytestorage.controller.wireless.empty").getString(), x + 30, y + 124, GuiStyle.TEXT_MUTED);
            text(g, Component.translatable("gui.simple_bytestorage.controller.wireless.hint").getString(), x + 30, y + 135, GuiStyle.TEXT_MUTED);
        }

        // Слот улучшения «Библиотека рецептов»
        GuiStyle.panel(g, x + 6, y + 148, 276, 26);
        GuiStyle.menuSlot(g, x, y, menu.slots.get(1), GuiStyle.SLOT_CARD, GuiStyle.TEXT_WARN);
        if (menu.getStats().libraryRecipes > 0) {
            text(g, Component.translatable("gui.simple_bytestorage.controller.library.active").getString(), x + 30, y + 152, GuiStyle.TEXT_GOOD);
            text(g, Component.translatable("gui.simple_bytestorage.controller.library.count", menu.getStats().libraryRecipes).getString(), x + 30, y + 163, GuiStyle.TEXT_LABEL);
        } else {
            text(g, Component.translatable("gui.simple_bytestorage.controller.library.empty").getString(), x + 30, y + 152, GuiStyle.TEXT_MUTED);
            text(g, Component.translatable("gui.simple_bytestorage.controller.library.hint").getString(), x + 30, y + 163, GuiStyle.TEXT_MUTED);
        }

        GuiStyle.inventoryLabel(g, this.playerInventoryTitle, x + ControllerMenu.PLAYER_INV_X, y + ControllerMenu.PLAYER_INV_Y - 10);
        GuiStyle.playerSlots(g, x, y, menu.slots, ControllerMenu.PLAYER_START);
    }

    private static final int JOURNAL_PAGE = 5;
    private int journalOffset = 0;
    private UiButton journalUp;
    private UiButton journalDown;

    private static String formatAge(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        return seconds < 3600 ? (seconds / 60) + "m" : (seconds / 3600) + "h";
    }

    private void drawJournal(GuiGraphics g, NetworkStats s, int x, int y) {
        if (s.journal.isEmpty()) {
            text(g, Component.translatable("gui.simple_bytestorage.controller.journal.empty").getString(), x, y + 4, GuiStyle.TEXT_MUTED);
            return;
        }
        journalOffset = Math.max(0, Math.min(journalOffset, Math.max(0, s.journal.size() - JOURNAL_PAGE)));
        for (int i = 0; i < JOURNAL_PAGE && journalOffset + i < s.journal.size(); i++) {
            NetworkStats.JournalRow row = s.journal.get(journalOffset + i);
            int cy = y + 2 + i * 14;
            g.renderItem(row.item(), x, cy - 3);
            int color = switch (row.type()) {
                case 1 -> GuiStyle.TEXT_GOOD;
                case 4 -> GuiStyle.TEXT_BAD;
                case 5 -> GuiStyle.TEXT_WARN;
                default -> GuiStyle.TEXT_LABEL;
            };
            text(g, Component.translatable("gui.simple_bytestorage.journal.type." + row.type()).getString()
                    + " x" + n(row.amount()), x + 20, cy + 1, color);
            String age = formatAge(row.ageSeconds());
            g.drawString(this.font, age, x + 236 - this.font.width(age), cy + 1, GuiStyle.TEXT_MUTED, false);
        }
    }

    private void drawStock(GuiGraphics g, NetworkStats s, int x, int y) {
        int page = menu.getStockPage();
        for (int row = 0; row < ControllerMenu.STOCK_ROWS; row++) {
            int rule = page * ControllerMenu.STOCK_ROWS + row;
            net.minecraft.world.inventory.Slot slot = menu.slots.get(ControllerMenu.STOCK_START + rule);
            GuiStyle.menuSlot(g, x, y, slot, GuiStyle.SLOT_CARD);
            if (slot.hasItem()) {
                int color = switch (s.stockState[rule]) {
                    case 1 -> GuiStyle.TEXT_GOOD;
                    case 2 -> GuiStyle.TEXT_TITLE;
                    case 3 -> GuiStyle.TEXT_BAD;
                    default -> GuiStyle.TEXT_MUTED;
                };
                int ty = y + slot.y + 4;
                String t = n(s.stockHave[rule]) + "/" + n(s.stockMin[rule]) + (s.stockState[rule] == 3 ? " !" : "");
                g.drawString(this.font, this.font.plainSubstrByWidth(t, 62), x + 32, ty, color, false);
            }
        }
        boolean anyEmpty = true;
        for (int i = 0; i < NetworkControllerBlockEntity.STOCK_RULES; i++) {
            if (menu.slots.get(ControllerMenu.STOCK_START + i).hasItem()) {
                anyEmpty = false;
            }
        }
        if (anyEmpty) {
            text(g, Component.translatable("gui.simple_bytestorage.controller.stock.hint").getString(), x + 32, y + 48, GuiStyle.TEXT_MUTED);
        }
    }

    private void drawRecipes(GuiGraphics g, int x, int y) {
        text(g, Component.translatable("gui.simple_bytestorage.controller.recipes.blacklist").getString(), x + 10, y + 42, GuiStyle.TEXT_BAD);
        text(g, Component.translatable("gui.simple_bytestorage.controller.recipes.prefs").getString(), x + 10, y + 72, GuiStyle.TEXT_GOOD);
        for (int i = ControllerMenu.BLACKLIST_START; i < ControllerMenu.STOCK_START; i++) {
            GuiStyle.menuSlot(g, x, y, menu.slots.get(i), GuiStyle.SLOT_CARD);
        }
        if (!menu.slots.get(1).hasItem()) {
            text(g, Component.translatable("gui.simple_bytestorage.controller.recipes.need_library").getString(), x + 10, y + 105, GuiStyle.TEXT_WARN);
        }
    }

    private void drawOverview(GuiGraphics g, NetworkStats s, int x, int y) {
        String[] states = {"offline", "online", "conflict"};
        int stateColor = s.state == 1 ? GuiStyle.TEXT_GOOD : (s.state == 2 ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_WARN);
        text(g, Component.translatable("gui.simple_bytestorage.controller.status").getString() + ": "
                + Component.translatable("gui.simple_bytestorage.controller.state." + states[Math.min(2, Math.max(0, s.state))]).getString()
                + "   " + Component.translatable("gui.simple_bytestorage.controller.nodes", s.nodeTotal).getString(), x, y, stateColor);
        text(g, Component.translatable("gui.simple_bytestorage.controller.energy").getString() + ": " + n(s.energy) + " / " + n(s.maxEnergy)
                + " FE   (-" + n(s.drain) + " FE/t)", x, y + 11, GuiStyle.TEXT_TITLE);
        GuiStyle.meter(g, x, y + 22, 264, 6, s.maxEnergy == 0 ? 0 : s.energy / (float) s.maxEnergy);
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
        GuiStyle.meter(g, x, y + 11, 264, 6, frac);
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
        if (s.queue.isEmpty()) {
            text(g, Component.translatable("gui.simple_bytestorage.controller.queue.empty").getString(), x, y + 16, GuiStyle.TEXT_MUTED);
            return;
        }
        int i = 0;
        for (NetworkStats.QueueRow q : s.queue) {
            if (i >= QUEUE_ROWS) break;
            int cy = y + 14 + i * ROW_H;
            g.renderItem(q.output(), x, cy - 3);
            int color = q.paused() ? GuiStyle.TEXT_WARN : (q.active() ? GuiStyle.TEXT_GOOD : GuiStyle.TEXT_LABEL);
            text(g, "x" + q.crafts(), x + 18, cy + 2, color);
            GuiStyle.progress(g, x + 52, cy + 3, 50, 5, q.percent() / 100f);
            text(g, q.paused() ? Component.translatable("gui.simple_bytestorage.controller.queue.paused").getString()
                    : (q.etaSeconds() > 0 ? formatEta(q.etaSeconds()) : "--"), x + 106, cy + 2, color);
            text(g, (q.priority() > 0 ? "P+" : "P") + q.priority(), x + 138, cy + 2, q.priority() != 0 ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_MUTED);
            i++;
        }
        if (s.queue.size() > QUEUE_ROWS) {
            text(g, "+" + (s.queue.size() - QUEUE_ROWS) + " ...", x, y + 14 + QUEUE_ROWS * ROW_H, GuiStyle.TEXT_MUTED);
        }
    }

    private static String formatEta(int seconds) {
        return com.simpleae.bytestorage.util.QueueOrder.formatEta(seconds);
    }

    private void queueAction(int row, int action) {
        List<NetworkStats.QueueRow> rows = menu.getStats().queue;
        if (row < rows.size()) {
            SimpleByteStoragePackets.sendToServer(new ControllerQueuePacket(action, rows.get(row).patternId()));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }
}
