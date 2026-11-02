package com.simpleae.bytestorage.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Єдиний стиль інтерфейсів SimpleAE: ByteStorage — «ME-обладнання».
 *
 * Темний шліфований метал (як текстури блоків), заглиблені панелі, неоново-бірюзовий акцент
 * для активних елементів, помаранчевий/зелений/червоний — лише для статусів.
 * Усі вікна мода малюються виключно через цей клас, тож зміна палітри тут міняє всі екрани.
 */
public final class GuiStyle {

    private GuiStyle() {
    }

    // --- Палітра (ARGB) ---

    public static final int WINDOW_BG = 0xFF2C3037;
    public static final int WINDOW_EDGE = 0xFF0B0D10;
    public static final int BEVEL_LIGHT = 0xFF5A6372;
    public static final int BEVEL_DARK = 0xFF181B20;
    public static final int HEADER_BG = 0xFF1C1F25;

    public static final int ACCENT = 0xFF00D8F0;
    public static final int ACCENT_DIM = 0xFF0A6C78;
    public static final int ACCENT_FAINT = 0x3000D8F0;

    public static final int PANEL_BG = 0xFF1A1D23;
    public static final int PANEL_EDGE_DARK = 0xFF0B0D10;
    public static final int PANEL_EDGE_LIGHT = 0xFF454C58;
    public static final int DISPLAY_BG = 0xFF0C1826;

    public static final int SLOT_PLAYER = 0xFF1B1E24;
    public static final int SLOT_POOL = 0xFF141B26;
    public static final int SLOT_CARD = 0xFF1A2A3D;
    public static final int SLOT_CRAFT = 0xFF2B2517;
    public static final int SLOT_RESULT = 0xFF3A2D10;
    public static final int SLOT_OUTPUT = 0xFF173236;

    public static final int TEXT_TITLE = 0xFFE8F4FF;
    public static final int TEXT_LABEL = 0xFF9FB0C4;
    public static final int TEXT_MUTED = 0xFF6E7887;
    public static final int TEXT_GOOD = 0xFF3DDC84;
    public static final int TEXT_WARN = 0xFFFF9F1C;
    public static final int TEXT_BAD = 0xFFFF5A47;

    public static final int LED_GOOD = 0xFF00B464;
    public static final int LED_WARN = 0xFFFF8C00;
    public static final int LED_BAD = 0xFFDC2800;

    // --- Вікно ---

    public static void window(GuiGraphics g, int x, int y, int w, int h, Component title) {
        window(g, x, y, w, h, title, LED_GOOD);
    }

    /**
     * Вікно: темний метал, фаски, смуга заголовка з бірюзовою лінією, світлодіод статусу, заклепки.
     */
    public static void window(GuiGraphics g, int x, int y, int w, int h, Component title, int ledColor) {
        g.fill(x, y, x + w, y + h, WINDOW_EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, WINDOW_BG);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, BEVEL_LIGHT);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, BEVEL_LIGHT);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BEVEL_DARK);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, BEVEL_DARK);

        // Смуга заголовка
        g.fill(x + 3, y + 3, x + w - 3, y + 16, HEADER_BG);
        g.fill(x + 3, y + 15, x + w - 3, y + 16, ACCENT_DIM);
        if (title != null) {
            Font font = Minecraft.getInstance().font;
            g.drawString(font, title, x + 8, y + 5, TEXT_TITLE, false);
        }
        led(g, x + w - 11, y + 7, ledColor);

        // Заклепки внизу
        g.fill(x + 3, y + h - 5, x + 5, y + h - 3, 0xFF96A0AF);
        g.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, 0xFF96A0AF);
    }

    /** Світлодіод 3×3 з м'яким ореолом. */
    public static void led(GuiGraphics g, int x, int y, int color) {
        int halo = (color & 0x00FFFFFF) | 0x40000000;
        g.fill(x - 1, y - 1, x + 4, y + 4, halo);
        g.fill(x, y, x + 3, y + 3, color);
        g.fill(x, y, x + 1, y + 1, 0x80FFFFFF);
    }

    // --- Панелі ---

    /** Заглиблена панель. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL_EDGE_LIGHT);
        g.fill(x, y, x + w - 1, y + h - 1, PANEL_EDGE_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_BG);
    }

    /** «Екран» — синя заглиблена панель для списків і пулу предметів. */
    public static void display(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL_EDGE_LIGHT);
        g.fill(x, y, x + w - 1, y + h - 1, PANEL_EDGE_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, DISPLAY_BG);
    }

    /** Підпис панелі: дрібний текст кольору TEXT_LABEL. */
    public static void label(GuiGraphics g, Component text, int x, int y) {
        g.drawString(Minecraft.getInstance().font, text, x, y, TEXT_LABEL, false);
    }

    // --- Слоти ---

    /** Рамка слота 18×18 (boxX/boxY — лівий верхній кут рамки; предмет лежить на +1/+1). */
    public static void slot(GuiGraphics g, int boxX, int boxY, int tint) {
        g.fill(boxX, boxY, boxX + 18, boxY + 18, PANEL_EDGE_LIGHT);
        g.fill(boxX, boxY, boxX + 17, boxY + 17, PANEL_EDGE_DARK);
        g.fill(boxX + 1, boxY + 1, boxX + 17, boxY + 17, tint);
    }

    /** Слот з кольоровим контуром (результат, вихід). */
    public static void slot(GuiGraphics g, int boxX, int boxY, int tint, int accent) {
        g.fill(boxX, boxY, boxX + 18, boxY + 18, accent);
        g.fill(boxX + 1, boxY + 1, boxX + 17, boxY + 17, tint);
    }

    public static void menuSlot(GuiGraphics g, int leftPos, int topPos, Slot slot, int tint) {
        slot(g, leftPos + slot.x - 1, topPos + slot.y - 1, tint);
    }

    public static void menuSlot(GuiGraphics g, int leftPos, int topPos, Slot slot, int tint, int accent) {
        slot(g, leftPos + slot.x - 1, topPos + slot.y - 1, tint, accent);
    }

    /** Рамки слотів інвентаря та хотбара гравця (з індексу {@code from} до кінця списку). */
    public static void playerSlots(GuiGraphics g, int leftPos, int topPos, List<Slot> slots, int from) {
        for (int i = from; i < slots.size(); i++) {
            menuSlot(g, leftPos, topPos, slots.get(i), SLOT_PLAYER);
        }
    }

    /** Тонкий роздільник між інвентарем і хотбаром не потрібен; підпис «Інвентар» — дрібний, зліва. */
    public static void inventoryLabel(GuiGraphics g, Component text, int x, int y) {
        g.drawString(Minecraft.getInstance().font, text, x, y, TEXT_LABEL, false);
    }

    // --- Індикатори ---

    /** Сегментований індикатор прогресу (бірюзовий LED-бар). */
    public static void progress(GuiGraphics g, int x, int y, int w, int h, float fraction) {
        bar(g, x, y, w, h, fraction, ACCENT, true);
    }

    /** Смуга заповнення (пам'ять): колір залежить від рівня заповнення. */
    public static void meter(GuiGraphics g, int x, int y, int w, int h, float fraction) {
        int color = fraction >= 0.95F ? LED_BAD : (fraction >= 0.75F ? LED_WARN : ACCENT);
        bar(g, x, y, w, h, fraction, color, false);
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int color, boolean segments) {
        fraction = Math.max(0.0F, Math.min(1.0F, fraction));
        g.fill(x, y, x + w, y + h, PANEL_EDGE_LIGHT);
        g.fill(x, y, x + w - 1, y + h - 1, PANEL_EDGE_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, DISPLAY_BG);
        int fill = (int) ((w - 2) * fraction);
        if (fill > 0) {
            g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, color);
            g.fill(x + 1, y + 1, x + 1 + fill, y + 2, 0x70FFFFFF);
            if (segments) {
                for (int sx = x + 7; sx < x + 1 + fill; sx += 6) {
                    g.fill(sx, y + 1, sx + 1, y + h - 1, 0x60000000);
                }
            }
        }
    }

    /** Стрілка вправо 14×11 бірюзового кольору. */
    public static void arrow(GuiGraphics g, int x, int y) {
        g.fill(x, y + 4, x + 9, y + 7, ACCENT_DIM);
        g.fill(x, y + 4, x + 9, y + 5, ACCENT);
        for (int j = 0; j < 6; j++) {
            g.fill(x + 8 + j, y + j, x + 9 + j, y + 11 - j, ACCENT_DIM);
        }
        for (int j = 1; j < 5; j++) {
            g.fill(x + 8 + j, y + j + 1, x + 9 + j, y + 10 - j, ACCENT);
        }
    }

    /** Вертикальна смуга прокрутки. */
    public static void scrollbar(GuiGraphics g, int x, int y, int w, int h,
                                 int offset, int maxScroll, int visible, int total) {
        g.fill(x, y, x + w, y + h, PANEL_EDGE_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_BG);
        if (total <= visible || maxScroll <= 0) {
            return;
        }
        int thumbH = Math.max(10, (h - 2) * visible / Math.max(1, total));
        int thumbY = y + 1 + (h - 2 - thumbH) * offset / maxScroll;
        g.fill(x + 1, thumbY, x + w - 1, thumbY + thumbH, ACCENT_DIM);
        g.fill(x + 1, thumbY, x + w - 1, thumbY + 1, ACCENT);
    }
}
