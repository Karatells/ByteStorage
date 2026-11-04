package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.network.packet.CraftRequestPacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.util.CraftingService;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Общие возможности экранов терминалов: сортировка списка предметов (имя/количество, по возрастанию/убыванию)
 * и заказ автокрафта из любого терминала (Ctrl+клик или клик по предмету «0 в наличии»).
 */
public class TerminalExtras {

    // Настройки сортировки общие для всех терминалов и сохраняются на время игры
    private static boolean byCount = false;
    private static boolean descending = false;

    private static List<ItemStack> craftables = new ArrayList<>();
    private static Set<String> craftableKeys = new HashSet<>();

    public static void setCraftables(List<ItemStack> items) {
        craftables = items;
        Set<String> keys = new HashSet<>();
        for (ItemStack s : items) {
            keys.add(CraftingService.key(s));
        }
        craftableKeys = keys;
    }

    private static final NumberFormat FMT = NumberFormat.getInstance(Locale.US);
    private static final int SORT_W = 40;
    private static final int SORT_H = 14;
    private static final int DIALOG_W = 176;
    private static final int DIALOG_H = 78;

    private Set<String> poolKeys = new HashSet<>();

    private ItemStack dialogStack = ItemStack.EMPTY;
    private int dialogAmount = 1;

    // --- Список ---

    private record Row(ItemStack stack, String name, int count) {
    }

    /** Предмет сети + предметы, доступные только для крафта (количество 0); фильтр по тексту и сортировка. */
    public List<ItemStack> view(List<ItemStack> pool, String query) {
        Set<String> keys = new HashSet<>();
        List<Row> rows = new ArrayList<>();
        for (ItemStack s : pool) {
            keys.add(CraftingService.key(s));
        }
        for (ItemStack s : pool) {
            rows.add(new Row(s, s.getHoverName().getString(), s.getCount()));
        }
        for (ItemStack s : craftables) {
            if (!keys.contains(CraftingService.key(s))) {
                rows.add(new Row(s, s.getHoverName().getString(), 0));
            }
        }
        this.poolKeys = keys;

        String q = query == null ? "" : query.trim().toLowerCase();
        if (!q.isEmpty()) {
            rows.removeIf(r -> !r.name().toLowerCase().contains(q));
        }
        rows.sort((a, b) -> {
            int c = byCount ? Integer.compare(a.count(), b.count()) : a.name().compareToIgnoreCase(b.name());
            if (c == 0) {
                c = byCount ? a.name().compareToIgnoreCase(b.name()) : Integer.compare(a.count(), b.count());
            }
            return descending ? -c : c;
        });
        List<ItemStack> result = new ArrayList<>(rows.size());
        for (Row r : rows) {
            result.add(r.stack());
        }
        return result;
    }

    /** Предмет есть только как рецепт (в сети его нет). */
    public boolean isGhost(ItemStack stack) {
        return !poolKeys.contains(CraftingService.key(stack));
    }

    public boolean isCraftable(ItemStack stack) {
        return craftableKeys.contains(CraftingService.key(stack));
    }

    // --- Сортировка (кнопки справа от окна) ---

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String keyLabel() {
        return Component.translatable(byCount ? "gui.simple_bytestorage.sort.count" : "gui.simple_bytestorage.sort.name").getString();
    }

    private String dirLabel() {
        String k = byCount ? (descending ? "91" : "19") : (descending ? "za" : "az");
        return Component.translatable("gui.simple_bytestorage.sort." + k).getString();
    }

    public void renderSort(GuiGraphics g, Font font, int leftPos, int topPos, int imageWidth, int mx, int my) {
        int x = leftPos + imageWidth + 2;
        UiButton.paint(g, font, x, topPos + 4, SORT_W, SORT_H, keyLabel(), inside(mx, my, x, topPos + 4, SORT_W, SORT_H), true);
        UiButton.paint(g, font, x, topPos + 4 + SORT_H + 2, SORT_W, SORT_H, dirLabel(),
                inside(mx, my, x, topPos + 4 + SORT_H + 2, SORT_W, SORT_H), true);
        if (inside(mx, my, x, topPos + 4, SORT_W, SORT_H)) {
            g.renderTooltip(font, Component.translatable("gui.simple_bytestorage.sort.key.tip"), mx, my);
        } else if (inside(mx, my, x, topPos + 4 + SORT_H + 2, SORT_W, SORT_H)) {
            g.renderTooltip(font, Component.translatable("gui.simple_bytestorage.sort.dir.tip"), mx, my);
        }
    }

    /** @return true, если клик пришёлся на кнопку сортировки. */
    public boolean clickSort(double mx, double my, int button, int leftPos, int topPos, int imageWidth) {
        if (button != 0) {
            return false;
        }
        int x = leftPos + imageWidth + 2;
        if (inside(mx, my, x, topPos + 4, SORT_W, SORT_H)) {
            byCount = !byCount;
            return true;
        }
        if (inside(mx, my, x, topPos + 4 + SORT_H + 2, SORT_W, SORT_H)) {
            descending = !descending;
            return true;
        }
        return false;
    }

    // --- Диалог заказа ---

    public boolean isDialogOpen() {
        return !dialogStack.isEmpty();
    }

    public void openDialog(ItemStack stack) {
        ItemStack one = stack.copy();
        one.setCount(1);
        this.dialogStack = one;
        this.dialogAmount = 1;
    }

    public void closeDialog() {
        this.dialogStack = ItemStack.EMPTY;
    }

    private static final int[] STEPS = {-64, -10, -1, 1, 10, 64};

    public void renderDialog(GuiGraphics g, Font font, int screenW, int screenH, int mx, int my) {
        if (!isDialogOpen()) {
            return;
        }
        int x = (screenW - DIALOG_W) / 2;
        int y = (screenH - DIALOG_H) / 2;
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 400.0F);
        GuiStyle.panel(g, x, y, DIALOG_W, DIALOG_H);
        g.renderItem(dialogStack, x + 6, y + 6);
        String title = Component.translatable("gui.simple_bytestorage.order.title", dialogStack.getHoverName()).getString();
        g.drawString(font, font.plainSubstrByWidth(title, DIALOG_W - 32), x + 26, y + 10, GuiStyle.TEXT_TITLE, false);

        int bx = x + 6;
        for (int i = 0; i < 3; i++) {
            drawStep(g, font, bx + i * 23, y + 30, STEPS[i], mx, my);
        }
        String amount = FMT.format(dialogAmount);
        g.drawString(font, amount, x + DIALOG_W / 2 - font.width(amount) / 2, y + 34, GuiStyle.ACCENT, false);
        for (int i = 3; i < 6; i++) {
            drawStep(g, font, x + DIALOG_W - 6 - 23 * (6 - i) + 1, y + 30, STEPS[i], mx, my);
        }
        UiButton.paint(g, font, x + 6, y + DIALOG_H - 22, 76, 16,
                Component.translatable("gui.simple_bytestorage.order.confirm").getString(),
                inside(mx, my, x + 6, y + DIALOG_H - 22, 76, 16), true);
        UiButton.paint(g, font, x + DIALOG_W - 82, y + DIALOG_H - 22, 76, 16,
                Component.translatable("gui.simple_bytestorage.order.cancel").getString(),
                inside(mx, my, x + DIALOG_W - 82, y + DIALOG_H - 22, 76, 16), true);
        g.pose().popPose();
    }

    private void drawStep(GuiGraphics g, Font font, int x, int y, int step, int mx, int my) {
        UiButton.paint(g, font, x, y, 22, 16, (step > 0 ? "+" : "") + step, inside(mx, my, x, y, 22, 16), true);
    }

    /** Модальный диалог: поглощает все клики, пока открыт. */
    public boolean clickDialog(double mx, double my, int button, int screenW, int screenH) {
        if (!isDialogOpen()) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        int x = (screenW - DIALOG_W) / 2;
        int y = (screenH - DIALOG_H) / 2;
        for (int i = 0; i < 3; i++) {
            if (inside(mx, my, x + 6 + i * 23, y + 30, 22, 16)) {
                step(STEPS[i]);
                return true;
            }
        }
        for (int i = 3; i < 6; i++) {
            if (inside(mx, my, x + DIALOG_W - 6 - 23 * (6 - i) + 1, y + 30, 22, 16)) {
                step(STEPS[i]);
                return true;
            }
        }
        if (inside(mx, my, x + 6, y + DIALOG_H - 22, 76, 16)) {
            SimpleByteStoragePackets.sendToServer(new CraftRequestPacket(dialogStack, dialogAmount));
            closeDialog();
        } else if (inside(mx, my, x + DIALOG_W - 82, y + DIALOG_H - 22, 76, 16)) {
            closeDialog();
        }
        return true;
    }

    private void step(int delta) {
        dialogAmount = Math.max(1, Math.min(CraftRequestPacket.MAX_AMOUNT, dialogAmount + delta));
    }
}
