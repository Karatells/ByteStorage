package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.DriveMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Клієнтський екран накопичувача дисків (Drive GUI) у єдиному стилі мода.
 */
public class DriveScreen extends AbstractContainerScreen<DriveMenu> {

    public DriveScreen(DriveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 192;
        this.inventoryLabelY = 98;
    }

    private UiButton upButton;
    private UiButton downButton;

    @Override
    protected void init() {
        super.init();
        this.upButton = new UiButton(this.leftPos + 144, this.topPos + 19, 26, 11,
                () -> Component.literal("+"), UiButton.Icon.NONE,
                () -> sendButton(hasShiftDown() ? DriveMenu.BUTTON_PRIORITY_UP_10 : DriveMenu.BUTTON_PRIORITY_UP))
                .tooltip(Component.translatable("gui.simple_bytestorage.drive.priority.tip"));
        this.downButton = new UiButton(this.leftPos + 144, this.topPos + 46, 26, 11,
                () -> Component.literal("-"), UiButton.Icon.NONE,
                () -> sendButton(hasShiftDown() ? DriveMenu.BUTTON_PRIORITY_DOWN_10 : DriveMenu.BUTTON_PRIORITY_DOWN))
                .tooltip(Component.translatable("gui.simple_bytestorage.drive.priority.tip"));
    }

    private void sendButton(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.upButton.mouseClicked(mouseX, mouseY, button) || this.downButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.upButton.render(graphics, this.font, mouseX, mouseY);
        this.downButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.upButton.renderTooltip(graphics, this.font, mouseX, mouseY);
        this.downButton.renderTooltip(graphics, this.font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title);

        // Відсік накопичувача: 2 рядки по 5 слотів
        GuiStyle.panel(graphics, x + 38, y + 17, 102, 41);

        // Слоти меню: перші 10 — комірки накопичувача, решта — інвентар гравця
        int cells = Math.min(10, menu.slots.size());
        for (int i = 0; i < cells; i++) {
            Slot slot = menu.slots.get(i);
            GuiStyle.menuSlot(graphics, x, y, slot, GuiStyle.SLOT_CARD);
        }

        // Пріоритет вставки
        String priority = String.valueOf(menu.getPriority());
        graphics.drawString(this.font, priority, x + 157 - this.font.width(priority) / 2, y + 34, GuiStyle.ACCENT, false);

        // Білий список
        GuiStyle.panel(graphics, x + 6, y + 60, 164, 34);
        GuiStyle.label(graphics, Component.translatable("gui.simple_bytestorage.drive.filter"), x + 10, y + 63);
        for (int i = DriveMenu.FILTER_START; i < DriveMenu.FILTER_END; i++) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(i), GuiStyle.SLOT_POOL);
        }

        GuiStyle.playerSlots(graphics, x, y, menu.slots, DriveMenu.PLAYER_START);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Заголовок і підписи малюються у renderBg через GuiStyle
    }
}
