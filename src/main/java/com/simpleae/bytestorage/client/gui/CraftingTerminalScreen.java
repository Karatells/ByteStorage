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

    // Бокова ліва панель пулу мережі (5×10 = 50 слотів)
    private static final int SIDE_COLS = 5;
    private static final int SIDE_ROWS = 10;
    public static final int SIDE_PANEL_W = 114;
    private static final int SIDE_GAP = 4;
    private static boolean sidePool = false;

    private EditBox searchBox;
    private UiButton depositButton;
    private UiButton layoutButton;
    private int scrollOffset = 0;
    private final TerminalExtras extras = new TerminalExtras();

    public CraftingTerminalScreen(CraftingTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 195;
        this.imageHeight = 228;
        this.inventoryLabelY = 138;
    }

    public boolean isSidePool() {
        return sidePool;
    }

    public void toggleSidePool() {
        sidePool = !sidePool;
        scrollOffset = 0;
        updateLayoutTooltip();
    }

    public int getSidePanelX() {
        return this.leftPos - SIDE_PANEL_W - SIDE_GAP;
    }

    private int poolCols() {
        return sidePool ? SIDE_COLS : ITEMS_PER_ROW;
    }

    private int poolRows() {
        return sidePool ? SIDE_ROWS : VISIBLE_ROWS;
    }

    private int poolOriginX() {
        return sidePool ? (getSidePanelX() + 6) : (this.leftPos + POOL_X);
    }

    private int poolOriginY() {
        return sidePool ? (this.topPos + 20) : (this.topPos + POOL_Y);
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(this.font, this.leftPos + 99, this.topPos + 21, 82, 10, Component.empty());
        this.searchBox.setMaxLength(60);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(GuiStyle.TEXT_TITLE);
        this.searchBox.setHint(Component.translatable("gui.simple_bytestorage.terminal.search")
                .withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.searchBox);
        this.extras.bindSearch(() -> this.searchBox.getValue(), q -> this.searchBox.setValue(q));

        this.layoutButton = new UiButton(this.leftPos + 150, this.topPos + 46, 16, 16,
                () -> Component.literal(sidePool ? "v" : "<"), UiButton.Icon.NONE, this::toggleSidePool);
        updateLayoutTooltip();

        this.depositButton = new UiButton(this.leftPos + 170, this.topPos + 46, 16, 16,
                () -> Component.empty(), UiButton.Icon.DOWN, () -> {
            int flag = hasShiftDown() ? 1 : 0;
            SimpleByteStoragePackets.sendToServer(
                    new TerminalActionPacket(new ItemStack(Items.AIR, flag), TerminalActionPacket.ACTION_DEPOSIT_ALL));
        });
        this.depositButton.tooltip(Component.translatable("gui.simple_bytestorage.terminal.deposit"));
    }

    private void updateLayoutTooltip() {
        if (this.layoutButton != null) {
            this.layoutButton.tooltip(Component.translatable(sidePool
                    ? "gui.simple_bytestorage.terminal.layout_bottom"
                    : "gui.simple_bytestorage.terminal.layout_side"));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.layoutButton.render(graphics, this.font, mouseX, mouseY);
        this.depositButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.layoutButton.renderTooltip(graphics, this.font, mouseX, mouseY);
        this.depositButton.renderTooltip(graphics, this.font, mouseX, mouseY);
        this.renderPoolTooltip(graphics, mouseX, mouseY);
        this.extras.renderSort(graphics, this.font, this.leftPos, this.topPos, this.imageWidth, mouseX, mouseY);
        this.extras.renderDialog(graphics, this.font, this.width, this.height, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        int used = menu.getClientTotalUsed();
        int max = menu.getClientTotalCapacity();
        float fill = max > 0 ? (float) used / max : 0.0F;
        int led = max <= 0 ? GuiStyle.LED_BAD : (fill >= 0.95F ? GuiStyle.LED_WARN : GuiStyle.LED_GOOD);

        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led, "hammer");

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

        List<ItemStack> filtered = getFilteredItems();
        int cols = poolCols();
        int rows = poolRows();
        int px = poolOriginX();
        int py = poolOriginY();
        int maxRows = (filtered.size() + cols - 1) / cols;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, maxRows - rows)));

        if (sidePool) {
            // Ліва бокова панель зі списком предметів мережі (5×10)
            int sx = getSidePanelX();
            GuiStyle.window(graphics, sx, y, SIDE_PANEL_W, imageHeight,
                    Component.translatable("gui.simple_bytestorage.terminal.side_pool_title"), led, "drive");
            GuiStyle.display(graphics, px - 1, py - 1, cols * 18 + 2, rows * 18 + 2);
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    GuiStyle.slot(graphics, px + col * 18, py + row * 18, GuiStyle.SLOT_POOL);
                }
            }
            GuiStyle.scrollbar(graphics, px + cols * 18 + 3, py - 1, 10, rows * 18 + 2,
                    scrollOffset, Math.max(0, maxRows - rows), rows, maxRows);
            GuiStyle.meter(graphics, sx + 6, y + imageHeight - 12, SIDE_PANEL_W - 12, 4, fill);

            // Компактна інформаційна панель у центральному вікні замість нижньої сітки
            GuiStyle.panel(graphics, x + POOL_X - 1, y + POOL_Y + 6, 181, 42);
            Component movedMsg = Component.translatable("gui.simple_bytestorage.terminal.side_pool_active");
            int mw = this.font.width(movedMsg);
            graphics.drawString(this.font, movedMsg, x + POOL_X + (179 - mw) / 2, y + POOL_Y + 23, GuiStyle.TEXT_MUTED, false);
        } else {
            // Звичайний пул предметів під крафтовою сіткою (9×3)
            GuiStyle.display(graphics, px - 1, py - 1, 164, 56);
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    GuiStyle.slot(graphics, px + col * 18, py + row * 18, GuiStyle.SLOT_POOL);
                }
            }
            GuiStyle.scrollbar(graphics, x + 172, y + POOL_Y - 1, 10, 56,
                    scrollOffset, Math.max(0, maxRows - rows), rows, maxRows);
        }

        // Пам'ять
        GuiStyle.meter(graphics, x + 8, y + 132, 179, 4, fill);
        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        int invRight = 8 + 13 + this.font.width(this.playerInventoryTitle) + 6;
        int availW = Math.max(40, 187 - invRight);
        String text = Component.translatable("gui.simple_bytestorage.terminal.memory",
                FORMAT.format(used), FORMAT.format(max),
                String.format(Locale.ROOT, "%.0f", fill * 100.0F)).getString();
        if (this.font.width(text) > availW) {
            text = GuiStyle.compactNumber(used) + "/" + GuiStyle.compactNumber(max) + " B ("
                    + String.format(Locale.ROOT, "%.0f", fill * 100.0F) + "%)";
        }
        text = GuiStyle.clip(this.font, text, availW);
        int color = max > 0 && used >= max ? GuiStyle.TEXT_BAD : GuiStyle.TEXT_GOOD;
        graphics.drawString(this.font, text, x + 187 - this.font.width(text), y + this.inventoryLabelY, color, false);

        renderPoolItems(graphics, mouseX, mouseY, filtered);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Усе малюється у renderBg через GuiStyle
    }

    private void renderPoolItems(GuiGraphics graphics, int mouseX, int mouseY, List<ItemStack> filtered) {
        int cols = poolCols();
        int rows = poolRows();
        int px = poolOriginX();
        int py = poolOriginY();
        int start = scrollOffset * cols;
        for (int i = 0; i < cols * rows; i++) {
            int itemX = px + 1 + (i % cols) * 18;
            int itemY = py + 1 + (i / cols) * 18;

            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                graphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, GuiStyle.ACCENT_FAINT);
            }
            if (start + i < filtered.size()) {
                ItemStack stack = filtered.get(start + i);
                graphics.renderItem(stack, itemX, itemY);
                GuiStyle.itemCount(graphics, this.font, stack, itemX, itemY, extras.isGhost(stack) ? TerminalExtras.craftLabel() : formatCount(stack.getCount()));
                if (extras.isGhost(stack)) {
                    graphics.fill(itemX, itemY, itemX + 16, itemY + 16, 0x90141B26);
                }
                if (TerminalExtras.isFavorite(stack)) {
                    TerminalExtras.drawFavorite(graphics, itemX, itemY);
                }
            }
        }
    }

    private void renderPoolTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack stack = stackAt(mouseX, mouseY);
        if (stack.isEmpty() || extras.isDialogOpen()) {
            return;
        }
        List<Component> tooltip = getTooltipFromContainerItem(stack);
        if (extras.isGhost(stack)) {
            tooltip.add(Component.translatable("gui.simple_bytestorage.terminal.craft_only").withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("gui.simple_bytestorage.terminal.stored", FORMAT.format(stack.getCount()))
                    .withStyle(ChatFormatting.GOLD));
            int reservedCount = com.simpleae.bytestorage.client.TerminalAvailabilityClient.reserved(stack);
            if (reservedCount > 0) {
                tooltip.add(Component.translatable("gui.simple_bytestorage.terminal.reserved", FORMAT.format(reservedCount))
                        .withStyle(ChatFormatting.YELLOW));
            }
            if (extras.isCraftable(stack)) {
                tooltip.add(Component.translatable("gui.simple_bytestorage.terminal.craft_hint").withStyle(ChatFormatting.AQUA));
            }
        }
        int craftingCount = com.simpleae.bytestorage.client.TerminalAvailabilityClient.crafting(stack);
        if (craftingCount > 0) {
            tooltip.add(Component.translatable("gui.simple_bytestorage.terminal.in_production", FORMAT.format(craftingCount))
                    .withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("gui.simple_bytestorage.terminal.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("gui.simple_bytestorage.terminal.fav_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private ItemStack stackAt(double mouseX, double mouseY) {
        List<ItemStack> filtered = getFilteredItems();
        int cols = poolCols();
        int rows = poolRows();
        int px = poolOriginX();
        int py = poolOriginY();
        int start = scrollOffset * cols;
        for (int i = 0; i < cols * rows; i++) {
            if (start + i >= filtered.size()) {
                break;
            }
            int itemX = px + 1 + (i % cols) * 18;
            int itemY = py + 1 + (i / cols) * 18;
            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                return filtered.get(start + i);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (extras.clickDialog(mouseX, mouseY, button, this.width, this.height)) {
            return true;
        }
        if (extras.clickSort(mouseX, mouseY, button, this.leftPos, this.topPos, this.imageWidth)) {
            scrollOffset = 0;
            return true;
        }
        this.searchBox.setFocused(this.searchBox.isMouseOver(mouseX, mouseY));
        if (this.searchBox.isFocused()) {
            this.searchBox.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (this.layoutButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.depositButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int px = poolOriginX();
        int py = poolOriginY();
        boolean inPool = mouseX >= px && mouseX < px + 18 * poolCols()
                && mouseY >= py && mouseY < py + 18 * poolRows();
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
            if (!clicked.isEmpty() && hasAltDown()) {
                TerminalExtras.toggleFavorite(clicked);
                return true;
            }
            if (!clicked.isEmpty() && (extras.isGhost(clicked)
                    || (extras.isCraftable(clicked) && (hasControlDown() || button == 2)))) {
                extras.openDialog(clicked);
                return true;
            }
            if (!clicked.isEmpty() && button != 2) {
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
        if (extras.isDialogOpen()) {
            if (keyCode == 256) {
                extras.closeDialog();
            }
            return true;
        }
        if (this.searchBox.isFocused() && keyCode != 256) {
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (extras.isDialogOpen()) {
            return true;
        }
        if (this.searchBox.isFocused()) {
            boolean handled = this.searchBox.charTyped(codePoint, modifiers);
            scrollOffset = 0;
            return handled;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxRows = (getFilteredItems().size() + poolCols() - 1) / poolCols();
        int maxScroll = Math.max(0, maxRows - poolRows());
        if (delta < 0) {
            scrollOffset = Math.min(scrollOffset + 1, maxScroll);
        } else if (delta > 0) {
            scrollOffset = Math.max(scrollOffset - 1, 0);
        }
        return true;
    }

    private List<ItemStack> getFilteredItems() {
        String query = searchBox != null ? searchBox.getValue() : "";
        return extras.view(menu.getClientVirtualPool(), query);
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
