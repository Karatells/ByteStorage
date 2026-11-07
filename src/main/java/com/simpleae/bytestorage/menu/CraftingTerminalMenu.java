package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.block.entity.CraftingTerminalBlockEntity;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.network.packet.SyncCraftingTerminalDataPacket;
import com.simpleae.bytestorage.network.packet.TerminalActionPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Контейнер-меню Термінала Крафту Simple Byte Storage (Crafting Terminal Menu).
 *
 * Поєднує функціональність звичайного термінала (перегляд сховища)
 * з крафтовою сіткою 3x3 та слотом результату.
 * Предмети для крафту беруться з інвентаря гравця або сховища.
 */
public class CraftingTerminalMenu extends AbstractContainerMenu implements IGridMenu {

    private int craftSyncTimer = 0;

    @Override
    public com.simpleae.bytestorage.api.network.IGrid getMenuGrid() {
        return terminalEntity == null ? null : terminalEntity.getGrid();
    }


    private final CraftingTerminalBlockEntity terminalEntity;
    private final Player player;
    private final TransientCraftingContainer craftMatrix;
    private final ResultContainer resultContainer = new ResultContainer();
    private final ContainerLevelAccess access;

    // Клієнтський стан сховища
    private int clientTotalCapacity = 0;
    private int clientTotalUsed = 0;
    private List<ItemStack> clientVirtualPool = new ArrayList<>();

    private int lastCapacity = -1;
    private int lastUsed = -1;

    // Клієнтський конструктор
    public CraftingTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, (CraftingTerminalBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // Серверний конструктор
    public CraftingTerminalMenu(int containerId, Inventory playerInventory, CraftingTerminalBlockEntity terminalEntity) {
        super(SimpleByteStorage.CRAFTING_TERMINAL_MENU.get(), containerId);
        this.terminalEntity = terminalEntity;
        this.player = playerInventory.player;
        this.access = terminalEntity != null
                ? ContainerLevelAccess.create(terminalEntity.getNodeLevel(), terminalEntity.getNodePos())
                : ContainerLevelAccess.NULL;
        this.craftMatrix = new TransientCraftingContainer(this, 3, 3);

        // Слот 0: Результат крафту
        this.addSlot(new ResultSlot(player, craftMatrix, resultContainer, 0, 124, 40) {
            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (!stack.isEmpty()) {
                    CraftingTerminalMenu.this.craftResult(player, stack);
                }
            }
        });

        // Слоти 1-9: Крафтова сітка 3x3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(craftMatrix, col + row * 3, 30 + col * 18, 20 + row * 18));
            }
        }

        // Слоти 10-36: Інвентар гравця (3 рядки по 9 слотів)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 148 + row * 18));
            }
        }

        // Слоти 37-45: Хотбар гравця
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 202));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == craftMatrix) {
            Level level = player.level();
            if (!level.isClientSide) {
                updateCraftingResult(level);
            }
        }
        super.slotsChanged(container);
    }

    private void updateCraftingResult(Level level) {
        // Спроба знайти рецепт для поточних предметів крафтової матриці
        Optional<CraftingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftMatrix, level);

        if (recipe.isPresent()) {
            ItemStack result = recipe.get().assemble(craftMatrix, level.registryAccess());
            resultContainer.setItem(0, result);
        } else {
            resultContainer.setItem(0, ItemStack.EMPTY);
        }
    }

    /**
     * Виконує крафт: споживає інгредієнти з сховища та видає результат гравцю.
     */
    private void craftResult(Player player, ItemStack result) {
        if (player.level().isClientSide) {
            return;
        }
        if (terminalEntity == null || terminalEntity.getGrid() == null) {
            return;
        }

        INetworkStorage storage = terminalEntity.getGrid().getStorage();

        // 1. Споживаємо інгредієнти — витираємо їх з віртуального пулу (1 предмет = 1 байт)
        boolean ingredientsAvailable = true;
        for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
            ItemStack stack = craftMatrix.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack simulated = storage.extractItem(stack, stack.getCount(), true);
            if (simulated.isEmpty()) {
                // Не вистачило інгредієнтів — не викликаємо крафт
                ingredientsAvailable = false;
                break;
            }
            storage.extractItem(stack, stack.getCount(), false);
        }

        // 2. Видаємо результат гравцю (не використано — повертаємо в сховище)
        if (ingredientsAvailable && !result.isEmpty()) {
            ItemStack toGive = result.copy();
            if (!player.getInventory().add(toGive)) {
                storage.insertItem(toGive, false);
            }
        }

        // 3. Очищаємо крафтову матрицю (викликає автоматичне оновлення результату)
        for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
            craftMatrix.setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (terminalEntity == null) return false;
        Level level = terminalEntity.getNodeLevel();
        if (level == null) return false;
        return level.getBlockState(terminalEntity.getNodePos()).is(SimpleByteStorage.CRAFTING_TERMINAL_BLOCK.get())
                && player.distanceToSqr(
                terminalEntity.getNodePos().getX() + 0.5,
                terminalEntity.getNodePos().getY() + 0.5,
                terminalEntity.getNodePos().getZ() + 0.5) <= 64.0;
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

                if (currentCap != lastCapacity || currentUsed != lastUsed || serverPlayer.tickCount % 20 == 0) {
                    this.lastCapacity = currentCap;
                    this.lastUsed = currentUsed;
                    SimpleByteStoragePackets.sendToPlayer(
                            new SyncCraftingTerminalDataPacket(currentCap, currentUsed, storage.getAllItems()),
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
     * Швидке переміщення (Shift-клік).
     * Якщо клік по слоту крафту або результату - переміщення в інвентар гравця.
     * Якщо клік по інвентарю гравця - спроба вставити в сховище.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();

        if (index == 0) {
            // Результат крафту -> в інвентар гравця
            if (!this.moveItemStackTo(stackInSlot, 10, 46, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stackInSlot, copy);
        } else if (index >= 1 && index <= 9) {
            // Крафтова сітка -> в інвентар гравця
            if (!this.moveItemStackTo(stackInSlot, 10, 46, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= 10 && index <= 45) {
            // Інвентар гравця -> спроба в сховище
            if (!player.level().isClientSide() && terminalEntity != null
                    && terminalEntity.getGrid() != null
                    && terminalEntity.getGrid().getState().isActive()) {
                INetworkStorage storage = terminalEntity.getGrid().getStorage();
                ItemStack remainder = storage.insertItem(stackInSlot, false);
                if (remainder.getCount() == copy.getCount()) {
                    // Не вдалося вставити - пробуємо перемістити в крафтову сітку
                    if (!this.moveItemStackTo(stackInSlot, 1, 10, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    slot.set(remainder);
                    slot.setChanged();
                    if (player instanceof ServerPlayer) {
                        broadcastChanges();
                    }
                    return remainder.isEmpty() ? copy : ItemStack.EMPTY;
                }
            } else {
                // Мережа недоступна - пробуємо перемістити в крафтову сітку
                if (!this.moveItemStackTo(stackInSlot, 1, 10, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    /**
     * Обробка клієнтських дій (вилучення або вставка через клік по терміналу).
     * Делегуємо в ту ж логіку, що і звичайний термінал.
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
                int toExtract = Math.min(targetItem.getMaxStackSize(), 64);
                ItemStack extracted = storage.extractItem(targetItem, toExtract, false);
                if (!extracted.isEmpty()) {
                    if (!serverPlayer.getInventory().add(extracted)) {
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

    /**
     * Повернути предмети з крафтової сітки в інвентар гравця при закритті меню.
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            this.clearContainer(player, craftMatrix);
        });
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

    public CraftingTerminalBlockEntity getTerminalEntity() {
        return terminalEntity;
    }

    public TransientCraftingContainer getCraftMatrix() {
        return craftMatrix;
    }

    public ResultContainer getResultContainer() {
        return resultContainer;
    }
}
