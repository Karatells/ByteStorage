package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.QuantumRelayMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Екран Квантового ретранслятора: слот кристала та кількість зв'язаних партнерів. */
public class QuantumRelayScreen extends AbstractContainerScreen<QuantumRelayMenu> {

    public QuantumRelayScreen(QuantumRelayMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = 77;
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
        int led = !menu.isOnline() ? GuiStyle.LED_BAD : (menu.getPartnerCount() > 0 ? GuiStyle.LED_GOOD : GuiStyle.LED_WARN);
        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led, "chip");

        GuiStyle.panel(graphics, x + 6, y + 20, 164, 34);
        GuiStyle.menuSlot(graphics, x, y, menu.slots.get(QuantumRelayMenu.CRYSTAL_SLOT), GuiStyle.SLOT_CARD);

        GuiStyle.display(graphics, x + 38, y + 25, 128, 24);
        Component status = Component.translatable("gui.simple_bytestorage.relay.partners", menu.getPartnerCount());
        int color = menu.getPartnerCount() > 0 ? GuiStyle.TEXT_GOOD : GuiStyle.TEXT_WARN;
        graphics.drawString(this.font, status, x + 44, y + 33, color, false);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, QuantumRelayMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
