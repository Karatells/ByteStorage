package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.SecurityTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Екран Термінала безпеки у стилі мода. */
public class SecurityTerminalScreen extends AbstractContainerScreen<SecurityTerminalMenu> {

    private UiButton lockButton;

    public SecurityTerminalScreen(SecurityTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = 77;
    }

    @Override
    protected void init() {
        super.init();
        this.lockButton = new UiButton(this.leftPos + 8, this.topPos + 51, 160, 16,
                () -> Component.translatable(this.menu.isLocked()
                        ? "gui.simple_bytestorage.security.btn.locked" : "gui.simple_bytestorage.security.btn.unlocked"),
                UiButton.Icon.NONE, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, SecurityTerminalMenu.BUTTON_LOCK);
            }
        }).primary();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.lockButton.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.lockButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int led = !menu.isOnline() ? GuiStyle.LED_BAD : (menu.isLocked() ? GuiStyle.LED_WARN : GuiStyle.LED_GOOD);
        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led, "chip");

        GuiStyle.display(graphics, x + 6, y + 20, 164, 24);
        graphics.drawString(this.font,
                Component.translatable("gui.simple_bytestorage.security.owner", menu.getOwnerName()),
                x + 10, y + 28, GuiStyle.TEXT_TITLE, false);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, 0);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
