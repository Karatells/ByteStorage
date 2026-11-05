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
        this.imageHeight = 204;
        this.inventoryLabelY = 111;
    }

    private UiButton recordButton;

    @Override
    protected void init() {
        super.init();
        recordButton = new UiButton(leftPos + 6, topPos + 78, 164, 14,
                () -> Component.translatable(menu.getRecordState() >= 1 && menu.getRecordState() <= 3
                        ? "gui.simple_bytestorage.provider.record.stop" : "gui.simple_bytestorage.provider.record.start"),
                UiButton.Icon.NONE, () -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                }).tooltip(Component.translatable("gui.simple_bytestorage.provider.record.tip"));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (recordButton.mouseClicked(mx, my, button)) {
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        recordButton.render(graphics, this.font, mouseX, mouseY);
        recordButton.renderTooltip(graphics, this.font, mouseX, mouseY);
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

        int rec = menu.getRecordState();
        if (rec != 0) {
            int color = rec == 4 ? GuiStyle.TEXT_GOOD : (rec >= 5 ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_TITLE);
            graphics.drawString(this.font, this.font.plainSubstrByWidth(
                    Component.translatable("gui.simple_bytestorage.provider.record.state." + rec).getString(), 156),
                    x + 10, y + 96, color, false);
        }

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, PatternProviderMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Усе малюється у renderBg через GuiStyle
    }
}
