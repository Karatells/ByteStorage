package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.item.EntangledCrystalItem;
import com.simpleae.bytestorage.menu.QuantumRelayMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** BlockEntity Квантового ретранслятора: об'єднує віддалені сегменти мережі за LinkId кристала. */
public class QuantumRelayBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    private static final Map<UUID, Set<QuantumRelayBlockEntity>> REGISTRY = new ConcurrentHashMap<>();

    private IGrid grid;
    private UUID registeredId = null;

    private final ItemStackHandler crystalSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof EntangledCrystalItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            updateRegistry();
            setChanged();
        }
    };

    public QuantumRelayBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.QUANTUM_RELAY_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStackHandler getCrystalInventory() {
        return crystalSlot;
    }

    public ItemStack getCrystal() {
        return crystalSlot.getStackInSlot(0);
    }

    public ItemStack swapCrystal(ItemStack next) {
        ItemStack prev = crystalSlot.getStackInSlot(0);
        crystalSlot.setStackInSlot(0, next);
        return prev;
    }

    public List<QuantumRelayBlockEntity> getLinkedPartners() {
        UUID id = registeredId;
        if (id == null) {
            return Collections.emptyList();
        }
        Set<QuantumRelayBlockEntity> set = REGISTRY.get(id);
        if (set == null || set.isEmpty()) {
            return Collections.emptyList();
        }
        List<QuantumRelayBlockEntity> partners = new ArrayList<>();
        for (QuantumRelayBlockEntity other : set) {
            if (other != this && !other.isRemoved() && other.getLevel() != null && !other.getLevel().isClientSide) {
                partners.add(other);
            }
        }
        return partners;
    }

    private void updateRegistry() {
        if (registeredId != null) {
            Set<QuantumRelayBlockEntity> old = REGISTRY.get(registeredId);
            if (old != null) {
                old.remove(this);
                if (old.isEmpty()) {
                    REGISTRY.remove(registeredId);
                }
            }
            registeredId = null;
        }
        if (level != null && !level.isClientSide && !isRemoved() && !getCrystal().isEmpty()) {
            UUID id = EntangledCrystalItem.getLinkId(getCrystal());
            if (id != null) {
                registeredId = id;
                REGISTRY.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet()).add(this);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateRegistry();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        crystalSlot.setStackInSlot(0, ItemStack.EMPTY);
        updateRegistry();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (registeredId != null) {
            Set<QuantumRelayBlockEntity> set = REGISTRY.get(registeredId);
            if (set != null) {
                set.remove(this);
            }
            registeredId = null;
        }
    }

    // --- IGridNode ---

    @Override
    @Nullable
    public IGrid getGrid() {
        return grid;
    }

    @Override
    public void setGrid(@Nullable IGrid grid) {
        this.grid = grid;
    }

    @Override
    public BlockPos getNodePos() {
        return this.worldPosition;
    }

    @Override
    public Level getNodeLevel() {
        return this.level;
    }

    @Override
    public int getIdleEnergyDrain() {
        return registeredId != null ? 50 : 5;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.quantum_relay");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new QuantumRelayMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("CrystalInv", crystalSlot.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("CrystalInv")) {
            crystalSlot.deserializeNBT(tag.getCompound("CrystalInv"));
        } else if (tag.contains("Crystal")) {
            crystalSlot.setStackInSlot(0, ItemStack.of(tag.getCompound("Crystal")));
        } else {
            crystalSlot.setStackInSlot(0, ItemStack.EMPTY);
        }
        updateRegistry();
    }
}
