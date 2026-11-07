package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.network.packet.CraftPlanPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Общий вид окна «Подтверждение заказа»: что будет скрафчено (с подкрафтами), чего не хватает,
 * кнопки «Начать» / «Отмена». Одинаков в Терминале шаблонов и в обычных терминалах.
 */
public final class CraftPlanView {

    public static final int W = 202;
    public static final int BASE_H = 140;

    private CraftPlanView() {
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /**
     * @param extra дополнительная высота сверху (строка выбора количества в обычных терминалах)
     */
    public static void draw(GuiGraphics g, Font font, int x, int y, int extra, Component title,
                            List<CraftPlanPacket.Entry> steps, List<CraftPlanPacket.Entry> missing,
                            boolean startEnabled, int mx, int my) {
        draw(g, font, x, y, extra, title, steps, missing, startEnabled, mx, my, 0);
    }

    /**
     * @param maxAmount если чего-то не хватает, но часть заказа выполнима — кнопка «Начать» превращается в «Максимум: N»
     */
    public static void draw(GuiGraphics g, Font font, int x, int y, int extra, Component title,
                            List<CraftPlanPacket.Entry> steps, List<CraftPlanPacket.Entry> missing,
                            boolean startEnabled, int mx, int my, int maxAmount) {
        int h = BASE_H + extra;
        GuiStyle.panel(g, x, y, W, h);
        g.drawString(font, title, x + 6, y + 5, GuiStyle.TEXT_TITLE, false);

        GuiStyle.label(g, Component.translatable("gui.simple_bytestorage.plan.steps"), x + 6, y + 17 + extra);
        int line = y + 28 + extra;
        for (int i = 0; i < steps.size() && i < 6; i++) {
            CraftPlanPacket.Entry entry = steps.get(i);
            String text = entry.count + "× " + entry.stack.getHoverName().getString();
            if (entry.processing) {
                text += " " + Component.translatable("gui.simple_bytestorage.pattern.tag.process").getString();
            }
            g.drawString(font, font.plainSubstrByWidth(text, W - 16), x + 10, line, GuiStyle.TEXT_GOOD, false);
            line += 10;
        }
        if (steps.size() > 6) {
            g.drawString(font, "+" + (steps.size() - 6), x + 10, line, GuiStyle.TEXT_MUTED, false);
        }

        if (!missing.isEmpty()) {
            GuiStyle.label(g, Component.translatable("gui.simple_bytestorage.plan.missing"), x + 6, y + 92 + extra);
            int mline = y + 102 + extra;
            for (int i = 0; i < missing.size() && i < 3; i++) {
                CraftPlanPacket.Entry entry = missing.get(i);
                String text = entry.count + "× " + entry.stack.getHoverName().getString();
                g.drawString(font, font.plainSubstrByWidth(text, W - 16), x + 10, mline, GuiStyle.TEXT_BAD, false);
                mline += 10;
            }
            if (missing.size() > 3) {
                g.drawString(font, "+" + (missing.size() - 3), x + 10, mline, GuiStyle.TEXT_MUTED, false);
            }
        }

        int by = y + h - 20;
        boolean offerMax = !startEnabled && !missing.isEmpty() && maxAmount > 0;
        String startLabel = offerMax
                ? Component.translatable("gui.simple_bytestorage.plan.max", maxAmount).getString()
                : Component.translatable("gui.simple_bytestorage.plan.start").getString();
        UiButton.paint(g, font, x + 6, by, 92, 14, startLabel, inside(mx, my, x + 6, by, 92, 14), startEnabled || offerMax);
        UiButton.paint(g, font, x + W - 98, by, 92, 14, Component.translatable("gui.simple_bytestorage.plan.cancel").getString(),
                inside(mx, my, x + W - 98, by, 92, 14), true);
    }

    public static boolean startHit(double mx, double my, int x, int y, int extra) {
        return inside(mx, my, x + 6, y + BASE_H + extra - 20, 92, 14);
    }

    public static boolean cancelHit(double mx, double my, int x, int y, int extra) {
        return inside(mx, my, x + W - 98, y + BASE_H + extra - 20, 92, 14);
    }
}
