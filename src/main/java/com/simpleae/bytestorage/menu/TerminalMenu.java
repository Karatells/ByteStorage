package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.network.packet.SyncTerminalDataPacket;
import com.simpleae.bytestorage.network.packet.TerminalActionPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Контейнер-меню Термінала Simple Byte Storage (Terminal Menu).
 * 
 * Забезпечує зв'язок між клієнтським GUI та віртуальним сховищем INetworkStorage.
 * Підтримує відображення списку предметів без прив'язки до фіксованих слотів,
 * вилучення предметів за кліком і вставку предметів з інвентаря гравця.
 */
public class TerminalMenu extends AbstractContainerMenu implements IGridMenu {

    private int craftSyncTimer = 0;

    @Override
    public com.simpleae.bytestorage.api.network.IGrid getMenuGrid() {
        return terminalEntity == null ? null : terminalEntity.getGrid();
    }


    private final IGridNode terminalEntity;
    private final Player player;
    private final boolean wireless;

    // Клієнтський стан сховища
    private int clientTotalCapacity = 0;
    private int clientTotalUsed = 0;
    private List<ItemStack> clientVirtualPool = new ArrayList<>();

    private int lastCapacity = -1;
    private int lastUsed = -1;

    // Клієнтський конструктор
    public TerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, (IGridNode) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // Серверний конструктор
    public TerminalMenu(int containerId, Inventory playerInventory, IGridNode terminalEntity) {
        this(containerId, playerInventory, terminalEntity, false);
    }

    /** @param wireless true — відкрито бездротовим терміналом: відстань до блока не обмежена. */
    public TerminalMenu(int containerId, Inventory playerInventory, IGridNode terminalEntity, boolean wireless) {
        super(SimpleByteStorage.TERMINAL_MENU.get(), containerId);
        this.wireless = wireless;
        this.terminalEntity = terminalEntity;
        this.player = playerInventory.player;

        // Додаємо слоти інвентаря гравця (нижня частина інтерфейсу)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Хотбар гравця
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (terminalEntity == null) return false;
        if (wireless) {
            net.minecraft.world.level.Level lvl = this.terminalEntity.getNodeLevel();
            return lvl != null && lvl.getBlockEntity(this.terminalEntity.getNodePos()) == this.terminalEntity;
        }
        return stillValid(this.terminalEntity.getNodeLevel(), this.terminalEntity.getNodePos(), player, SimpleByteStorage.TERMINAL_BLOCK.get());
    }

    private static boolean stillValid(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, Player player, net.minecraft.world.level.block.Block targetBlock) {
        return level != null && level.getBlockState(pos).is(targetBlock) && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player instanceof ServerPlayer craftSyncPlayer && (craftSyncTimer++ % 40 == 0)) {
            sendCraftables(craftSyncPlayer);
        }

        // Серверна синхронізація пулу предметів з клієнтом
        if (player instanceof ServerPlayer serverPlayer && terminalEntity != null) {
            IGrid grid = terminalEntity.getGrid();
            if (grid != null && grid.getState().isActive()) {
                INetworkStorage storage = grid.getStorage();
                int currentCap = storage.getTotalCapacityBytes();
                int currentUsed = storage.getTotalUsedBytes();

                // Оновлюємо при зміні байтів або періодично
                if (currentCap != lastCapacity || currentUsed != lastUsed || serverPlayer.tickCount % 20 == 0) {
                    this.lastCapacity = currentCap;
                    this.lastUsed = currentUsed;
                    SimpleByteStoragePackets.sendToPlayer(
                            new SyncTerminalDataPacket(currentCap, currentUsed, storage.getAllItems()),
                            serverPlayer
                    );
                    if (serverPlayer.tickCount % 20 == 0) {
                        SimpleByteStoragePackets.sendToPlayer(new com.simpleae.bytestorage.network.packet.TerminalAvailabilityPacket(
                                com.simpleae.bytestorage.util.TerminalAvailability.compute(grid)), serverPlayer);
                    }
                }
            }
        }
    }

    /**
     * Швидке переміщення (Shift-клік) з інвентаря гравця у віртуальне сховище термінала.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();

        // Клієнтська сторона
        if (player.level().isClientSide()) {
            int freeBytes = clientTotalCapacity - clientTotalUsed;
            if (freeBytes > 0) {
                int toMove = Math.min(stackInSlot.getCount(), freeBytes);
                stackInSlot.shrink(toMove);
                slot.setChanged();
                return copy;
            }
            return ItemStack.EMPTY;
        }

        // Серверна сторона
        if (terminalEntity != null && terminalEntity.getGrid() != null && terminalEntity.getGrid().getState().isActive()) {
            INetworkStorage storage = terminalEntity.getGrid().getStorage();
            ItemStack remainder = storage.insertItem(stackInSlot, false);

            if (remainder.getCount() == copy.getCount()) {
                // Сховище повністю заповнене або предмет не вдалося вставити
                return ItemStack.EMPTY;
            }

            slot.set(remainder);
            slot.setChanged();

            if (player instanceof ServerPlayer serverPlayer) {
                broadcastChanges();
            }
            // Повертаємо copy тільки якщо весь слот спорожнів, інакше EMPTY,
            // щоб уникнути нескінченного циклу у ванільному AbstractContainerMenu.clicked
            return remainder.isEmpty() ? copy : ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    /**
     * Обробка клієнтських дій (вилучення або вставка через клік по терміналу).
     */
    public void handleClientAction(ItemStack targetItem, int actionType, ServerPlayer serverPlayer) {
        if (terminalEntity == null || terminalEntity.getGrid() == null || !terminalEntity.getGrid().getState().isActive()) {
            return;
        }

        INetworkStorage storage = terminalEntity.getGrid().getStorage();

        switch (actionType) {
            case TerminalActionPacket.ACTION_EXTRACT_STACK -> {
                int toExtract = Math.min(targetItem.getMaxStackSize(), 64);
                ItemStack carried = getCarried();

                if (carried.isEmpty()) {
                    ItemStack extracted = storage.extractItem(targetItem, toExtract, false);
                    setCarried(extracted);
                } else if (ItemStack.isSameItemSameTags(carried, targetItem)) {
                    int space = carried.getMaxStackSize() - carried.getCount();
                    int extractCount = Math.min(space, toExtract);
                    if (extractCount > 0) {
                        ItemStack extracted = storage.extractItem(targetItem, extractCount, false);
                        carried.grow(extracted.getCount());
                    }
                }
            }
            case TerminalActionPacket.ACTION_EXTRACT_HALF -> {
                int toExtract = Math.max(1, Math.min(targetItem.getMaxStackSize() / 2, 32));
                ItemStack carried = getCarried();

                if (carried.isEmpty()) {
                    ItemStack extracted = storage.extractItem(targetItem, toExtract, false);
                    setCarried(extracted);
                }
            }
            case TerminalActionPacket.ACTION_EXTRACT_SHIFT -> {
                // Вилучити стак прямо в інвентар гравця
                int toExtract = Math.min(targetItem.getMaxStackSize(), 64);
                ItemStack extracted = storage.extractItem(targetItem, toExtract, false);
                if (!extracted.isEmpty()) {
                    if (!serverPlayer.getInventory().add(extracted)) {
                        // Якщо інвентар переповнився, повертаємо залишок назад у сховище
                        storage.insertItem(extracted, false);
                    }
                }
            }
            case TerminalActionPacket.ACTION_INSERT_CURSOR -> {
                ItemStack carried = getCarried();
                if (!carried.isEmpty()) {
                    ItemStack remainder = storage.insertItem(carried, false);
                    setCarried(remainder);
                }
            }
            case TerminalActionPacket.ACTION_INSERT_ONE -> {
                ItemStack carried = getCarried();
                if (!carried.isEmpty()) {
                    ItemStack single = carried.copyWithCount(1);
                    ItemStack remainder = storage.insertItem(single, false);
                    if (remainder.isEmpty()) {
                        carried.shrink(1);
                        setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                    }
                }
            }
            case TerminalActionPacket.ACTION_DEPOSIT_ALL -> {
                // Вкласти інвентар гравця
                boolean includeHotbar = targetItem != null && targetItem.getCount() == 1;
                int startSlot = includeHotbar ? 0 : 9;
                for (int i = startSlot; i < 36; i++) {
                    ItemStack stack = serverPlayer.getInventory().getItem(i);
                    if (!stack.isEmpty()) {
                        ItemStack remainder = storage.insertItem(stack, false);
                        serverPlayer.getInventory().setItem(i, remainder);
                    }
                }
            }
        }

        broadcastChanges();
    }

    // --- Клієнтські методи оновлення ---

    public void updateClientData(int capacity, int used, List<ItemStack> items) {
        this.clientTotalCapacity = capacity;
        this.clientTotalUsed = used;
        this.clientVirtualPool = new ArrayList<>(items);
    }

    public int getClientTotalCapacity() {
        return clientTotalCapacity;
    }

    public int getClientTotalUsed() {
        return clientTotalUsed;
    }

    public List<ItemStack> getClientVirtualPool() {
        return Collections.unmodifiableList(clientVirtualPool);
    }

    public IGridNode getTerminalEntity() {
        return terminalEntity;
    }
}
