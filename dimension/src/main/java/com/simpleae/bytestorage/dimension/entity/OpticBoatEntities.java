package com.simpleae.bytestorage.dimension.entity;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Оптическая лодка и оптическая лодка с сундуком.
 */
public final class OpticBoatEntities {

    private OpticBoatEntities() {}

    public static class OpticBoat extends Boat {
        public OpticBoat(EntityType<? extends Boat> type, Level level) {
            super(type, level);
        }

        public OpticBoat(Level level, double x, double y, double z) {
            this(SimpleAeDimension.OPTIC_BOAT_ENTITY.get(), level);
            this.setPos(x, y, z);
            this.xo = x;
            this.yo = y;
            this.zo = z;
        }

        @Override
        public Item getDropItem() {
            return SimpleAeDimension.OPTIC_BOAT.get();
        }
    }

    public static class OpticChestBoat extends ChestBoat {
        public OpticChestBoat(EntityType<? extends Boat> type, Level level) {
            super(type, level);
        }

        public OpticChestBoat(Level level, double x, double y, double z) {
            this(SimpleAeDimension.OPTIC_CHEST_BOAT_ENTITY.get(), level);
            this.setPos(x, y, z);
            this.xo = x;
            this.yo = y;
            this.zo = z;
        }

        @Override
        public Item getDropItem() {
            return SimpleAeDimension.OPTIC_CHEST_BOAT.get();
        }
    }
}
