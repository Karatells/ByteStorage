package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.item.WirelessTerminalItem;
import com.simpleae.bytestorage.menu.ControllerMenu;
import com.simpleae.bytestorage.network.packet.ControllerQueuePacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.util.NetworkStats;
import com.simpleae.bytestorage.util.StatsHistory;
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
    private static final String[] TABS = {"overview", "storage", "crafting", "recipes", "stock", "journal", "stats"};
    private static final String[] TAB_ICONS = {"monitor", "box", "hammer", "grid", "shield", "doc", "chart"};
    private static final int STATS_TAB = 6;
    /** Верх области содержимого вкладок (от верха окна). */
    private static final int CY = 26;

    private static final int JOURNAL_TAB = 5;
    private static final int QUEUE_ROWS = 4;
    private static final int ROW_H = 14;
    private int tab = 0;
    private final List<UiButton> rowButtons = new ArrayList<>();
    private final List<UiButton> stockButtons = new ArrayList<>();
    private UiButton pageButton;
    private UiButton skipButton;
    private UiButton scheduleButton;
    private final List<UiButton> tabButtons = new ArrayList<>();

    public ControllerScreen(ControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = ControllerMenu.TAB_COL + 288;
        this.imageHeight = 270;
    }

    @Override
    protected void init() {
        super.init();
        tabButtons.clear();
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            UiButton b = new UiButton(leftPos + 8, topPos + CY + i * 21, ControllerMenu.TAB_COL - 14, 19,
                    () -> Component.translatable("gui.simple_bytestorage.controller.tab." + TABS[index]),
                    UiButton.Icon.NONE, () -> { tab = index; menu.setTab(index); });
            tabButtons.add(b);
        }
        final int ox = leftPos + ControllerMenu.TAB_COL;
        rowButtons.clear();
        for (int r = 0; r < QUEUE_ROWS; r++) {
            final int row = r;
            int by = topPos + CY + 14 + r * ROW_H - 2;
            int bx = ox + 10 + 202;
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
                stockButtons.add(new UiButton(ox + 10 + 68 + a * 25, topPos + ControllerMenu.STOCK_Y + r * 18 + 1, 24, 14,
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
        pageButton = new UiButton(ox + 262, topPos + ControllerMenu.STOCK_Y + 1, 18, 14,
                () -> Component.literal(String.valueOf(menu.getStockPage() + 1)), UiButton.Icon.NONE,
                () -> menu.setStockPage(1 - menu.getStockPage()))
                .tooltip(Component.translatable("gui.simple_bytestorage.controller.stock.page"));
        journalUp = new UiButton(ox + 262, topPos + CY + 2, 18, 14, () -> Component.literal("^"), UiButton.Icon.NONE,
                () -> journalOffset = Math.max(0, journalOffset - JOURNAL_PAGE))
                .tooltip(Component.translatable("gui.simple_bytestorage.controller.journal.newer"));
        journalDown = new UiButton(ox + 262, topPos + CY + 70, 18, 14, () -> Component.literal("v"), UiButton.Icon.NONE,
                () -> journalOffset += JOURNAL_PAGE)
                .tooltip(Component.translatable("gui.simple_bytestorage.controller.journal.older"));
        scheduleButton = new UiButton(ox + 10, topPos + 101, 264, 12,
                () -> Component.translatable("gui.simple_bytestorage.controller.stock.mode." + menu.getStockMode())
                        .append(menu.isStockActive() ? Component.empty()
                                : Component.literal("  ").append(Component.translatable("gui.simple_bytestorage.controller.stock.mode.paused"))),
                UiButton.Icon.NONE, () -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                    }
                }).tooltip(Component.translatable("gui.simple_bytestorage.controller.stock.mode.tip"));
        skipButton = new UiButton(ox + 10, topPos + 97, 264, 13,
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
            if (pageButton.mouseClicked(mx, my, button) || scheduleButton.mouseClicked(mx, my, button)) {
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
            drawTab(g, i, mouseX, mouseY);
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
            NetworkStats st = menu.getStats();
            for (int row = 0; row < ControllerMenu.STOCK_ROWS; row++) {
                int rule = menu.getStockPage() * ControllerMenu.STOCK_ROWS + row;
                int rx = leftPos + ControllerMenu.TAB_COL + 10 + 22;
                int ry = topPos + ControllerMenu.STOCK_Y + row * 18;
                if (st.stockState[rule] == 3 && st.stockReason[rule] != null
                        && mouseX >= rx && mouseX < rx + 46 && mouseY >= ry && mouseY < ry + 16) {
                    g.renderTooltip(this.font, st.stockReason[rule], mouseX, mouseY);
                }
            }
            pageButton.render(g, this.font, mouseX, mouseY);
            pageButton.renderTooltip(g, this.font, mouseX, mouseY);
            scheduleButton.render(g, this.font, mouseX, mouseY);
            scheduleButton.renderTooltip(g, this.font, mouseX, mouseY);
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

    /** Вкладка в левой колонке: иконка + название, активная подсвечена бирюзовой рамкой. */
    private void drawTab(GuiGraphics g, int i, int mouseX, int mouseY) {
        int bx = leftPos + 8;
        int by = topPos + CY + i * 21;
        int bw = ControllerMenu.TAB_COL - 14;
        boolean active = i == tab;
        boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 19;
        int border = active ? GuiStyle.ACCENT : (hover ? GuiStyle.ACCENT_DIM : 0xFF2F353E);
        int fill = active ? 0xFF15303C : (hover ? 0xFF2B3C4C : 0xFF222831);
        g.fill(bx, by, bx + bw, by + 19, border);
        g.fill(bx + 1, by + 1, bx + bw - 1, by + 18, fill);
        if (active) {
            g.fill(bx - 1, by + 2, bx, by + 17, GuiStyle.ACCENT_FAINT);
            g.fill(bx + bw, by + 2, bx + bw + 1, by + 17, GuiStyle.ACCENT_FAINT);
        }
        PixelIcons.draw(g, TAB_ICONS[i], bx + 4, by + 5, active ? GuiStyle.ACCENT : GuiStyle.TEXT_LABEL,
                active ? 0xFFFFFFFF : GuiStyle.TEXT_TITLE);
        String name = Component.translatable("gui.simple_bytestorage.controller.tab." + TABS[i]).getString();
        g.drawString(this.font, name, bx + 17, by + 6, active ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_LABEL, false);
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
        int x = leftPos + ControllerMenu.TAB_COL; // начало области содержимого справа от колонки вкладок
        int y = topPos;
        NetworkStats s = menu.getStats();
        int stateColor = s.state == 1 ? GuiStyle.LED_GOOD : (s.state == 2 ? GuiStyle.LED_BAD : GuiStyle.LED_WARN);
        GuiStyle.window(g, leftPos, y, imageWidth, imageHeight, this.title, stateColor, "network");

        // Колонка вкладок слева
        GuiStyle.panel(g, leftPos + 4, y + 22, ControllerMenu.TAB_COL - 8, TABS.length * 21 + 3);

        if (tab == 0) {
            drawOverview(g, s, x, y);
        } else {
            GuiStyle.display(g, x + 6, y + 22, 276, 92);
            switch (tab) {
                case 1 -> drawStorage(g, s, x + 10, y + CY);
                case 2 -> drawCrafting(g, s, x + 10, y + CY);
                case 3 -> drawRecipes(g, x, y);
                case 4 -> drawStock(g, s, x, y);
                case STATS_TAB -> drawHistory(g, s, x + 10, y + CY);
                default -> drawJournal(g, s, x + 10, y + CY);
            }
        }

        // Слот привязки беспроводного терминала
        GuiStyle.panel(g, x + 6, y + 118, 276, 26);
        GuiStyle.menuSlot(g, leftPos, y, menu.slots.get(0), GuiStyle.SLOT_CARD, GuiStyle.ACCENT);
        ItemStack w = menu.getWirelessStack();
        if (w.getItem() instanceof WirelessTerminalItem) {
            int energy = WirelessTerminalItem.getEnergy(w);
            text(g, Component.translatable("gui.simple_bytestorage.controller.wireless.bound").getString(), x + 30, y + 122, GuiStyle.TEXT_GOOD);
            GuiStyle.meter(g, x + 30, y + 134, 100, 6, energy / (float) WirelessTerminalItem.CAPACITY);
            text(g, n(energy) + " / " + n(WirelessTerminalItem.CAPACITY) + " FE", x + 136, y + 133, GuiStyle.TEXT_LABEL);
        } else {
            text(g, Component.translatable("gui.simple_bytestorage.controller.wireless.empty").getString(), x + 30, y + 122, GuiStyle.TEXT_TITLE);
            text(g, Component.translatable("gui.simple_bytestorage.controller.wireless.hint").getString(), x + 30, y + 133, GuiStyle.TEXT_MUTED);
        }

        // Слот улучшения «Библиотека рецептов»
        boolean library = menu.getStats().libraryRecipes > 0;
        GuiStyle.panel(g, x + 6, y + 148, 276, 26);
        if (library) {
            int c = GuiStyle.TEXT_WARN;
            g.fill(x + 6, y + 148, x + 282, y + 149, c);
            g.fill(x + 6, y + 173, x + 282, y + 174, c);
            g.fill(x + 6, y + 148, x + 7, y + 174, c);
            g.fill(x + 281, y + 148, x + 282, y + 174, c);
        }
        GuiStyle.menuSlot(g, leftPos, y, menu.slots.get(1), GuiStyle.SLOT_CARD, GuiStyle.TEXT_WARN);
        if (library) {
            text(g, Component.translatable("gui.simple_bytestorage.controller.library.active").getString(), x + 30, y + 152, GuiStyle.TEXT_GOOD);
            text(g, Component.translatable("gui.simple_bytestorage.controller.library.count", menu.getStats().libraryRecipes).getString(), x + 30, y + 163, GuiStyle.TEXT_LABEL);
        } else {
            text(g, Component.translatable("gui.simple_bytestorage.controller.library.empty").getString(), x + 30, y + 152, GuiStyle.TEXT_MUTED);
            text(g, Component.translatable("gui.simple_bytestorage.controller.library.hint").getString(), x + 30, y + 163, GuiStyle.TEXT_MUTED);
        }

        GuiStyle.inventoryLabel(g, this.playerInventoryTitle, leftPos + ControllerMenu.PLAYER_INV_X, y + ControllerMenu.PLAYER_INV_Y - 10);
        GuiStyle.playerSlots(g, leftPos, y, menu.slots, ControllerMenu.PLAYER_START);
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
                case 5, 6, 8 -> GuiStyle.TEXT_WARN;
                case 7 -> GuiStyle.TEXT_TITLE;
                default -> GuiStyle.TEXT_LABEL;
            };
            String label = row.text().isEmpty()
                    ? Component.translatable("gui.simple_bytestorage.journal.type." + row.type()).getString()
                    : row.text();
            text(g, this.font.plainSubstrByWidth(label, 190) + (row.amount() > 1 || row.text().isEmpty() ? " x" + n(row.amount()) : ""),
                    x + 20, cy + 1, color);
            String age = formatAge(row.ageSeconds());
            g.drawString(this.font, age, x + 236 - this.font.width(age), cy + 1, GuiStyle.TEXT_MUTED, false);
        }
    }

    private void drawStock(GuiGraphics g, NetworkStats s, int x, int y) {
        int page = menu.getStockPage();
        for (int row = 0; row < ControllerMenu.STOCK_ROWS; row++) {
            int rule = page * ControllerMenu.STOCK_ROWS + row;
            net.minecraft.world.inventory.Slot slot = menu.slots.get(ControllerMenu.STOCK_START + rule);
            GuiStyle.menuSlot(g, x - ControllerMenu.TAB_COL, y, slot, GuiStyle.SLOT_CARD);
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
            text(g, Component.translatable("gui.simple_bytestorage.controller.stock.hint").getString(), x + 32, y + 32, GuiStyle.TEXT_MUTED);
        }
    }

    private void drawRecipes(GuiGraphics g, int x, int y) {
        text(g, Component.translatable("gui.simple_bytestorage.controller.recipes.blacklist").getString(), x + 10, y + 28, GuiStyle.TEXT_BAD);
        text(g, Component.translatable("gui.simple_bytestorage.controller.recipes.prefs").getString(), x + 10, y + 58, GuiStyle.TEXT_GOOD);
        for (int i = ControllerMenu.BLACKLIST_START; i < ControllerMenu.STOCK_START; i++) {
            GuiStyle.menuSlot(g, x - ControllerMenu.TAB_COL, y, menu.slots.get(i), GuiStyle.SLOT_CARD);
        }
        if (!menu.slots.get(1).hasItem()) {
            text(g, Component.translatable("gui.simple_bytestorage.controller.recipes.need_library").getString(), x + 10, y + 90, GuiStyle.TEXT_WARN);
        }
    }

    /** Обзор: панель статуса и энергии, панель состава сети (в стиле референса). */
    private void drawOverview(GuiGraphics g, NetworkStats s, int x, int y) {
        String[] states = {"offline", "online", "conflict"};
        int stateColor = s.state == 1 ? GuiStyle.TEXT_GOOD : (s.state == 2 ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_WARN);
        int ledColor = s.state == 1 ? GuiStyle.LED_GOOD : (s.state == 2 ? GuiStyle.LED_BAD : GuiStyle.LED_WARN);

        // --- Статус и энергия ---
        GuiStyle.display(g, x + 6, y + 22, 276, 42);
        GuiStyle.led(g, x + 14, y + 29, ledColor);
        String status = Component.translatable("gui.simple_bytestorage.controller.status").getString() + ": ";
        g.drawString(this.font, status, x + 24, y + 28, GuiStyle.TEXT_LABEL, false);
        g.drawString(this.font, Component.translatable("gui.simple_bytestorage.controller.state."
                + states[Math.min(2, Math.max(0, s.state))]).getString(), x + 24 + this.font.width(status), y + 28, stateColor, false);
        PixelIcons.draw(g, "network", x + 150, y + 27, GuiStyle.ACCENT);
        text(g, Component.translatable("gui.simple_bytestorage.controller.nodes", s.nodeTotal).getString(), x + 164, y + 28, GuiStyle.TEXT_TITLE);

        PixelIcons.draw(g, "bolt", x + 12, y + 39, GuiStyle.TEXT_WARN);
        text(g, Component.translatable("gui.simple_bytestorage.controller.energy").getString() + ": " + n(s.energy) + " / " + n(s.maxEnergy)
                + " FE  (" + (s.drain > 0 ? "-" : "") + n(s.drain) + " FE/t)", x + 26, y + 40, GuiStyle.TEXT_TITLE);
        GuiStyle.meter(g, x + 12, y + 53, 264, 6, s.maxEnergy == 0 ? 0 : s.energy / (float) s.maxEnergy);

        // --- Состав сети ---
        GuiStyle.display(g, x + 6, y + 68, 276, 46);
        int shown = 0;
        for (NetworkStats.BlockCount b : s.blocks) {
            if (shown >= 4) {
                break;
            }
            int cx = x + 12 + (shown % 2) * 144;
            int cy = y + 72 + (shown / 2) * 13;
            PixelIcons.draw(g, blockIcon(b.nameKey()), cx, cy, GuiStyle.ACCENT);
            text(g, Component.translatable(b.nameKey()).getString() + " x" + b.count(), cx + 14, cy + 1, GuiStyle.TEXT_LABEL);
            shown++;
        }
        int percent = s.maxEnergy == 0 ? 0 : (int) (s.energy * 100L / s.maxEnergy);
        int cy = y + 98;
        PixelIcons.draw(g, "cube", x + 12, cy, GuiStyle.ACCENT);
        text(g, Component.translatable("gui.simple_bytestorage.controller.items_total", n((int) Math.min(Integer.MAX_VALUE, s.totalItems))).getString(),
                x + 26, cy + 1, GuiStyle.TEXT_LABEL);
        PixelIcons.draw(g, "bolt", x + 156, cy, GuiStyle.TEXT_WARN);
        String charge = Component.translatable("gui.simple_bytestorage.controller.charge", percent).getString();
        if (s.drain > 0 && s.energy > 0) {
            charge += "  (~" + formatEta((int) Math.max(1, s.energy / (s.drain * 20L))) + ")";
        }
        text(g, charge, x + 170, cy + 1, percent < 25 ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_LABEL);
    }

    private static String blockIcon(String nameKey) {
        if (nameKey.contains("cable")) {
            return "cable";
        }
        if (nameKey.contains("io_bus")) {
            return "chip";
        }
        if (nameKey.contains("assembler")) {
            return "wrench";
        }
        if (nameKey.contains("controller")) {
            return "network";
        }
        return "cube";
    }

    /** Графики запасов и заряда за последние минуты и предметы, которые больше всего прибыли/убыли. */
    private void drawHistory(GuiGraphics g, NetworkStats s, int x, int y) {
        String items = Component.translatable("gui.simple_bytestorage.controller.history.items").getString();
        String energy = Component.translatable("gui.simple_bytestorage.controller.history.energy").getString();
        int lastItems = s.totalSeries.length == 0 ? 0 : s.totalSeries[s.totalSeries.length - 1];
        int lastEnergy = s.energySeries.length == 0 ? 0 : s.energySeries[s.energySeries.length - 1];
        text(g, items + ": " + n(lastItems), x, y, GuiStyle.TEXT_LABEL);
        text(g, energy + ": " + lastEnergy + "%", x + 136, y, GuiStyle.TEXT_LABEL);
        graph(g, x, y + 9, 128, 30, s.totalSeries, false, GuiStyle.ACCENT);
        graph(g, x + 136, y + 9, 128, 30, s.energySeries, true, GuiStyle.LED_GOOD);
        if (s.historySeconds > 0) {
            String window = Component.translatable("gui.simple_bytestorage.controller.history.window",
                    Math.max(1, Math.round(s.historySeconds / 60.0F))).getString();
            text(g, window, x + 264 - this.font.width(window), y, GuiStyle.TEXT_MUTED);
        }
        int ty = y + 43;
        text(g, Component.translatable("gui.simple_bytestorage.controller.history.rising").getString(), x, ty, GuiStyle.TEXT_GOOD);
        text(g, Component.translatable("gui.simple_bytestorage.controller.history.falling").getString(), x + 136, ty, GuiStyle.TEXT_BAD);
        trendRows(g, s.gaining, x, ty + 9, GuiStyle.TEXT_GOOD);
        trendRows(g, s.losing, x + 136, ty + 9, GuiStyle.TEXT_BAD);
    }

    private void trendRows(GuiGraphics g, List<NetworkStats.ItemDelta> rows, int x, int y, int color) {
        for (int i = 0; i < rows.size(); i++) {
            NetworkStats.ItemDelta d = rows.get(i);
            String value = (d.delta() > 0 ? "+" : "") + n(d.delta());
            int w = this.font.width(value);
            String name = this.font.plainSubstrByWidth(d.stack().getHoverName().getString(), 126 - w - 4);
            text(g, name, x, y + i * 9, GuiStyle.TEXT_LABEL);
            text(g, value, x + 126 - w, y + i * 9, color);
        }
    }

    /** Столбчатый график: новые значения справа; {@code percent} — шкала 0..100, иначе от минимума до максимума. */
    private void graph(GuiGraphics g, int x, int y, int w, int h, int[] series, boolean percent, int color) {
        g.fill(x, y, x + w, y + h, GuiStyle.PANEL_EDGE_LIGHT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, GuiStyle.DISPLAY_BG);
        if (series.length < 2) {
            text(g, Component.translatable("gui.simple_bytestorage.controller.history.nodata").getString(), x + 4, y + h / 2 - 4, GuiStyle.TEXT_MUTED);
            return;
        }
        int lo = 0;
        int hi = 100;
        if (!percent) {
            lo = Integer.MAX_VALUE;
            hi = Integer.MIN_VALUE;
            for (int v : series) {
                lo = Math.min(lo, v);
                hi = Math.max(hi, v);
            }
        }
        int inner = h - 2;
        int bar = Math.max(1, (w - 2) / StatsHistory.MAX_SAMPLES);
        int right = x + w - 1;
        for (int i = 0; i < series.length; i++) {
            int idx = series.length - 1 - i;
            float frac = hi == lo ? 0.5F : (series[idx] - lo) / (float) (hi - lo);
            int bh = 1 + Math.round(frac * (inner - 1));
            int bx = right - (i + 1) * bar;
            if (bx < x + 1) {
                break;
            }
            g.fill(bx, y + h - 1 - bh, bx + bar, y + h - 1, color);
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
