package com.simpleae.bytestorage.scripting.client;

import com.simpleae.bytestorage.client.gui.CodeEditor;
import com.simpleae.bytestorage.client.gui.GuiStyle;
import com.simpleae.bytestorage.client.gui.UiButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Редактор скрипта на весь экран: та же логика, что и в Логическом терминале, но с большим полем. */
public class ScriptEditorScreen extends Screen {

    private final LogicTerminalScreen parent;
    private final int maxLength;
    private final boolean canSave;
    private String initial;
    private CodeEditor editor;
    private final List<UiButton> buttons = new ArrayList<>();
    private long savedUntil;

    public ScriptEditorScreen(LogicTerminalScreen parent, String text, int maxLength, boolean canSave) {
        super(Component.translatable("gui.simple_bytestorage_scripting.editor.title"));
        this.parent = parent;
        this.initial = text;
        this.maxLength = maxLength;
        this.canSave = canSave;
    }

    @Override
    protected void init() {
        int margin = 8;
        int left = margin;
        int top = margin;
        int w = this.width - margin * 2;
        int h = this.height - margin * 2;
        String previous = this.editor != null ? this.editor.getValue() : initial;
        this.editor = new CodeEditor(this.font, left + 6, top + 22, w - 12, h - 22 - 26);
        this.editor.setCompletion(ApiCatalog.engine(), ApiCatalog.GLOBALS);
        this.editor.setMaxLength(maxLength);
        this.editor.setValue(previous);
        this.addRenderableWidget(this.editor);
        this.setInitialFocus(this.editor);

        buttons.clear();
        int by = top + h - 21;
        buttons.add(new UiButton(left + 6, by, 70, 16,
                () -> Component.translatable("gui.simple_bytestorage_scripting.save"), UiButton.Icon.NONE, this::save)
                .enabledWhen(() -> canSave).primary()
                .tooltip(Component.translatable("gui.simple_bytestorage_scripting.save.tip")));
        buttons.add(new UiButton(left + 82, by, 90, 16,
                () -> Component.translatable("gui.simple_bytestorage_scripting.editor.back"), UiButton.Icon.NONE,
                this::onClose));
    }

    private void save() {
        parent.saveFromFullscreen(editor.getValue());
        savedUntil = System.currentTimeMillis() + 2000;
    }

    @Override
    public void onClose() {
        parent.restoreFromFullscreen(editor.getValue());
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_S && canSave) {
            save();
            return true;
        }
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_E) {
            onClose();
            return true;
        }
        if (this.editor.isFocused() && this.editor.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (UiButton b : buttons) {
            if (b.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int margin = 8;
        GuiStyle.window(g, margin, margin, this.width - margin * 2, this.height - margin * 2, this.title,
                GuiStyle.LED_GOOD, "code");
        super.render(g, mouseX, mouseY, partialTick);
        for (UiButton b : buttons) {
            b.render(g, this.font, mouseX, mouseY);
        }
        int y = this.height - margin - 17;
        String pos = Component.translatable("gui.simple_bytestorage_scripting.editor.pos",
                editor.cursorLine(), editor.cursorColumn(), editor.length(), editor.maxLength()).getString();
        g.drawString(this.font, pos, margin + 182, y, GuiStyle.TEXT_LABEL, false);
        if (System.currentTimeMillis() < savedUntil) {
            g.drawString(this.font, Component.translatable("gui.simple_bytestorage_scripting.editor.saved"),
                    margin + 182 + this.font.width(pos) + 12, y, GuiStyle.TEXT_GOOD, false);
        }
        String hint = Component.translatable("gui.simple_bytestorage_scripting.editor.hint").getString();
        int hw = this.font.width(hint);
        if (margin + 182 + this.font.width(pos) + 80 + hw < this.width - margin - 6) {
            g.drawString(this.font, hint, this.width - margin - 8 - hw, y, GuiStyle.TEXT_MUTED, false);
        }
        for (UiButton b : buttons) {
            b.renderTooltip(g, this.font, mouseX, mouseY);
        }
    }
}
