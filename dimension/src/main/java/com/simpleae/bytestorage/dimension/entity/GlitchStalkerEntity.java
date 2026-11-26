package com.simpleae.bytestorage.dimension.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Глитч-Сталкер (Glitch Stalker) — быстрый обитатель Битых Секторов и Сердца Реестра.
 * При получении урона может совершать короткий глитч-рывок с фиолетовыми частицами.
 */
public class GlitchStalkerEntity extends Monster {

    public GlitchStalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 36.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.isAlive() && this.random.nextFloat() < 0.40F) {
            double tx = this.getX() + (this.random.nextDouble() - 0.5) * 8.0;
            double ty = this.getY();
            double tz = this.getZ() + (this.random.nextDouble() - 0.5) * 8.0;
            if (this.randomTeleport(tx, ty, tz, true)) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        this.getSoundSource(), 0.9F, 1.4F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.PORTAL,
                            this.getX(), this.getY() + 1.0, this.getZ(),
                            18, 0.4, 0.7, 0.4, 0.1);
                }
            }
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.PORTAL,
                    this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5),
                    0.0, 0.05, 0.0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }
}
