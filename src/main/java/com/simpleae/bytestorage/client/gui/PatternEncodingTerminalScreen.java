package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.PatternEncodingTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Екран Терміналу Кодування Шаблонів.
 *
 *   ┌ Рецепт ──────┐        ┌ Результат ┐
 *   │ [ ][ ][ ]    │  ───►  │   [   ]   │
 *   │ [ ][ ][ ] ×  │        │           │
 *   │ [ ][ ][ ]    │        │           │
 *   └──────────────┘        └───────────┘
 *   [шаблон]   [   Записати   ]   [готовий]
 *   Інвентар гравця 3×9 + хотбар
 */
public class PatternEncodingTerminalScreen extends AbstractContainerScreen<PatternEncodingTerminalMenu> {

    private UiButton encodeButton;
    private UiButton clearButton;
    private UiButton modeButton;

    public PatternEncodingTerminalScreen(PatternEncodingTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 220;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();

        this.encodeButton = new UiButton(this.leftPos + 40, this.topPos + 99, 96, 20,
                () -> Component.translatable("gui.simple_bytestorage.encoder.encode"), UiButton.Icon.NONE,
                () -> sendButton(PatternEncodingTerminalMenu.BUTTON_ENCODE))
                .primary().icon("check")
                .enabledWhen(this::canEncode);

        this.clearButton = new UiButton(this.leftPos + 66, this.topPos + 20, 12, 12,
                () -> Component.empty(), UiButton.Icon.CROSS,
                () -> sendButton(PatternEncodingTerminalMenu.BUTTON_CLEAR))
                .tooltip(Component.translatable("gui.simple_bytestorage.encoder.clear"));

        this.modeButton = new UiButton(this.leftPos + 108, this.topPos + 74, 58, 14,
                this::modeLabel, UiButton.Icon.NONE,
                () -> sendButton(PatternEncodingTerminalMenu.BUTTON_MODE));
    }

    private Component modeLabel() {
        return Component.translatable(this.menu.isProcessing()
                ? "gui.simple_bytestorage.encoder.mode.processing"
                : "gui.simple_bytestorage.encoder.mode.crafting");
    }

    private Component modeTooltip() {
        return Component.translatable(this.menu.isProcessing()
                ? "gui.simple_bytestorage.encoder.mode.processing.tip"
                : "gui.simple_bytestorage.encoder.mode.crafting.tip");
    }

    /** Кнопка «Записати» доступна (процес: коли є шаблон, відомий результат і вільний вихідний слот; верстак: коли відомий результат). */
    private boolean canEncode() {
        boolean hasPattern = this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_BLANK).hasItem();
        boolean hasResult = this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_RESULT).hasItem();
        boolean outputFree = !this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_ENCODED).hasItem();
        if (!this.menu.isProcessing()) {
            return hasResult; // рецепт верстака записується прямо в мережу, шаблон не потрібен
        }
        return hasPattern && hasResult && outputFree;
    }

    private void sendButton(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.encodeButton.render(graphics, this.font, mouseX, mouseY);
        this.clearButton.render(graphics, this.font, mouseX, mouseY);
        this.modeButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (this.modeButton.isOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font, modeTooltip(), mouseX, mouseY);
        }
        this.clearButton.renderTooltip(graphics, this.font, mouseX, mouseY);
        this.renderEmptySlotHint(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.encodeButton.mouseClicked(mouseX, mouseY, button)
                || this.clearButton.mouseClicked(mouseX, mouseY, button)
                || this.modeButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Підказка над порожніми службовими слотами. */
    private void renderEmptySlotHint(GuiGraphics graphics, int mouseX, int mouseY) {
        Slot hovered = this.hoveredSlot;
        if (hovered == null || hovered.hasItem() || !this.menu.getCarried().isEmpty()) {
            return;
        }
        int index = hovered.index;
        if (index == PatternEncodingTerminalMenu.SLOT_BLANK) {
            graphics.renderTooltip(this.font, Component.translatable("gui.simple_bytestorage.encoder.slot.pattern"), mouseX, mouseY);
        } else if (index == PatternEncodingTerminalMenu.SLOT_ENCODED) {
            graphics.renderTooltip(this.font, Component.translatable("gui.simple_bytestorage.encoder.slot.encoded"), mouseX, mouseY);
        } else if (index >= PatternEncodingTerminalMenu.SLOT_GRID_START && index < PatternEncodingTerminalMenu.SLOT_GRID_END) {
            graphics.renderTooltip(this.font, Component.translatable("gui.simple_bytestorage.encoder.slot.grid"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        GuiStyle.window(graphics, x, y, this.imageWidth, this.imageHeight, this.title,
                canEncode() ? GuiStyle.LED_GOOD : 0xFF4A5260, "doc");

        // Панель рецепту та панель результату
        GuiStyle.panel(graphics, x + 6, y + 18, 76, 74);
        GuiStyle.label(graphics, Component.translatable(this.menu.isProcessing()
                ? "gui.simple_bytestorage.encoder.inputs" : "gui.simple_bytestorage.encoder.recipe"), x + 10, y + 22);
        GuiStyle.panel(graphics, x + 104, y + 18, 66, 74);
        GuiStyle.label(graphics, Component.translatable("gui.simple_bytestorage.encoder.result"), x + 108, y + 22);
        GuiStyle.arrow(graphics, x + 86, y + 57);

        // Нижня смуга: шаблон — кнопка — готовий шаблон
        GuiStyle.panel(graphics, x + 6, y + 96, 164, 26);

        for (int i = PatternEncodingTerminalMenu.SLOT_GRID_START; i < PatternEncodingTerminalMenu.SLOT_GRID_END; i++) {
            GuiStyle.menuSlot(graphics, x, y, this.menu.slots.get(i), GuiStyle.SLOT_CRAFT);
        }
        GuiStyle.menuSlot(graphics, x, y, this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_RESULT),
                GuiStyle.SLOT_RESULT, GuiStyle.TEXT_WARN);
        GuiStyle.menuSlot(graphics, x, y, this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_BLANK),
                GuiStyle.SLOT_CARD);
        GuiStyle.menuSlot(graphics, x, y, this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_ENCODED),
                GuiStyle.SLOT_OUTPUT, GuiStyle.ACCENT_DIM);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + this.inventoryLabelX, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, this.menu.slots, PatternEncodingTerminalMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Усе малюється у renderBg через GuiStyle
    }
}
