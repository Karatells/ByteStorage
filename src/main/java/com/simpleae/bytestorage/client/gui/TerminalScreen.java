package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.TerminalMenu;
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
 * Клієнтський екран Термінала: віртуальний пул предметів, пошук, швидке вкладання, індикатор пам'яті.
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {

    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);
    private static final int ITEMS_PER_ROW = 9;
    private static final int VISIBLE_ROWS = 5;
    private static final int POOL_X = 7;
    private static final int POOL_Y = 20;

    private EditBox searchBox;
    private UiButton depositButton;
    private UiButton fluidsButton;
    private boolean fluidTab = false;
    private int scrollOffset = 0;
    private final TerminalExtras extras = new TerminalExtras();

    public TerminalScreen(TerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 195;
        this.imageHeight = 222;
        this.inventoryLabelY = 127;
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(this.font, this.leftPos + 85, this.topPos + 5, 74, 10, Component.empty());
        this.searchBox.setMaxLength(60);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(GuiStyle.TEXT_TITLE);
        this.searchBox.setHint(Component.translatable("gui.simple_bytestorage.terminal.search")
                .withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.searchBox);
        this.extras.bindSearch(() -> this.searchBox.getValue(), q -> this.searchBox.setValue(q));

        this.depositButton = new UiButton(this.leftPos + 150, this.topPos + 4, 14, 11,
                () -> Component.empty(), UiButton.Icon.DOWN, () -> {
            int flag = hasShiftDown() ? 1 : 0;
            SimpleByteStoragePackets.sendToServer(
                    new TerminalActionPacket(new ItemStack(Items.AIR, flag), TerminalActionPacket.ACTION_DEPOSIT_ALL));
        });
        this.depositButton.tooltip(Component.translatable("gui.simple_bytestorage.terminal.deposit"));

        this.fluidsButton = new UiButton(this.leftPos + this.imageWidth + 2, this.topPos + 92, 40, 14,
                () -> Component.translatable(this.fluidTab
                        ? "gui.simple_bytestorage.terminal.tab.fluids"
                        : "gui.simple_bytestorage.terminal.tab.items"),
                UiButton.Icon.NONE, () -> {
            this.fluidTab = !this.fluidTab;
            this.scrollOffset = 0;
        });
        this.fluidsButton.tooltip(Component.translatable("gui.simple_bytestorage.terminal.tab.tip"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.depositButton.render(graphics, this.font, mouseX, mouseY);
        this.fluidsButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.depositButton.renderTooltip(graphics, this.font, mouseX, mouseY);
        this.fluidsButton.renderTooltip(graphics, this.font, mouseX, mouseY);
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

        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led, "monitor");

        // Поле пошуку (текст малює EditBox без власної рамки)
        GuiStyle.display(graphics, x + 82, y + 4, 80, 11);

        // Пул предметів
        GuiStyle.display(graphics, x + POOL_X - 1, y + POOL_Y - 1, 164, 92);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                GuiStyle.slot(graphics, x + POOL_X + col * 18, y + POOL_Y + row * 18, GuiStyle.SLOT_POOL);
            }
        }
        List<ItemStack> filtered = getFilteredItems();
        int maxRows = (filtered.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, maxRows - VISIBLE_ROWS)));
        GuiStyle.scrollbar(graphics, x + 172, y + POOL_Y - 1, 10, 92,
                scrollOffset, Math.max(0, maxRows - VISIBLE_ROWS), VISIBLE_ROWS, maxRows);

        // Пам'ять
        GuiStyle.meter(graphics, x + 8, y + 114, 179, 6, fill);
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

        GuiStyle.playerSlots(graphics, x, y, menu.slots, 0);

        if (fluidTab) {
            renderFluidPool(graphics, mouseX, mouseY);
        } else {
            renderPoolItems(graphics, mouseX, mouseY, filtered);
        }
    }

    private void renderFluidPool(GuiGraphics graphics, int mouseX, int mouseY) {
        List<net.minecraftforge.fluids.FluidStack> fluids = menu.getClientFluids();
        int start = scrollOffset * ITEMS_PER_ROW;
        for (int i = 0; i < VISIBLE_ROWS * ITEMS_PER_ROW; i++) {
            int col = i % ITEMS_PER_ROW;
            int row = i / ITEMS_PER_ROW;
            int slotX = this.leftPos + POOL_X + col * 18;
            int slotY = this.topPos + POOL_Y + row * 18;
            if (start + i < fluids.size()) {
                net.minecraftforge.fluids.FluidStack fs = fluids.get(start + i);
                ItemStack icon = new ItemStack(fs.getFluid().getBucket());
                if (icon.isEmpty()) icon = new ItemStack(Items.WATER_BUCKET);
                graphics.renderItem(icon, slotX + 1, slotY + 1);
                String amt = fs.getAmount() >= 1000
                        ? String.format(Locale.ROOT, "%.1fB", fs.getAmount() / 1000.0)
                        : fs.getAmount() + "m";
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.pose().scale(0.5F, 0.5F, 1.0F);
                int tx = (int) ((slotX + 17) * 2) - this.font.width(amt);
                int ty = (int) ((slotY + 13) * 2);
                graphics.drawString(this.font, amt, tx, ty, GuiStyle.ACCENT, true);
                graphics.pose().popPose();
            }
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x80FFFFFF);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Усе малюється у renderBg через GuiStyle
    }

    private void renderPoolItems(GuiGraphics graphics, int mouseX, int mouseY, List<ItemStack> filtered) {
        int start = scrollOffset * ITEMS_PER_ROW;
        for (int i = 0; i < ITEMS_PER_ROW * VISIBLE_ROWS; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            int itemX = this.leftPos + POOL_X + 1 + col * 18;
            int itemY = this.topPos + POOL_Y + 1 + row * 18;

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
        if (this.fluidTab) {
            List<net.minecraftforge.fluids.FluidStack> fluids = menu.getClientFluids();
            int start = scrollOffset * ITEMS_PER_ROW;
            for (int i = 0; i < VISIBLE_ROWS * ITEMS_PER_ROW && start + i < fluids.size(); i++) {
                int slotX = this.leftPos + POOL_X + (i % ITEMS_PER_ROW) * 18;
                int slotY = this.topPos + POOL_Y + (i / ITEMS_PER_ROW) * 18;
                if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                    net.minecraftforge.fluids.FluidStack fs = fluids.get(start + i);
                    graphics.renderComponentTooltip(this.font, List.of(
                            fs.getDisplayName(),
                            Component.literal(FORMAT.format(fs.getAmount()) + " mB").withStyle(ChatFormatting.AQUA),
                            Component.translatable("gui.simple_bytestorage.terminal.fluid.hint").withStyle(ChatFormatting.GRAY)
                    ), mouseX, mouseY);
                    return;
                }
            }
            return;
        }
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

    /** Предмет пулу під курсором або порожній стак. */
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
        if (extras.clickDialog(mouseX, mouseY, button, this.width, this.height)) {
            return true;
        }
        if (extras.clickSort(mouseX, mouseY, button, this.leftPos, this.topPos, this.imageWidth)) {
            scrollOffset = 0;
            return true;
        }
        // Пошук: клік всередині фокусує, поза ним — знімає фокус
        this.searchBox.setFocused(this.searchBox.isMouseOver(mouseX, mouseY));
        if (this.searchBox.isFocused()) {
            this.searchBox.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (this.depositButton.mouseClicked(mouseX, mouseY, button)
                || this.fluidsButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.fluidTab && mouseX >= this.leftPos + POOL_X && mouseX < this.leftPos + POOL_X + ITEMS_PER_ROW * 18
                && mouseY >= this.topPos + POOL_Y && mouseY < this.topPos + POOL_Y + VISIBLE_ROWS * 18) {
            int col = (int) ((mouseX - (this.leftPos + POOL_X)) / 18);
            int row = (int) ((mouseY - (this.topPos + POOL_Y)) / 18);
            int idx = scrollOffset * ITEMS_PER_ROW + row * ITEMS_PER_ROW + col;
            List<net.minecraftforge.fluids.FluidStack> fluids = menu.getClientFluids();
            net.minecraft.resources.ResourceLocation id = idx >= 0 && idx < fluids.size()
                    ? net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluids.get(idx).getFluid())
                    : null;
            SimpleByteStoragePackets.sendToServer(
                    new com.simpleae.bytestorage.network.packet.FluidTerminalActionPacket(id));
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
