package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.item.DataBlobItem;
import com.simpleae.bytestorage.menu.ArchiverMenu;
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
import org.jetbrains.annotations.Nullable;

/** BlockEntity Архіватора: автоматично стискає пачки по 64 предмети, коли запас перевищує 4096 шт. */
public class ArchiverBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    private static final int THRESHOLD = 4096;
    private static final int RATIO = DataBlobItem.DEFAULT_RATIO;
    private static final int FE_PER_COMPRESS = 160;

    private IGrid grid;
    private boolean enabled = true;
    private int tickCounter = 0;

    public ArchiverBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.ARCHIVER_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggleEnabled() {
        enabled = !enabled;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArchiverBlockEntity archiver) {
        if (!archiver.enabled || ++archiver.tickCounter % 20 != 0) {
            return;
        }
        IGrid grid = archiver.grid;
        if (grid == null || !grid.getState().isActive() || grid.isEcoMode() || grid.getStorage() == null) {
            return;
        }
        NetworkControllerBlockEntity controller = grid.getController();
        if (controller == null || controller.getEnergyStored() < FE_PER_COMPRESS) {
            return;
        }
        INetworkStorage storage = grid.getStorage();
        for (ItemStack stack : storage.getAllItems()) {
            if (stack.isEmpty() || stack.hasTag() || stack.getItem() instanceof DataBlobItem) {
                continue;
            }
            if (stack.getMaxStackSize() >= 16 && stack.getCount() >= THRESHOLD + RATIO) {
                ItemStack extracted = storage.extractItem(stack, RATIO, false);
                if (extracted.getCount() == RATIO) {
                    ItemStack blob = DataBlobItem.createFor(extracted, RATIO);
                    ItemStack rem = storage.insertItem(blob, false);
                    if (!rem.isEmpty()) {
                        storage.insertItem(extracted, false);
                    } else {
                        controller.drawEnergy(FE_PER_COMPRESS);
                    }
                    break;
                } else if (!extracted.isEmpty()) {
                    storage.insertItem(extracted, false);
                }
            }
        }
    }

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
        return enabled ? 10 : 1;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.archiver");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ArchiverMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Enabled", enabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
    }
}
