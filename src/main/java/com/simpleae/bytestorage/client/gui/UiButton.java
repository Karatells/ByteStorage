package com.simpleae.bytestorage.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Проста кнопка в стилі мода. Не є віджетом Minecraft: екран сам викликає
 * {@link #render} та {@link #mouseClicked} (клік обробляється ДО super.mouseClicked).
 */
public class UiButton {

    public enum Icon { NONE, DOWN, CROSS }

    private int x;
    private int y;
    private final int width;
    private final int height;
    private final Supplier<Component> label;
    private final Icon icon;
    private final Runnable action;
    private BooleanSupplier enabled = () -> true;
    private Component tooltip;
    private boolean primary;

    public UiButton(int x, int y, int width, int height, Supplier<Component> label, Icon icon, Runnable action) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.icon = icon;
        this.action = action;
    }

    public UiButton enabledWhen(BooleanSupplier supplier) {
        this.enabled = supplier;
        return this;
    }

    public UiButton tooltip(Component text) {
        this.tooltip = text;
        return this;
    }

    public UiButton primary() {
        this.primary = true;
        return this;
    }

    public boolean isOver(double mx, double my) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    public void render(GuiGraphics g, Font font, int mx, int my) {
        boolean on = enabled.getAsBoolean();
        boolean hover = on && isOver(mx, my);

        int border = !on ? 0xFF2F353E : (hover || primary ? GuiStyle.ACCENT : GuiStyle.ACCENT_DIM);
        int fill = !on ? 0xFF1B1E24 : (hover ? 0xFF2B3C4C : 0xFF222831);
        int text = !on ? GuiStyle.TEXT_MUTED : (hover || primary ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_LABEL);

        g.fill(x, y, x + width, y + height, border);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        if (on) {
            g.fill(x + 1, y + 1, x + width - 1, y + 2, 0x25FFFFFF);
        }

        int cx = x + width / 2;
        int cy = y + height / 2;
        switch (icon) {
            case DOWN -> {
                for (int i = 0; i < 4; i++) {
                    g.fill(cx - 3 + i, cy - 2 + i, cx + 4 - i, cy - 1 + i, text);
                }
                g.fill(cx - 1, cy - 4, cx + 2, cy - 2, text);
            }
            case CROSS -> {
                for (int i = 0; i < 6; i++) {
                    g.fill(cx - 3 + i, cy - 3 + i, cx - 2 + i, cy - 2 + i, text);
                    g.fill(cx + 2 - i, cy - 3 + i, cx + 3 - i, cy - 2 + i, text);
                }
            }
            default -> {
                String s = font.plainSubstrByWidth(label.get().getString(), width - 4);
                g.drawString(font, s, cx - font.width(s) / 2, y + (height - 8) / 2, text, false);
            }
        }
    }

    /** Малює текстову кнопку без створення об'єкта (для рядків списків). */
    public static void paint(GuiGraphics g, Font font, int x, int y, int w, int h, String text,
                             boolean hover, boolean enabled) {
        int border = !enabled ? 0xFF2F353E : (hover ? GuiStyle.ACCENT : GuiStyle.ACCENT_DIM);
        int fill = !enabled ? 0xFF1B1E24 : (hover ? 0xFF2B3C4C : 0xFF222831);
        int color = !enabled ? GuiStyle.TEXT_MUTED : (hover ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_LABEL);
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, color, false);
    }

    public void renderTooltip(GuiGraphics g, Font font, int mx, int my) {
        if (tooltip != null && isOver(mx, my)) {
            g.renderTooltip(font, tooltip, mx, my);
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && enabled.getAsBoolean() && isOver(mx, my)) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            action.run();
            return true;
        }
        return false;
    }
}
