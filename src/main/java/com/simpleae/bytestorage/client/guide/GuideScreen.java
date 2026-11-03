package com.simpleae.bytestorage.client.guide;

import com.simpleae.bytestorage.client.gui.GuiStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Екран Книги-довідника: зліва розділи, справа текст із прокруткою. */
public class GuideScreen extends Screen {

    private static final int W = 340;
    private static final int H = 224;
    private static final int LIST_W = 100;
    private static final int ROW_H = 16;
    private static final int TEXT_X = 112;
    private static final int TEXT_W = 214;
    private static final int TEXT_H = 190;

    // Запам'ятовуємо вибір між відкриттями
    private static int rememberedChapter = 0;

    private int left;
    private int top;
    private int chapterIndex = rememberedChapter;
    private int scroll = 0;
    private int listScroll = 0;
    private List<GuideRegistry.Chapter> chapters = new ArrayList<>();
    private List<FormattedCharSequence> lines = new ArrayList<>();

    public GuideScreen() {
        super(Component.translatable("gui.simple_bytestorage.guide.title"));
    }

    @Override
    protected void init() {
        this.left = (this.width - W) / 2;
        this.top = (this.height - H) / 2;
        this.chapters = GuideRegistry.chapters();
        if (chapterIndex >= chapters.size()) {
            chapterIndex = 0;
        }
        rebuild();
    }

    private void rebuild() {
        lines = new ArrayList<>();
        if (chapters.isEmpty()) {
            return;
        }
        String body = Component.translatable(chapters.get(chapterIndex).bodyKey()).getString();
        for (String paragraph : body.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(this.font.split(FormattedText.of(paragraph), TEXT_W));
        }
        scroll = Math.min(scroll, maxScroll());
    }

    private int visibleLines() {
        return TEXT_H / 10;
    }

    private int maxScroll() {
        return Math.max(0, lines.size() - visibleLines());
    }

    private int listRows() {
        return (H - 28) / ROW_H;
    }

    private void select(int index) {
        chapterIndex = index;
        rememberedChapter = index;
        scroll = 0;
        rebuild();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= left + 6 && mx < left + 6 + LIST_W && my >= top + 20 && my < top + 20 + listRows() * ROW_H) {
            int index = listScroll + (int) ((my - top - 20) / ROW_H);
            if (index >= 0 && index < chapters.size()) {
                select(index);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < left + 6 + LIST_W) {
            int max = Math.max(0, chapters.size() - listRows());
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.signum(delta)));
        } else {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta) * 3));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // PageUp/PageDown/стрілки — прокрутка тексту
        if (keyCode == 266 || keyCode == 265) {
            scroll = Math.max(0, scroll - (keyCode == 266 ? visibleLines() : 1));
            return true;
        }
        if (keyCode == 267 || keyCode == 264) {
            scroll = Math.min(maxScroll(), scroll + (keyCode == 267 ? visibleLines() : 1));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.renderBackground(g);
        GuiStyle.window(g, left, top, W, H, this.title);

        GuiStyle.display(g, left + 6, top + 20, LIST_W, listRows() * ROW_H + 2);
        for (int row = 0; row < listRows(); row++) {
            int index = listScroll + row;
            if (index >= chapters.size()) {
                break;
            }
            int ry = top + 21 + row * ROW_H;
            if (index == chapterIndex) {
                g.fill(left + 7, ry, left + 5 + LIST_W, ry + ROW_H - 1, GuiStyle.ACCENT_FAINT);
            }
            String title = Component.translatable(chapters.get(index).titleKey()).getString();
            g.drawString(this.font, this.font.plainSubstrByWidth(title, LIST_W - 8), left + 10, ry + 4,
                    index == chapterIndex ? GuiStyle.ACCENT : GuiStyle.TEXT_TITLE, false);
        }

        GuiStyle.display(g, left + TEXT_X - 4, top + 20, TEXT_W + 14, TEXT_H + 8);
        int y = top + 24;
        for (int i = scroll; i < Math.min(lines.size(), scroll + visibleLines()); i++) {
            g.drawString(this.font, lines.get(i), left + TEXT_X, y, GuiStyle.TEXT_TITLE, false);
            y += 10;
        }
        GuiStyle.scrollbar(g, left + TEXT_X + TEXT_W + 2, top + 21, 6, TEXT_H + 6,
                scroll, maxScroll(), visibleLines(), lines.size());

        super.render(g, mx, my, partialTick);
    }
}
