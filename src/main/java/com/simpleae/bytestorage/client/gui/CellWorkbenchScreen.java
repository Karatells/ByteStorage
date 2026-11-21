package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.CellWorkbenchMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Екран Верстака ячеек: слот ячейки, фільтр (9 слотів), перемикач Allow/Deny та Очистити. */
public class CellWorkbenchScreen extends AbstractContainerScreen<CellWorkbenchMenu> {

    private UiButton modeButton;
    private UiButton clearButton;

    public CellWorkbenchScreen(CellWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 180;
        this.inventoryLabelY = 87;
    }

    @Override
    protected void init() {
        super.init();
        this.modeButton = new UiButton(this.leftPos + 40, this.topPos + 24, 62, 16,
                () -> Component.translatable(this.menu.isBlacklistMode()
                        ? "gui.simple_bytestorage.iobus.blacklist" : "gui.simple_bytestorage.iobus.whitelist"),
                UiButton.Icon.NONE, () -> clickBtn(CellWorkbenchMenu.BUTTON_BLACKLIST))
                .tooltip(Component.translatable("gui.simple_bytestorage.storage_bus.filter.tip"));
        this.clearButton = new UiButton(this.leftPos + 106, this.topPos + 24, 62, 16,
                () -> Component.translatable("gui.simple_bytestorage.workbench.clear"),
                UiButton.Icon.NONE, () -> clickBtn(CellWorkbenchMenu.BUTTON_CLEAR));
    }

    private void clickBtn(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.modeButton.mouseClicked(mouseX, mouseY, button) || this.clearButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.modeButton.render(graphics, this.font, mouseX, mouseY);
        this.clearButton.render(graphics, this.font, mouseX, mouseY);
        this.modeButton.renderTooltip(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, GuiStyle.LED_GOOD, "box");
        GuiStyle.panel(graphics, x + 6, y + 19, 164, 26);
        GuiStyle.menuSlot(graphics, x, y, menu.slots.get(CellWorkbenchMenu.CELL_SLOT), GuiStyle.SLOT_CARD);

        GuiStyle.panel(graphics, x + 6, y + 48, 164, 24);
        for (int i = CellWorkbenchMenu.FILTER_START; i < CellWorkbenchMenu.FILTER_END; i++) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(i), GuiStyle.SLOT_POOL);
        }

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, CellWorkbenchMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
