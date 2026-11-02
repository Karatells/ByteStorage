package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.PatternProviderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Екран Шини Шаблонів: ряд слотів із процесними шаблонами, стан черги та машини поруч.
 */
public class PatternProviderScreen extends AbstractContainerScreen<PatternProviderMenu> {

    public PatternProviderScreen(PatternProviderMenu menu, Inventory playerInventory, Component title) {
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

        int led = !menu.isOnline() ? GuiStyle.LED_BAD : (menu.getMachines() > 0 ? GuiStyle.LED_GOOD : GuiStyle.LED_WARN);
        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led);

        GuiStyle.panel(graphics, x + 6, y + 20, 164, 24);
        for (int i = 0; i < PatternProviderMenu.PATTERN_SLOTS; i++) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(i), GuiStyle.SLOT_CARD);
        }

        GuiStyle.display(graphics, x + 6, y + 48, 164, 26);
        Component machine = Component.translatable(menu.getMachines() > 0
                ? "gui.simple_bytestorage.provider.machine.ok"
                : "gui.simple_bytestorage.provider.machine.none");
        int machineColor = menu.getMachines() > 0 ? GuiStyle.TEXT_GOOD : GuiStyle.TEXT_WARN;
        if (!menu.isOnline()) {
            machine = Component.translatable("gui.simple_bytestorage.provider.offline");
            machineColor = GuiStyle.TEXT_BAD;
        }
        graphics.drawString(this.font, machine, x + 10, y + 52, machineColor, false);
        graphics.drawString(this.font,
                Component.translatable("gui.simple_bytestorage.provider.queue", menu.getQueue()),
                x + 10, y + 63, GuiStyle.TEXT_LABEL, false);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, PatternProviderMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Усе малюється у renderBg через GuiStyle
    }
}
