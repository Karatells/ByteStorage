package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.client.WirelessKeys;
import com.simpleae.bytestorage.item.WirelessConfig;
import com.simpleae.bytestorage.item.WirelessUpgradeItem;
import com.simpleae.bytestorage.menu.WirelessSettingsMenu;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.network.packet.WirelessSettingsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Экран настроек Беспроводного терминала: вкладки «Улучшения», «Перенос», «Макрос». */
public class WirelessSettingsScreen extends AbstractContainerScreen<WirelessSettingsMenu> {

    private static final String[] TABS = {"upgrades", "transfer", "macro"};

    private final List<UiButton> tabButtons = new ArrayList<>();
    private final List<UiButton> transferButtons = new ArrayList<>();
    private EditBox macroBox;

    public WirelessSettingsScreen(WirelessSettingsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 206;
    }

    private void click(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void init() {
        super.init();
        tabButtons.clear();
        transferButtons.clear();
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            tabButtons.add(new UiButton(leftPos + 6 + i * 55, topPos + 18, 54, 14,
                    () -> Component.translatable("gui.simple_bytestorage.wireless.tab." + TABS[index]),
                    UiButton.Icon.NONE, () -> menu.setTab(index)));
        }
        transferButtons.add(new UiButton(leftPos + 8, topPos + 38, 52, 16,
                () -> Component.translatable(menu.isRestock() ? "gui.simple_bytestorage.wireless.restock.on" : "gui.simple_bytestorage.wireless.restock.off"),
                UiButton.Icon.NONE, () -> click(0)).tooltip(Component.translatable("gui.simple_bytestorage.wireless.restock.tip")));
        transferButtons.add(new UiButton(leftPos + 62, topPos + 38, 52, 16,
                () -> Component.translatable(menu.isPickup() ? "gui.simple_bytestorage.wireless.pickup.on" : "gui.simple_bytestorage.wireless.pickup.off"),
                UiButton.Icon.NONE, () -> click(1)).tooltip(Component.translatable("gui.simple_bytestorage.wireless.pickup.tip")));
        transferButtons.add(new UiButton(leftPos + 116, topPos + 38, 52, 16,
                () -> Component.translatable(menu.isWhitelist() ? "gui.simple_bytestorage.wireless.whitelist" : "gui.simple_bytestorage.wireless.blacklist"),
                UiButton.Icon.NONE, () -> click(2)).tooltip(Component.translatable("gui.simple_bytestorage.wireless.list.tip")));

        macroBox = new EditBox(this.font, leftPos + 9, topPos + 53, 158, 14, Component.empty());
        macroBox.setMaxLength(WirelessConfig.MACRO_MAX_LENGTH);
        macroBox.setValue(WirelessConfig.macroName(menu.getTerminal()));
        macroBox.setResponder(value -> SimpleByteStoragePackets.sendToServer(new WirelessSettingsPacket(value)));
        macroBox.setFilter(s -> WirelessConfig.sanitizeMacro(s).length() == s.length());
        this.addRenderableWidget(macroBox);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (UiButton b : tabButtons) {
            if (b.mouseClicked(mx, my, button)) return true;
        }
        if (menu.getTab() == WirelessSettingsMenu.TAB_TRANSFER) {
            for (UiButton b : transferButtons) {
                if (b.mouseClicked(mx, my, button)) return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (macroBox.isVisible() && macroBox.isFocused() && keyCode != 256) {
            macroBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        macroBox.setVisible(menu.getTab() == WirelessSettingsMenu.TAB_MACRO);
        macroBox.setEditable(menu.getTab() == WirelessSettingsMenu.TAB_MACRO);
        if (!macroBox.isVisible()) {
            macroBox.setFocused(false);
        }
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        for (UiButton b : tabButtons) {
            b.render(g, this.font, mouseX, mouseY);
        }
        g.fill(leftPos + 6 + menu.getTab() * 55, topPos + 32, leftPos + 60 + menu.getTab() * 55, topPos + 34, GuiStyle.ACCENT);
        if (menu.getTab() == WirelessSettingsMenu.TAB_TRANSFER) {
            for (UiButton b : transferButtons) {
                b.render(g, this.font, mouseX, mouseY);
            }
        }
        this.renderTooltip(g, mouseX, mouseY);
        if (menu.getTab() == WirelessSettingsMenu.TAB_TRANSFER) {
            for (UiButton b : transferButtons) {
                b.renderTooltip(g, this.font, mouseX, mouseY);
            }
        }
    }

    private void line(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(this.font, this.font.plainSubstrByWidth(text, 160), x, y, color, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        GuiStyle.window(g, x, y, imageWidth, imageHeight, this.title, GuiStyle.LED_GOOD, "network");

        switch (menu.getTab()) {
            case WirelessSettingsMenu.TAB_UPGRADES -> drawUpgrades(g, x, y);
            case WirelessSettingsMenu.TAB_TRANSFER -> drawTransfer(g, x, y);
            default -> drawMacro(g, x, y);
        }

        GuiStyle.inventoryLabel(g, this.playerInventoryTitle, x + 8, y + WirelessSettingsMenu.PLAYER_INV_Y - 10);
        GuiStyle.playerSlots(g, x, y, menu.slots, WirelessSettingsMenu.PLAYER_START);
    }

    private void drawUpgrades(GuiGraphics g, int x, int y) {
        GuiStyle.panel(g, x + 6, y + 38, 164, 24);
        for (int i = 0; i < WirelessConfig.UPGRADE_SLOTS; i++) {
            GuiStyle.menuSlot(g, x, y, menu.slots.get(i), GuiStyle.SLOT_CARD, GuiStyle.ACCENT);
        }
        int row = y + 68;
        boolean any = false;
        for (WirelessUpgradeItem.Type type : WirelessUpgradeItem.Type.values()) {
            if (!menu.hasUpgrade(type)) continue;
            any = true;
            line(g, Component.translatable(type.nameKey()).getString(), x + 8, row, GuiStyle.TEXT_GOOD);
            line(g, Component.translatable(type.descKey()).getString(), x + 8, row + 10, GuiStyle.TEXT_MUTED);
            row += 22;
        }
        if (!any) {
            line(g, Component.translatable("gui.simple_bytestorage.wireless.upgrades.empty").getString(), x + 8, row, GuiStyle.TEXT_MUTED);
        }
    }

    private void drawTransfer(GuiGraphics g, int x, int y) {
        boolean installed = menu.hasUpgrade(WirelessUpgradeItem.Type.AUTO_TRANSFER);
        GuiStyle.label(g, "shield", Component.translatable("gui.simple_bytestorage.wireless.filter"), x + 8, y + 60);
        GuiStyle.panel(g, x + 6, y + 69, 164, 40);
        for (int i = 0; i < WirelessConfig.FILTER_SLOTS; i++) {
            GuiStyle.menuSlot(g, x, y, menu.slots.get(WirelessConfig.UPGRADE_SLOTS + i), GuiStyle.SLOT_POOL);
        }
        if (!installed) {
            String warn = Component.translatable("gui.simple_bytestorage.wireless.transfer.need_card").getString();
            g.drawString(this.font, this.font.plainSubstrByWidth(warn, 100), x + 74, y + 60, ChatFormatting.GOLD.getColor(), false);
        }
    }

    private void drawMacro(GuiGraphics g, int x, int y) {
        GuiStyle.label(g, "code", Component.translatable("gui.simple_bytestorage.wireless.macro.name"), x + 8, y + 40);
        GuiStyle.display(g, x + 7, y + 52, 162, 16);
        int row = y + 74;
        boolean installed = menu.hasUpgrade(WirelessUpgradeItem.Type.MACRO);
        if (!installed) {
            line(g, Component.translatable("gui.simple_bytestorage.wireless.macro.need_card").getString(), x + 8, row, ChatFormatting.GOLD.getColor());
            row += 11;
        }
        line(g, Component.translatable("gui.simple_bytestorage.wireless.macro.help1").getString(), x + 8, row, GuiStyle.TEXT_MUTED);
        line(g, Component.translatable("gui.simple_bytestorage.wireless.macro.help2").getString(), x + 8, row + 10, GuiStyle.TEXT_MUTED);
        line(g, Component.translatable("gui.simple_bytestorage.wireless.macro.key", WirelessKeys.MACRO.getTranslatedKeyMessage()).getString(),
                x + 8, row + 20, GuiStyle.TEXT_LABEL);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }
}
