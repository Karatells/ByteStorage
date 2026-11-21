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
    private List<net.minecraftforge.fluids.FluidStack> clientFluids = new ArrayList<>();

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
        this(SimpleByteStorage.TERMINAL_MENU.get(), containerId, playerInventory, terminalEntity, wireless);
    }

    public TerminalMenu(net.minecraft.world.inventory.MenuType<?> menuType, int containerId, Inventory playerInventory, IGridNode terminalEntity, boolean wireless) {
        super(menuType, containerId);
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
        if (terminalEntity instanceof com.simpleae.bytestorage.item.PortableByteCellItem.PortableCellNode portable) {
            return portable.isValid(player);
        }
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
        boolean isPortable = terminalEntity instanceof com.simpleae.bytestorage.item.PortableByteCellItem.PortableCellNode;
        if (!isPortable && player instanceof ServerPlayer craftSyncPlayer && (craftSyncTimer++ % 40 == 0)) {
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
                    if (!isPortable && serverPlayer.tickCount % 20 == 0) {
                        SimpleByteStoragePackets.sendToPlayer(new com.simpleae.bytestorage.network.packet.TerminalAvailabilityPacket(
                                com.simpleae.bytestorage.util.TerminalAvailability.compute(grid)), serverPlayer);
                        SimpleByteStoragePackets.sendToPlayer(
                                new com.simpleae.bytestorage.network.packet.SyncFluidsPacket(collectGridFluids(grid)),
                                serverPlayer);
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

    public void updateClientFluids(List<net.minecraftforge.fluids.FluidStack> fluids) {
        this.clientFluids = new ArrayList<>(fluids);
    }

    public List<net.minecraftforge.fluids.FluidStack> getClientFluids() {
        return Collections.unmodifiableList(clientFluids);
    }

    private static List<net.minecraftforge.fluids.FluidStack> collectGridFluids(IGrid grid) {
        java.util.Map<net.minecraft.world.level.material.Fluid, net.minecraftforge.fluids.FluidStack> merged = new java.util.LinkedHashMap<>();
        for (IGridNode node : grid.getNodes()) {
            if (!(node instanceof com.simpleae.bytestorage.block.entity.DriveBlockEntity drive)) continue;
            net.minecraftforge.items.IItemHandler inv = drive.getDriveInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack cell = inv.getStackInSlot(i);
                if (cell.getItem() instanceof com.simpleae.bytestorage.item.FluidCellItem) {
                    for (net.minecraftforge.fluids.FluidStack fs : com.simpleae.bytestorage.item.FluidCellItem.getStoredFluids(cell)) {
                        if (fs.isEmpty()) continue;
                        net.minecraftforge.fluids.FluidStack existing = merged.get(fs.getFluid());
                        if (existing == null) {
                            merged.put(fs.getFluid(), fs.copy());
                        } else {
                            existing.grow(fs.getAmount());
                        }
                    }
                }
            }
        }
        return new ArrayList<>(merged.values());
    }

    /** Налить жидкость в пустое ведро (1000 mB) или слить полное ведро в FluidCellItem сети. */
    public void handleFluidAction(@org.jetbrains.annotations.Nullable net.minecraft.resources.ResourceLocation fluidId, ServerPlayer serverPlayer) {
        if (terminalEntity == null || terminalEntity.getGrid() == null || !terminalEntity.getGrid().getState().isActive()) {
            return;
        }
        IGrid grid = terminalEntity.getGrid();
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;

        // 1. Слив полного ведра в сеть
        if (carried.getItem() instanceof net.minecraft.world.item.BucketItem bucket
                && bucket.getFluid() != net.minecraft.world.level.material.Fluids.EMPTY) {
            net.minecraftforge.fluids.FluidStack toInsert = new net.minecraftforge.fluids.FluidStack(bucket.getFluid(), 1000);
            int remaining = 1000;
            for (IGridNode node : grid.getNodes()) {
                if (!(node instanceof com.simpleae.bytestorage.block.entity.DriveBlockEntity drive) || drive.isLocked()) continue;
                net.minecraftforge.items.IItemHandler inv = drive.getDriveInventory();
                for (int i = 0; i < inv.getSlots() && remaining > 0; i++) {
                    ItemStack cell = inv.getStackInSlot(i);
                    if (cell.getItem() instanceof com.simpleae.bytestorage.item.FluidCellItem fc) {
                        int fit = fc.insertFluid(cell, new net.minecraftforge.fluids.FluidStack(bucket.getFluid(), remaining), true);
                        if (fit == remaining) {
                            fc.insertFluid(cell, toInsert, false);
                            drive.setChanged();
                            remaining = 0;
                        }
                    }
                }
            }
            if (remaining == 0) {
                setCarried(new ItemStack(net.minecraft.world.item.Items.BUCKET));
                SimpleByteStoragePackets.sendToPlayer(
                        new com.simpleae.bytestorage.network.packet.SyncFluidsPacket(collectGridFluids(grid)), serverPlayer);
            }
            return;
        }

        // 2. Налив жидкости в пустое ведро
        if (carried.is(net.minecraft.world.item.Items.BUCKET) && fluidId != null) {
            net.minecraft.world.level.material.Fluid target = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(fluidId);
            if (target == null || target == net.minecraft.world.level.material.Fluids.EMPTY) return;
            ItemStack filledBucket = new ItemStack(target.getBucket());
            if (filledBucket.isEmpty()) return;

            // Проверяем, есть ли 1000 mB нужной жидкости в сети
            int avail = 0;
            for (net.minecraftforge.fluids.FluidStack fs : collectGridFluids(grid)) {
                if (fs.getFluid() == target) {
                    avail = fs.getAmount();
                    break;
                }
            }
            if (avail < 1000) return;

            int need = 1000;
            for (IGridNode node : grid.getNodes()) {
                if (!(node instanceof com.simpleae.bytestorage.block.entity.DriveBlockEntity drive)) continue;
                net.minecraftforge.items.IItemHandler inv = drive.getDriveInventory();
                for (int i = 0; i < inv.getSlots() && need > 0; i++) {
                    ItemStack cell = inv.getStackInSlot(i);
                    if (cell.getItem() instanceof com.simpleae.bytestorage.item.FluidCellItem) {
                        List<net.minecraftforge.fluids.FluidStack> list = com.simpleae.bytestorage.item.FluidCellItem.getStoredFluids(cell);
                        boolean changed = false;
                        for (int j = 0; j < list.size() && need > 0; j++) {
                            net.minecraftforge.fluids.FluidStack fs = list.get(j);
                            if (fs.getFluid() == target && !fs.isEmpty()) {
                                int take = Math.min(need, fs.getAmount());
                                fs.shrink(take);
                                need -= take;
                                if (fs.isEmpty()) list.remove(j--);
                                changed = true;
                            }
                        }
                        if (changed) {
                            com.simpleae.bytestorage.item.FluidCellItem.saveFluids(cell, list);
                            drive.setChanged();
                        }
                    }
                }
            }
            if (need == 0) {
                if (carried.getCount() == 1) {
                    setCarried(filledBucket);
                } else {
                    carried.shrink(1);
                    if (!serverPlayer.getInventory().add(filledBucket)) {
                        serverPlayer.drop(filledBucket, false);
                    }
                }
                SimpleByteStoragePackets.sendToPlayer(
                        new com.simpleae.bytestorage.network.packet.SyncFluidsPacket(collectGridFluids(grid)), serverPlayer);
            }
        }
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
