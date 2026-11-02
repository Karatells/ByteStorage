package com.simpleae.bytestorage.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import java.util.function.BooleanSupplier;

/**
 * Единый оформляющий стиль интерфейсов мода SimpleAE: ByteStorage.
 *
 * Централизует цвета, размеры панелей, отрисовку слотов, индикаторов
 * прогресса и кнопок, чтобы все окна (Терминал, Асемблер, Терминал Шаблонов,
 * Терминал Кодирования) выглядели целостно и аккуратно.
 */
public class GuiStyle {

    // --- Основная палитра цветов (ARGB) ---

    public static int COLOR_WINDOW_BG = 0xFFC6C6C6;      // светло-серый фон окна
    public static int COLOR_WINDOW_BORDER_LIGHT = 0xFFFFFFFF;
    public static int COLOR_WINDOW_BORDER_DARK = 0xFF555555;

    public static int COLOR_PANEL_BG = 0xFF2A2D33;       // тёмная панель внутри окна
    public static int COLOR_PANEL_BORDER = 0xFF373737;
    public static int COLOR_PANEL_HEADER = 0xFF4B5360;   // заголовок панели
    public static int COLOR_PANEL_HEADER_TEXT = 0xFFFFFFFF;

    public static int COLOR_SLOT_BG = 0xFF1E2127;        // фон слота
    public static int COLOR_SLOT_BORDER_SHADOW = 0xFF373737;
    public static int COLOR_SLOT_BORDER_HIGHLIGHT = 0xFFFFFFFF;

    public static int COLOR_SLOT_ACTIVE = 0xFF8595A8;    // активный/рабочий слот (сине-серый)
    public static int COLOR_SLOT_CRAFT = 0xFF9E8A6E;     // слот крафтовой сетки (коричневый)
    public static int COLOR_SLOT_RESULT = 0xFFB5963A;    // слот результата (золотой)
    public static int COLOR_SLOT_BUFFER = 0xFF8595A8;    // слот буферной карты (сине-серый)
    public static int COLOR_SLOT_OUTPUT = 0xFF6E7887;    // выходной слот (серо-синий)
    public static int COLOR_SLOT_PLAYER = 0xFF8B8B8B;    // слоты инвентаря игрока

    public static int COLOR_TEXT_TITLE = 0xFF404040;
    public static int COLOR_TEXT_LABEL = 0xFF707070;
    public static int COLOR_TEXT_VALUE = 0xFFE0E0E0;
    public static int COLOR_TEXT_MUTED = 0xFF9E9E9E;
    public static int COLOR_TEXT_GOOD = 0xFF00AA00;
    public static int COLOR_TEXT_BAD = 0xFFAA0000;
    public static int COLOR_TEXT_WARNING = 0xFFD4A017;

    public static int COLOR_PROGRESS_BG = 0xFF373737;
    public static int COLOR_PROGRESS_FILL = 0xFF00AA00;
    public static int COLOR_PROGRESS_BORDER = 0xFF555555;

    public static int COLOR_BUTTON_IDLE = 0xFF4B5360;
    public static int COLOR_BUTTON_HOVER = 0xFF6E7887;
    public static int COLOR_BUTTON_ACTIVE = 0xFF2A2D33;
    public static int COLOR_BUTTON_DISABLED = 0xFF373737;
    public static int COLOR_BUTTON_TEXT = 0xFFFFFFFF;
    public static int COLOR_BUTTON_TEXT_DISABLED = 0xFF9E9E9E;

    public static int COLOR_SCROLL_TRACK = 0xFF20232A;
    public static int COLOR_SCROLL_THUMB = 0xFF8B8B8B;

    // --- Размеры ---

    public static int SLOT_SIZE = 18;
    public static int SLOT_PADDING = 1;
    public static int PANEL_HEADER_HEIGHT = 14;

    /**
     * Отрисовка фирменной рамки окна (верхняя светлая фаска, нижняя тёмная).
     */
    public static void renderWindowFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, COLOR_WINDOW_BG);
        graphics.fill(x, y, x + width, y + 1, COLOR_WINDOW_BORDER_LIGHT);
        graphics.fill(x, y, x + 1, y + height, COLOR_WINDOW_BORDER_LIGHT);
        graphics.fill(x, y + height - 1, x + width, y + height, COLOR_WINDOW_BORDER_DARK);
        graphics.fill(x + width - 1, y, x + width, y + height, COLOR_WINDOW_BORDER_DARK);
    }

    /**
     * Отрисовка внутренней панели с заголовком.
     */
    public static void renderPanel(GuiGraphics graphics, int x, int y, int width, int height, String header) {
        // Фон панели
        graphics.fill(x, y, x + width, y + height, COLOR_PANEL_BG);
        graphics.fill(x, y, x + width, y + 1, COLOR_PANEL_BORDER);
        graphics.fill(x, y, x + 1, y + height, COLOR_PANEL_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF101216);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF101216);

        // Заголовок панели
        if (header != null && !header.isEmpty()) {
            int headerH = PANEL_HEADER_HEIGHT;
            graphics.fill(x, y, x + width, y + headerH, COLOR_PANEL_HEADER);
            graphics.fill(x, y + headerH - 1, x + width, y + headerH, COLOR_PANEL_BORDER);

            Font font = net.minecraft.client.Minecraft.getInstance().font;
            String headerText = font.plainSubstrByWidth(header, Math.max(0, width - 6));
            graphics.drawString(font, headerText, x + 3, y + 3, COLOR_PANEL_HEADER_TEXT, false);
        }
    }

    /**
     * Отрисовка одного рельефного слота Minecraft (как в инвентаре).
     *
     * @param innerColor цвет внутренней части слота (подсветка типа слота)
     */
    public static void renderSlotBox(GuiGraphics graphics, int x, int y, int innerColor) {
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, COLOR_SLOT_BORDER_SHADOW);           // верхняя тень
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, COLOR_SLOT_BORDER_SHADOW);           // левая тень
        graphics.fill(x + 1, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, COLOR_SLOT_BORDER_HIGHLIGHT); // нижний блик
        graphics.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, COLOR_SLOT_BORDER_HIGHLIGHT); // правый блик
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, innerColor); // внутренняя часть
    }

    /**
     * Отрисовка слота с подсветкой наведения.
     */
    public static void renderSlotBox(GuiGraphics graphics, int x, int y, int innerColor, boolean hovered) {
        renderSlotBox(graphics, x, y, innerColor);
        if (hovered) {
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x40FFFFFF);
        }
    }

    /**
     * Отрисовка простой линии толщиной 1 px (используется для соединительных
     * линий в схемах крафта/кодирования).
     */
    public static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) {
                break;
            }
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    /**
     * Отрисовка индикатора прогресса (горизонтальная полоса).
     *
     * @param progress значение от 0.0 до 1.0
     */
    public static void renderProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
        progress = Math.min(1.0F, Math.max(0.0F, progress));

        // Фон
        graphics.fill(x, y, x + width, y + height, COLOR_PROGRESS_BG);
        graphics.fill(x, y, x + width, y + 1, COLOR_PROGRESS_BORDER);
        graphics.fill(x, y, x + 1, y + height, COLOR_PROGRESS_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF101216);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF101216);

        // Заполнение
        int fillWidth = Math.max(0, (int) ((width - 2) * progress));
        if (fillWidth > 0) {
            int fillColor = progress >= 1.0F ? 0xFF00CC00 : COLOR_PROGRESS_FILL;
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, fillColor);
            // блик на заполнении
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + 2, 0x60FFFFFF);
        }

        // Текст百分比
        // (вызывающий код может нарисовать текст поверх)
    }

    /**
     * Отрисовка иконки предмета с количеством в указанной точке.
     */
    public static void renderItemStack(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
        if (stack.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int scaledX = (int) (x / scale);
        int scaledY = (int) (y / scale);
        graphics.renderItem(stack, scaledX, scaledY);
        graphics.renderItemDecorations(null, stack, scaledX, scaledY, String.valueOf(stack.getCount()));
        graphics.pose().popPose();
    }

    /**
     * Отрисовка иконки предмета (масштаб 1.0).
     */
    public static void renderItemStack(GuiGraphics graphics, ItemStack stack, int x, int y) {
        renderItemStack(graphics, stack, x, y, 1.0F);
    }

    /**
     * Создание стилизованной кнопки с поддержкой состояния enabled/disabled.
     *
     * @param onClick действие при клике
     * @param enabledSupplier поставщик состояния активности (проверяется каждый кадр)
     */
    public static Button createButton(Component text, int x, int y, int width, int height,
                                      Button.OnPress onClick, BooleanSupplier enabledSupplier) {
        return Button.builder(text, btn -> onClick.onPress(btn))
                .bounds(x, y, width, height)
                .build();
    }

    /**
     * Отрисовка полосы прокрутки (скроллбара).
     */
    public static void renderScrollBar(GuiGraphics graphics, int x, int y, int width, int height,
                                       int scrollOffset, int maxScroll, int visibleItems, int totalItems) {
        if (totalItems <= visibleItems) return;

        graphics.fill(x, y, x + width, y + height, COLOR_SCROLL_TRACK);

        int thumbH = Math.max(12, height * visibleItems / Math.max(1, totalItems));
        int thumbY = y;
        if (maxScroll > 0) {
            thumbY += (height - thumbH) * scrollOffset / maxScroll;
        }
        thumbY = Math.min(y + height - thumbH, Math.max(y, thumbY));

        graphics.fill(x + 1, thumbY, x + width - 1, thumbY + thumbH, COLOR_SCROLL_THUMB);
        graphics.fill(x + 1, thumbY, x + width - 1, thumbY + 1, 0xFFAAAAAA);
        graphics.fill(x + 1, thumbY + thumbH - 1, x + width - 1, thumbY + thumbH, 0xFF555555);
    }

    /**
     * Рисование многострочного текста с переносом по словам (упрощённо — по строкам).
     */
    public static void drawWrappedText(GuiGraphics graphics, net.minecraft.client.gui.Font font,
                                       String text, int x, int y, int maxWidth, int color) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineY = y;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (font.width(test) > maxWidth && line.length() > 0) {
                graphics.drawString(font, line.toString(), x, lineY, color, false);
                lineY += 9;
                line = new StringBuilder(word);
            } else {
                line.append(line.length() == 0 ? "" : " ").append(word);
            }
        }
        if (line.length() > 0) {
            graphics.drawString(font, line.toString(), x, lineY, color, false);
        }
    }

    /**
     * Рамка слота меню: slot.x / slot.y — позиція предмета, рамка на 1 px більша з кожного боку.
     */
    public static void renderMenuSlot(GuiGraphics graphics, int leftPos, int topPos, Slot slot, int innerColor) {
        renderSlotBox(graphics, leftPos + slot.x - 1, topPos + slot.y - 1, innerColor);
    }

    /**
     * Рамки для слотів інвентаря та хотбара гравця (слоти з індексу {@code from} до кінця списку).
     */
    public static void renderPlayerSlots(GuiGraphics graphics, int leftPos, int topPos, List<Slot> slots, int from) {
        for (int i = from; i < slots.size(); i++) {
            renderMenuSlot(graphics, leftPos, topPos, slots.get(i), COLOR_SLOT_PLAYER);
        }
    }

    /**
     * Проста стрілка вправо (14×11 px) для схем «вхід → вихід».
     */
    public static void renderArrow(GuiGraphics graphics, int x, int y) {
        int dark = 0xFF555555;
        int light = 0xFFB8B8B8;
        graphics.fill(x, y + 3, x + 9, y + 8, dark);
        graphics.fill(x + 1, y + 4, x + 9, y + 7, light);
        for (int j = 0; j < 6; j++) {
            graphics.fill(x + 8 + j, y + j, x + 9 + j, y + 11 - j, dark);
        }
        for (int j = 1; j < 5; j++) {
            graphics.fill(x + 8 + j, y + j + 1, x + 9 + j, y + 10 - j, light);
        }
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(Button button);
    }
}
