package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.PatternEncodingTerminalBlockEntity;
import com.simpleae.bytestorage.item.PatternItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Меню Терміналу Кодування Шаблонів.
 *
 * Індекси слотів меню:
 *   0        — шаблон (вхід)
 *   1        — закодований шаблон (вихід)
 *   2..10    — сітка рецепту 3×3 (слоти-привиди)
 *   11       — результат рецепту (лише відображення)
 *   12..38   — інвентар гравця (3×9)
 *   39..47   — хотбар
 *
 * Кнопки меню (clickMenuButton): 0 — записати шаблон, 1 — очистити сітку.
 */
public class PatternEncodingTerminalMenu extends AbstractContainerMenu {

    public static final int BUTTON_ENCODE = 0;
    public static final int BUTTON_CLEAR = 1;
    public static final int BUTTON_MODE = 2;

    public static final int SLOT_BLANK = 0;
    public static final int SLOT_ENCODED = 1;
    public static final int SLOT_GRID_START = 2;
    public static final int SLOT_GRID_END = SLOT_GRID_START + 9; // ексклюзивно
    public static final int SLOT_RESULT = SLOT_GRID_END;         // 11
    public static final int PLAYER_START = SLOT_RESULT + 1;      // 12
    public static final int PLAYER_END = PLAYER_START + 36;      // ексклюзивно

    // Координати слотів (позиція предмета; рамка слота малюється на 1 px лівіше/вище)
    public static final int GRID_X = 18;
    public static final int GRID_Y = 36;
    public static final int RESULT_X = 129;
    public static final int RESULT_Y = 54;
    public static final int BLANK_X = 15;
    public static final int ENCODED_X = 145;
    public static final int BOTTOM_ROW_Y = 101;
    public static final int PLAYER_INV_Y = 139;

    private final PatternEncodingTerminalBlockEntity encoder;
    private final ContainerData data;

    public PatternEncodingTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (PatternEncodingTerminalBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public PatternEncodingTerminalMenu(int containerId, Inventory playerInventory, PatternEncodingTerminalBlockEntity encoder) {
        super(SimpleByteStorage.PATTERN_ENCODING_TERMINAL_MENU.get(), containerId);
        this.encoder = encoder;

        // Режим кодування синхронізується з сервера (на клієнті encoder теж існує, але його стан не оновлюється)
        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(1)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return encoder.isProcessing() ? 1 : 0;
                    }

                    @Override
                    public void set(int index, int value) {
                    }

                    @Override
                    public int getCount() {
                        return 1;
                    }
                };
        this.addDataSlots(this.data);

        // 0: шаблон
        this.addSlot(new SlotItemHandler(encoder.getInventoryHandler(),
                PatternEncodingTerminalBlockEntity.BLANK_PATTERN_SLOT, BLANK_X, BOTTOM_ROW_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof PatternItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 1: закодований шаблон (тільки забрати)
        this.addSlot(new SlotItemHandler(encoder.getInventoryHandler(),
                PatternEncodingTerminalBlockEntity.PROGRAMMED_PATTERN_SLOT, ENCODED_X, BOTTOM_ROW_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        // 2..10: сітка рецепту — слоти-привиди (керуються в clicked())
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new SlotItemHandler(encoder.getInventoryHandler(),
                        PatternEncodingTerminalBlockEntity.RECIPE_GRID_START + col + row * 3,
                        GRID_X + col * 18, GRID_Y + row * 18) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
            }
        }

        // 11: результат рецепту (відображення)
        this.addSlot(new SlotItemHandler(encoder.getInventoryHandler(),
                PatternEncodingTerminalBlockEntity.RESULT_PREVIEW_SLOT, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        // Інвентар гравця 3×9 + хотбар
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public boolean isProcessing() {
        return data.get(0) != 0;
    }

    public PatternEncodingTerminalBlockEntity getEncoder() {
        return encoder;
    }

    // --- Слоти-привиди ---

    private static boolean isGridSlot(int slotId) {
        return slotId >= SLOT_GRID_START && slotId < SLOT_GRID_END;
    }

    private static void setGhost(Slot slot, ItemStack source) {
        setGhost(slot, source, 1);
    }

    private static void setGhost(Slot slot, ItemStack source, int count) {
        if (source.isEmpty() || count <= 0) {
            slot.set(ItemStack.EMPTY);
            return;
        }
        ItemStack ghost = source.copy();
        ghost.setCount(Math.min(count, ghost.getMaxStackSize()));
        slot.set(ghost);
    }

    /** Клік по слоту-привиду в процесному режимі: ЛКМ — стак з курсору, ПКМ — +1, порожня рука — очистити. */
    private void clickCountedGhost(Slot slot, int button, ItemStack carried) {
        ItemStack current = slot.getItem();
        if (carried.isEmpty()) {
            if (button == 1 && !current.isEmpty() && current.getCount() > 1) {
                ItemStack less = current.copy();
                less.shrink(1);
                slot.set(less);
            } else {
                slot.set(ItemStack.EMPTY);
            }
            return;
        }
        if (button == 1) {
            if (!current.isEmpty() && ItemStack.isSameItemSameTags(current, carried)) {
                setGhost(slot, carried, current.getCount() + 1);
            } else {
                setGhost(slot, carried, 1);
            }
        } else {
            setGhost(slot, carried, carried.getCount());
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean processing = isProcessing();
        boolean ghostSlot = isGridSlot(slotId) || (processing && slotId == SLOT_RESULT);
        if (ghostSlot
                && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP)) {
            Slot slot = this.slots.get(slotId);
            if (clickType == ClickType.SWAP) {
                if (button >= 0 && button < 9) {
                    ItemStack from = player.getInventory().getItem(button);
                    setGhost(slot, from, processing ? from.getCount() : 1);
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                setGhost(slot, ItemStack.EMPTY);
            } else if (processing) {
                clickCountedGhost(slot, button, this.getCarried());
            } else {
                // PICKUP: предмет з курсору «копіюється» у клітинку, порожня рука — очищує її.
                setGhost(slot, this.getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    // --- Кнопки ---

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (encoder == null || player.level().isClientSide) {
            return true;
        }
        if (id == BUTTON_ENCODE) {
            Component message = encoder.encodePattern();
            player.displayClientMessage(message, true);
            return true;
        }
        if (id == BUTTON_CLEAR) {
            encoder.clearRecipeGrid();
            return true;
        }
        if (id == BUTTON_MODE) {
            encoder.toggleProcessing();
            return true;
        }
        return false;
    }

    // --- Shift-клік ---

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stackInSlot = slot.getItem();
        ItemStack original = stackInSlot.copy();

        if (index == SLOT_BLANK || index == SLOT_ENCODED) {
            if (!this.moveItemStackTo(stackInSlot, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_START && index < PLAYER_END) {
            if (stackInSlot.getItem() instanceof PatternItem) {
                if (!this.moveItemStackTo(stackInSlot, SLOT_BLANK, SLOT_BLANK + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Не-шаблон: «намалювати» у першій вільній клітинці сітки (предмет не витрачається).
                boolean processing = isProcessing();
                for (int i = SLOT_GRID_START; i < SLOT_GRID_END; i++) {
                    Slot gridSlot = this.slots.get(i);
                    if (processing && gridSlot.hasItem()
                            && ItemStack.isSameItemSameTags(gridSlot.getItem(), stackInSlot)) {
                        setGhost(gridSlot, stackInSlot, gridSlot.getItem().getCount() + 1);
                        break;
                    }
                    if (!gridSlot.hasItem()) {
                        setGhost(gridSlot, stackInSlot);
                        break;
                    }
                }
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (encoder == null) {
            return false;
        }
        Level level = encoder.getNodeLevel();
        return level != null
                && level.getBlockState(encoder.getNodePos()).is(SimpleByteStorage.PATTERN_ENCODING_TERMINAL_BLOCK.get())
                && player.distanceToSqr(encoder.getNodePos().getX() + 0.5, encoder.getNodePos().getY() + 0.5,
                encoder.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
