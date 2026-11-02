package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.DriveMenu;
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
        this.imageHeight = 152;
        this.inventoryLabelY = 59;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
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
        GuiStyle.playerSlots(graphics, x, y, menu.slots, cells);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Заголовок і підписи малюються у renderBg через GuiStyle
    }
}
