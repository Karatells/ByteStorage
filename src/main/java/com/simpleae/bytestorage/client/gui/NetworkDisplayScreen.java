package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.block.entity.NetworkDisplayBlockEntity;
import com.simpleae.bytestorage.menu.NetworkDisplayMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Екран Інформаційного монітора мережі: 6 рядків тексту та кнопка очищення. */
public class NetworkDisplayScreen extends AbstractContainerScreen<NetworkDisplayMenu> {

    private UiButton clearButton;

    public NetworkDisplayScreen(NetworkDisplayMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = 111;
    }

    @Override
    protected void init() {
        super.init();
        this.clearButton = new UiButton(this.leftPos + 6, this.topPos + 90, 164, 14,
                () -> Component.translatable("gui.simple_bytestorage.workbench.clear"),
                UiButton.Icon.NONE, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, NetworkDisplayMenu.BUTTON_CLEAR);
            }
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.clearButton.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.clearButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int led = menu.isOnline() ? GuiStyle.LED_GOOD : GuiStyle.LED_BAD;
        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led, "chip");

        GuiStyle.display(graphics, x + 6, y + 20, 164, 66);
        for (int i = 0; i < NetworkDisplayBlockEntity.MAX_LINES; i++) {
            String line = menu.getLine(i);
            if (!line.isEmpty()) {
                graphics.drawString(this.font, this.font.plainSubstrByWidth(line, 156),
                        x + 10, y + 24 + i * 10, GuiStyle.ACCENT, false);
            }
        }

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, 0);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
