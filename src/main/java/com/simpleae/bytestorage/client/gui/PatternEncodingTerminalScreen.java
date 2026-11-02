package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.PatternEncodingTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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

    private Button encodeButton;
    private Button clearButton;

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

        this.encodeButton = Button.builder(
                Component.translatable("gui.simple_bytestorage.encoder.encode"),
                button -> sendButton(PatternEncodingTerminalMenu.BUTTON_ENCODE)
        ).bounds(this.leftPos + 40, this.topPos + 99, 96, 20).build();
        this.addRenderableWidget(this.encodeButton);

        this.clearButton = Button.builder(
                Component.literal("×"),
                button -> sendButton(PatternEncodingTerminalMenu.BUTTON_CLEAR)
        ).bounds(this.leftPos + 6 + 76 - 14, this.topPos + 19, 12, 12).build();
        this.clearButton.setTooltip(Tooltip.create(Component.translatable("gui.simple_bytestorage.encoder.clear")));
        this.addRenderableWidget(this.clearButton);
    }

    private void sendButton(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Кнопка «Записати» доступна, коли є шаблон, відомий результат і вільний вихідний слот.
        boolean hasPattern = this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_BLANK).hasItem();
        boolean hasResult = this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_RESULT).hasItem();
        boolean outputFree = !this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_ENCODED).hasItem();
        this.encodeButton.active = hasPattern && hasResult && outputFree;

        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.renderEmptySlotHint(graphics, mouseX, mouseY);
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

        GuiStyle.renderWindowFrame(graphics, x, y, this.imageWidth, this.imageHeight);

        // Панель рецепту та панель результату
        GuiStyle.renderPanel(graphics, x + 6, y + 18, 76, 74,
                Component.translatable("gui.simple_bytestorage.encoder.recipe").getString());
        GuiStyle.renderPanel(graphics, x + 104, y + 18, 66, 74,
                Component.translatable("gui.simple_bytestorage.encoder.result").getString());
        GuiStyle.renderArrow(graphics, x + 86, y + 57);

        // Нижня смуга: шаблон — кнопка — готовий шаблон
        GuiStyle.renderPanel(graphics, x + 6, y + 96, 164, 26, null);

        for (int i = PatternEncodingTerminalMenu.SLOT_GRID_START; i < PatternEncodingTerminalMenu.SLOT_GRID_END; i++) {
            GuiStyle.renderMenuSlot(graphics, x, y, this.menu.slots.get(i), GuiStyle.COLOR_SLOT_CRAFT);
        }
        GuiStyle.renderMenuSlot(graphics, x, y, this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_RESULT),
                GuiStyle.COLOR_SLOT_RESULT);
        GuiStyle.renderMenuSlot(graphics, x, y, this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_BLANK),
                GuiStyle.COLOR_SLOT_BUFFER);
        GuiStyle.renderMenuSlot(graphics, x, y, this.menu.slots.get(PatternEncodingTerminalMenu.SLOT_ENCODED),
                GuiStyle.COLOR_SLOT_OUTPUT);

        GuiStyle.renderPlayerSlots(graphics, x, y, this.menu.slots, PatternEncodingTerminalMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, GuiStyle.COLOR_TEXT_TITLE, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                GuiStyle.COLOR_TEXT_TITLE, false);
    }
}
