package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.CraftingTerminalMenu;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.network.packet.TerminalActionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Клієнтський екран Термінала Крафту: сітка 3×3 і результат зверху, пул предметів посередині.
 */
public class CraftingTerminalScreen extends AbstractContainerScreen<CraftingTerminalMenu> {

    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);
    private static final int ITEMS_PER_ROW = 9;
    private static final int VISIBLE_ROWS = 3;
    private static final int POOL_X = 7;
    private static final int POOL_Y = 76;

    private EditBox searchBox;
    private UiButton depositButton;
    private int scrollOffset = 0;

    public CraftingTerminalScreen(CraftingTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 195;
        this.imageHeight = 222;
        this.inventoryLabelY = 138;
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(this.font, this.leftPos + 99, this.topPos + 21, 82, 10, Component.empty());
        this.searchBox.setMaxLength(30);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(GuiStyle.TEXT_TITLE);
        this.searchBox.setHint(Component.translatable("gui.simple_bytestorage.terminal.search")
                .withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.searchBox);

        this.depositButton = new UiButton(this.leftPos + 166, this.topPos + 46, 16, 16,
                () -> Component.empty(), UiButton.Icon.DOWN, () -> {
            int flag = hasShiftDown() ? 1 : 0;
            SimpleByteStoragePackets.sendToServer(
                    new TerminalActionPacket(new ItemStack(Items.AIR, flag), TerminalActionPacket.ACTION_DEPOSIT_ALL));
        });
        this.depositButton.tooltip(Component.translatable("gui.simple_bytestorage.terminal.deposit"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.depositButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.depositButton.renderTooltip(graphics, this.font, mouseX, mouseY);
        this.renderPoolTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        int used = menu.getClientTotalUsed();
        int max = menu.getClientTotalCapacity();
        float fill = max > 0 ? (float) used / max : 0.0F;
        int led = max <= 0 ? GuiStyle.LED_BAD : (fill >= 0.95F ? GuiStyle.LED_WARN : GuiStyle.LED_GOOD);

        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led);

        // Пошук
        GuiStyle.display(graphics, x + 96, y + 19, 88, 12);

        // Слоти меню: 0 — результат, 1..9 — сітка, далі — інвентар гравця
        for (int i = 1; i <= 9 && i < menu.slots.size(); i++) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(i), GuiStyle.SLOT_CRAFT);
        }
        GuiStyle.arrow(graphics, x + 94, y + 42);
        GuiStyle.panel(graphics, x + 118, y + 34, 30, 30);
        if (!menu.slots.isEmpty()) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(0), GuiStyle.SLOT_RESULT, GuiStyle.TEXT_WARN);
        }
        GuiStyle.playerSlots(graphics, x, y, menu.slots, 10);

        // Пул предметів
        GuiStyle.display(graphics, x + POOL_X - 1, y + POOL_Y - 1, 164, 56);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                GuiStyle.slot(graphics, x + POOL_X + col * 18, y + POOL_Y + row * 18, GuiStyle.SLOT_POOL);
            }
        }
        List<ItemStack> filtered = getFilteredItems();
        int maxRows = (filtered.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, maxRows - VISIBLE_ROWS)));
        GuiStyle.scrollbar(graphics, x + 172, y + POOL_Y - 1, 10, 56,
                scrollOffset, Math.max(0, maxRows - VISIBLE_ROWS), VISIBLE_ROWS, maxRows);

        // Пам'ять
        GuiStyle.meter(graphics, x + 8, y + 132, 179, 4, fill);
        String text = Component.translatable("gui.simple_bytestorage.terminal.memory",
                FORMAT.format(used), FORMAT.format(max),
                String.format(Locale.ROOT, "%.0f", fill * 100.0F)).getString();
        int color = max > 0 && used >= max ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_GOOD;
        graphics.drawString(this.font, text, x + 187 - this.font.width(text), y + this.inventoryLabelY, color, false);
        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);

        renderPoolItems(graphics, mouseX, mouseY, filtered);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Усе малюється у renderBg через GuiStyle
    }

    private void renderPoolItems(GuiGraphics graphics, int mouseX, int mouseY, List<ItemStack> filtered) {
        int start = scrollOffset * ITEMS_PER_ROW;
        for (int i = 0; i < ITEMS_PER_ROW * VISIBLE_ROWS; i++) {
            int itemX = this.leftPos + POOL_X + 1 + (i % ITEMS_PER_ROW) * 18;
            int itemY = this.topPos + POOL_Y + 1 + (i / ITEMS_PER_ROW) * 18;

            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                graphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, GuiStyle.ACCENT_FAINT);
            }
            if (start + i < filtered.size()) {
                ItemStack stack = filtered.get(start + i);
                graphics.renderItem(stack, itemX, itemY);
                graphics.renderItemDecorations(this.font, stack, itemX, itemY, formatCount(stack.getCount()));
            }
        }
    }

    private void renderPoolTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack stack = stackAt(mouseX, mouseY);
        if (stack.isEmpty()) {
            return;
        }
        List<Component> tooltip = getTooltipFromContainerItem(stack);
        tooltip.add(Component.translatable("gui.simple_bytestorage.terminal.stored", FORMAT.format(stack.getCount()))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("gui.simple_bytestorage.terminal.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private ItemStack stackAt(double mouseX, double mouseY) {
        List<ItemStack> filtered = getFilteredItems();
        int start = scrollOffset * ITEMS_PER_ROW;
        for (int i = 0; i < ITEMS_PER_ROW * VISIBLE_ROWS; i++) {
            if (start + i >= filtered.size()) {
                break;
            }
            int itemX = this.leftPos + POOL_X + 1 + (i % ITEMS_PER_ROW) * 18;
            int itemY = this.topPos + POOL_Y + 1 + (i / ITEMS_PER_ROW) * 18;
            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                return filtered.get(start + i);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.searchBox.setFocused(this.searchBox.isMouseOver(mouseX, mouseY));
        if (this.searchBox.isFocused()) {
            this.searchBox.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (this.depositButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        boolean inPool = mouseX >= leftPos + POOL_X && mouseX < leftPos + POOL_X + 18 * ITEMS_PER_ROW
                && mouseY >= topPos + POOL_Y && mouseY < topPos + POOL_Y + 18 * VISIBLE_ROWS;
        if (inPool) {
            if (!menu.getCarried().isEmpty()) {
                if (button == 0) {
                    SimpleByteStoragePackets.sendToServer(
                            new TerminalActionPacket(menu.getCarried(), TerminalActionPacket.ACTION_INSERT_CURSOR));
                } else if (button == 1) {
                    SimpleByteStoragePackets.sendToServer(
                            new TerminalActionPacket(menu.getCarried(), TerminalActionPacket.ACTION_INSERT_ONE));
                }
                return true;
            }
            ItemStack clicked = stackAt(mouseX, mouseY);
            if (!clicked.isEmpty()) {
                int action = TerminalActionPacket.ACTION_EXTRACT_STACK;
                if (hasShiftDown()) {
                    action = TerminalActionPacket.ACTION_EXTRACT_SHIFT;
                } else if (button == 1) {
                    action = TerminalActionPacket.ACTION_EXTRACT_HALF;
                }
                SimpleByteStoragePackets.sendToServer(new TerminalActionPacket(clicked, action));
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused() && keyCode != 256) {
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.isFocused()) {
            boolean handled = this.searchBox.charTyped(codePoint, modifiers);
            scrollOffset = 0;
            return handled;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxRows = (getFilteredItems().size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        int maxScroll = Math.max(0, maxRows - VISIBLE_ROWS);
        if (delta < 0) {
            scrollOffset = Math.min(scrollOffset + 1, maxScroll);
        } else if (delta > 0) {
            scrollOffset = Math.max(scrollOffset - 1, 0);
        }
        return true;
    }

    private List<ItemStack> getFilteredItems() {
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";
        List<ItemStack> pool = menu.getClientVirtualPool();
        if (query.isEmpty()) {
            return pool;
        }
        return pool.stream()
                .filter(s -> s.getHoverName().getString().toLowerCase().contains(query))
                .toList();
    }

    private static String formatCount(int count) {
        if (count >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1_000.0);
        }
        return String.valueOf(count);
    }
}
