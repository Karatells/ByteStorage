package com.simpleae.bytestorage.item;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.CellTier;
import com.simpleae.bytestorage.api.storage.ICellInventory;
import com.simpleae.bytestorage.api.storage.IDriveHost;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.api.storage.IPatternStorage;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.menu.TerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Портативная байт-ячейка (1k / 4k / 16k):
 * - Работает как ручной терминал без лимита на типы предметов (ПКМ в руке).
 * - Shift+ПКМ переключает авто-подбор лута с земли напрямую в ячейку (с учётом фильтра Cell Workbench).
 * - Совместима с Drive и Cell Workbench, так как наследует ByteStorageCellItem.
 */
public class PortableByteCellItem extends ByteStorageCellItem {

    public static final String TAG_PICKUP = "AutoPickup";

    public PortableByteCellItem(CellTier tier, Properties properties) {
        super(tier, properties);
    }

    public static boolean isAutoPickup(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_PICKUP);
    }

    public static void setAutoPickup(ItemStack stack, boolean enabled) {
        stack.getOrCreateTag().putBoolean(TAG_PICKUP, enabled);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                boolean next = !isAutoPickup(held);
                setAutoPickup(held, next);
                player.displayClientMessage(
                        Component.translatable(next
                                ? "msg.simple_bytestorage.portable_cell.pickup_on"
                                : "msg.simple_bytestorage.portable_cell.pickup_off")
                                .withStyle(next ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int invSlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
            PortableCellNode node = new PortableCellNode(serverPlayer, invSlot, held);
            Component title = held.getHoverName();
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return title;
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player p) {
                    return new TerminalMenu(SimpleByteStorage.PORTABLE_CELL_MENU.get(), containerId, inv, node, true);
                }
            }, buf -> buf.writeVarInt(invSlot));
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    /** Пытается принять поднятый предмет в первую активную портативную ячейку с AutoPickup в инвентаре игрока. */
    public static ItemStack tryAutoPickup(Player player, ItemStack incoming) {
        if (incoming.isEmpty() || incoming.getItem() instanceof PortableByteCellItem) {
            return incoming;
        }
        ItemStack rest = incoming;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && !rest.isEmpty(); i++) {
            ItemStack slot = inv.getItem(i);
            if (slot.getItem() instanceof PortableByteCellItem cell && isAutoPickup(slot)) {
                ICellInventory cellInv = cell.getCellInventory(slot);
                rest = cellInv.insertItem(rest, false);
                cellInv.flush();
            }
        }
        return rest;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        boolean pickup = isAutoPickup(stack);
        tooltip.add(Component.translatable(pickup
                ? "tooltip.simple_bytestorage.portable_cell.pickup_on"
                : "tooltip.simple_bytestorage.portable_cell.pickup_off")
                .withStyle(pickup ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
    }

    /** Виртуальный IGridNode + IGrid + INetworkStorage поверх одной портативной ячейки в инвентаре игрока. */
    public static final class PortableCellNode implements IGridNode {
        private final ServerPlayer owner;
        private final int slotIndex;
        private final IGrid virtualGrid;

        public PortableCellNode(ServerPlayer owner, int slotIndex, ItemStack initialStack) {
            this.owner = owner;
            this.slotIndex = slotIndex;
            INetworkStorage storage = new PortableCellStorage(this);
            UUID id = UUID.randomUUID();
            this.virtualGrid = new IGrid() {
                @Override public UUID getGridId() { return id; }
                @Override public GridState getState() { return isValid(owner) ? GridState.ONLINE : GridState.OFFLINE; }
                @Override public void setState(GridState newState) {}
                @Override @Nullable public NetworkControllerBlockEntity getController() { return null; }
                @Override public INetworkStorage getStorage() { return storage; }
                @Override public IPatternStorage getPatternStorage() { return null; }
                @Override public Collection<IGridNode> getNodes() { return List.of(PortableCellNode.this); }
                @Override public void addNode(IGridNode node) {}
                @Override public void removeNode(IGridNode node) {}
                @Override public int getTotalEnergyDrain() { return 0; }
                @Override public void validateNetwork() {}
            };
        }

        public ItemStack currentStack() {
            return slotIndex == Inventory.SLOT_OFFHAND
                    ? owner.getOffhandItem()
                    : owner.getInventory().getItem(slotIndex);
        }

        public boolean isValid(Player player) {
            if (player != owner) return false;
            ItemStack s = currentStack();
            return !s.isEmpty() && s.getItem() instanceof PortableByteCellItem;
        }

        @Override public @Nullable IGrid getGrid() { return virtualGrid; }
        @Override public void setGrid(@Nullable IGrid grid) {}
        @Override public BlockPos getNodePos() { return owner.blockPosition(); }
        @Override public Level getNodeLevel() { return owner.level(); }
        @Override public int getIdleEnergyDrain() { return 0; }
        @Override public void onGridStateChanged(GridState newState) {}
    }

    private static final class PortableCellStorage implements INetworkStorage {
        private final PortableCellNode node;

        PortableCellStorage(PortableCellNode node) {
            this.node = node;
        }

        @Nullable
        private ICellInventory openCell() {
            ItemStack s = node.currentStack();
            if (s.getItem() instanceof PortableByteCellItem item) {
                return item.getCellInventory(s);
            }
            return null;
        }

        @Override
        public int getTotalCapacityBytes() {
            ItemStack s = node.currentStack();
            return s.getItem() instanceof PortableByteCellItem item ? item.getMaxBytes(s) : 0;
        }

        @Override
        public int getTotalUsedBytes() {
            ItemStack s = node.currentStack();
            return s.getItem() instanceof PortableByteCellItem item ? item.getUsedBytes(s) : 0;
        }

        @Override
        public List<ItemStack> getAllItems() {
            ICellInventory inv = openCell();
            return inv == null ? List.of() : inv.getStoredItems();
        }

        @Override
        public ItemStack insertItem(ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || stack.getItem() instanceof PortableByteCellItem) {
                return stack;
            }
            ICellInventory inv = openCell();
            if (inv == null) return stack;
            ItemStack rem = inv.insertItem(stack, simulate);
            if (!simulate) inv.flush();
            return rem;
        }

        @Override
        public ItemStack extractItem(ItemStack template, int count, boolean simulate) {
            ICellInventory inv = openCell();
            if (inv == null) return ItemStack.EMPTY;
            ItemStack out = inv.extractItem(template, count, simulate);
            if (!simulate) inv.flush();
            return out;
        }

        @Override
        public ItemStack extractItem(Item item, int count, boolean simulate) {
            return extractItem(new ItemStack(item), count, simulate);
        }

        @Override public void attachDrive(IDriveHost drive) {}
        @Override public void detachDrive(IDriveHost drive) {}
        @Override public void invalidateCache() {}
    }
}
