package dev.louis.chainstylewither.entity.goal;

import dev.louis.chainstylewither.ChainStyleWither;
import dev.louis.chainstylewither.entity.ChainStyleWitherBoss;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

public class WitherChargeAttackGoal extends Goal {
    public static final int chargeTime = 100;
    public static final int chargeHoldTime = 50;
    private final WitherEntity mob;
    @Nullable
    private LivingEntity target;
    private int chargeTick;
    private Vec3d lookAt;
    private float bodyRot;

    public WitherChargeAttackGoal(WitherEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return canUse(true);
    }

    public boolean canUse(boolean randed) {
        if (!ChainStyleWither.getConfig().isEnableChargeAttack())
            return false;

        if (randed && mob.getRandom().nextInt(4) != 0) return false;

        //if (mob.getInvulnerableTimer() > 0 || !mob.shouldRenderOverlay() || ((BEWitherBoss) mob).getChargeCoolDown() > 0) return false;
        if (mob.getInvulnerableTimer() > 0 || ((ChainStyleWitherBoss) mob).getChargeCoolDown() > 0) return false;
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity != null && livingEntity.isAlive()) {
            this.target = livingEntity;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        if (!mob.getWorld().isClient()) {
            WorldChunk worldChunk = (WorldChunk) mob.getWorld().getChunk(mob.getBlockPos());
        }

        this.chargeTick = this.getTickCount(chargeTime);
        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.chargeTick = 0;
        this.target = null;
        ((ChainStyleWitherBoss) mob).setChargeCoolDown(200);
        lookAt = null;
        bodyRot = 0f;
    }

    @Override
    public boolean shouldContinue() {
        return this.canUse(false) && this.chargeTick > 0 && target != null;
    }

    @Override
    public void tick() {
        this.chargeTick = Math.max(0, this.chargeTick - 1);
        if (this.chargeTick > this.getTickCount(chargeTime - chargeHoldTime)) {
            if (target != null) {
                this.mob.getLookControl().lookAt(target);
                double yDifference = target.getY() - this.mob.getY();
                lookAt = mob.getRotationVector().add(0, yDifference/20f, 0);

                /**if(Math.abs(yDifference) > 12) {
                    lookAt = mob.getRotationVector().multiply(1, 1, 0.2);
                }**/
                bodyRot = mob.bodyYaw;
                mob.setBodyYaw(bodyRot);
                mob.setVelocity(Vec3d.ZERO);
            }
        } else {
            ((ChainStyleWitherBoss) mob).setBlockBreakingCooldown(1);
            List<Entity> entities = this.mob.getWorld().getOtherEntities(this.mob, this.mob.getBoundingBox());
            for (Entity entity : entities) {
                if(entity instanceof ServerPlayerEntity player) {
                    boolean bl = target.damage(this.mob.getDamageSources().mobAttack(this.mob), 20f);
                    if(player.isBlocking()) {
                        player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS);
                    }
                    player.setVelocity(this.mob.getVelocity().multiply(0.8));
                    //player.damage(player.getWorld().getDamageSources().mobAttack(this.mob), 6.5f);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 10*20, 5), this.mob);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6*20, 3), this.mob);
                }
            }

            if (lookAt != null) {
                this.mob.getLookControl().lookAt(lookAt);
                mob.setVelocity(lookAt.multiply(this.mob.shouldRenderOverlay() ? 1.5 : 1.3));
                mob.setBodyYaw(bodyRot);
            }
        }
    }
}
