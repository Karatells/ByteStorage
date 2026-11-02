package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.DriveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Клієнтський екран для накопичувача дисків (Drive GUI).
 */
public class DriveScreen extends AbstractContainerScreen<DriveMenu> {

    public DriveScreen(DriveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 152;
        this.inventoryLabelY = 58;
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

        // Основне вікно у ванильному стилі з фасками (уніфікований рендер рамки)
        GuiStyle.renderWindowFrame(graphics, x, y, imageWidth, imageHeight);

        // Панель для відсіку накопичувача (2 рядки по 5 слотів для ячеєк)
        // Рамка панелі дисків
        graphics.fill(x + 40, y + 16, x + 40 + 5 * 18 + 6, y + 16 + 2 * 18 + 6, GuiStyle.COLOR_PANEL_BG);
        graphics.fill(x + 41, y + 17, x + 40 + 5 * 18 + 5, y + 16 + 2 * 18 + 5, 0xFF8B8B8B);
        graphics.fill(x + 41, y + 17, x + 40 + 5 * 18 + 5, y + 18, 0xFF555555);
        graphics.fill(x + 41, y + 17, x + 42, y + 16 + 2 * 18 + 5, 0xFF555555);

        // 10 слотів для дисків (2 рядки по 5 слотів) - світліший металевий відтінок замість чорного
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int slotBoxX = x + 43 + col * 18;
                int slotBoxY = y + 19 + row * 18;
                GuiStyle.renderSlotBox(graphics, slotBoxX, slotBoxY, GuiStyle.COLOR_SLOT_BUFFER);
            }
        }

        // Область для інвентаря гравця (3 рядки по 9 слотів)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotBoxX = x + 7 + col * 18;
                int slotBoxY = y + 69 + row * 18;
                GuiStyle.renderSlotBox(graphics, slotBoxX, slotBoxY, 0xFF8B8B8B);
            }
        }

        // Область для хотбару гравця (9 слотів)
        for (int col = 0; col < 9; col++) {
            int slotBoxX = x + 7 + col * 18;
            int slotBoxY = y + 127;
            GuiStyle.renderSlotBox(graphics, slotBoxX, slotBoxY, 0xFF8B8B8B);
        }
    }

    /**
     * Отрисовка рамки вікна (уніфікований стиль SimpleAE через GuiStyle).
     */
    public static void renderFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        GuiStyle.renderWindowFrame(graphics, x, y, width, height);
    }

    /**
     * Отрисовка стандартного 18x18 рельєфного слота Minecraft (через GuiStyle).
     */
    public static void renderSlotBox(GuiGraphics graphics, int x, int y, int innerColor) {
        GuiStyle.renderSlotBox(graphics, x, y, innerColor);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }
}
