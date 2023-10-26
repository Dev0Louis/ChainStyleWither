package dev.louis.chainstylewither.mixin;

import dev.louis.chainstylewither.entity.ChainStyleWitherBoss;
import dev.louis.chainstylewither.entity.goal.WitherChargeAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import dev.louis.chainstylewither.ChainStyleWither;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Debug(export = true)
@Mixin(WitherEntity.class)
public abstract class WitherBossMixin extends HostileEntity implements ChainStyleWitherBoss {
    @Unique
    private static final int MAX_WITHER_DEATH_TIME = 20 * 10;
    @Unique
    private int chargeTickCoolDown;
    @Unique
    private int clientChargeTick;
    @Unique
    private int clientChargeTickOld;
    @Unique
    private DamageSource lastDeathDamageSource;
    @Unique
    private boolean dropLootSkip;

    @Shadow
    public abstract int getInvulnerableTimer();

    @Shadow
    @Final
    private float[] sideHeadYaws;

    @Shadow
    public abstract boolean shouldRenderOverlay();

    @Shadow
    private int blockBreakingCooldown;

    @Shadow
    protected abstract void shootSkullAt(int headIndex, double targetX, double targetY, double targetZ, boolean charged);
    @Shadow
    @Final
    private ServerBossBar bossBar;
    @Unique
    private static final TrackedData<Boolean> DATA_ID_FORCED_POWER = DataTracker.registerData(WitherEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @ModifyConstant(
            method = "createWitherAttributes",
            constant = @Constant(doubleValue = 300.0)
    )
    private static double higherMaxHealth(double constant) {
        return constant * (ChainStyleWither.getConfig().isEnableALotHealth() ? 6.0 : 1.0);
    }

    protected WitherBossMixin(EntityType<? extends HostileEntity> entityType, World level) {
        super(entityType, level);
    }

    @Inject(method = "shootSkullAt(ILnet/minecraft/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private void shootSkullAt(int i, LivingEntity livingEntity, CallbackInfo ci) {
        if (!ChainStyleWither.getConfig().isEnableShootMoreBlueWitherSkull()) return;

        if (i == 0) {
            this.shootSkullAt(i, livingEntity.getX(), livingEntity.getY() + (double) livingEntity.getStandingEyeHeight() * 0.5, livingEntity.getZ(), true);
            ci.cancel();
        }
    }

    @Inject(
            method = "mobTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFZLnet/minecraft/world/World$ExplosionSourceType;)Lnet/minecraft/world/explosion/Explosion;"
            )
    )
    private void mobTick(CallbackInfo ci) {
        this.setHealth(this.getMaxHealth());
    }

    @Inject(method = "shouldRenderOverlay", at = @At("RETURN"), cancellable = true)
    private void maintainOverlay(CallbackInfoReturnable<Boolean> cir) {
        if ((ChainStyleWither.getConfig().isEnableMaintainWeakenedState() && isForcedPowered()) || (ChainStyleWither.getConfig().isEnableExplodeByDie() && deathTime > 0))
            cir.setReturnValue(true);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void registerGoals(CallbackInfo ci) {
        this.goalSelector.add(1, new WitherChargeAttackGoal((WitherEntity) (Object) this));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return damageSource.isIn(DamageTypeTags.IS_EXPLOSION) || super.isInvulnerableTo(damageSource);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void defineSynchedData(CallbackInfo ci) {
        this.dataTracker.startTracking(DATA_ID_FORCED_POWER, false);
    }


    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void addAdditionalSaveData(NbtCompound compoundTag, CallbackInfo ci) {
        compoundTag.putBoolean("FPower", isForcedPowered());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readAdditionalSaveData(NbtCompound compoundTag, CallbackInfo ci) {
        setForcedPowered(compoundTag.getBoolean("FPower"));
    }

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void aiStepPre(CallbackInfo ci) {
        if (ChainStyleWither.getConfig().isEnableExplodeByDie() && isDead()) ci.cancel();
    }

    @Inject(method = "getDeathSound", at = @At("RETURN"), cancellable = true)
    private void getDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
        if (!ChainStyleWither.getConfig().isEnableExplodeByDie())
            return;

        if (deathTime <= 0)
            cir.setReturnValue(null);
    }

    @ModifyConstant(method = "damage", constant = @Constant(intValue = 20))
    private int shortBreakCooldown(int constant) {
        return 5;
    }

    @ModifyConstant(
            method = "mobTick",
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/util/math/MathHelper;floor(D)I",
                             ordinal = 2
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;)Z"
                    )
            ),
            constant = @Constant(intValue = 0)
    )
    private int breakBlockBelowYouWither(int constant) {
        return -1;
    }

    @Override
    public void playAmbientSound() {
        if (!ChainStyleWither.getConfig().isEnableExplodeByDie()) {
            super.playAmbientSound();
            return;
        }

        if (isAlive())
            super.playAmbientSound();
    }

    @Override
    public void tick() {
        super.tick();

        if (ChainStyleWither.getConfig().isEnableExplodeByHalfHealth() && getInvulnerableTimer() <= 0 && shouldRenderOverlay() && !isForcedPowered())
            setVelocity(getVelocity().add(0, -0.7f, 0));
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if (!ChainStyleWither.getConfig().isEnableExplodeByDie()) {
            super.onDeath(damageSource);
            return;
        }

        dropLootSkip = true;
        super.onDeath(damageSource);
        dropLootSkip = false;

        if (this.isDead())
            lastDeathDamageSource = damageSource;
    }

    @Override
    protected void drop(DamageSource damageSource) {
        if (!dropLootSkip)
            super.drop(damageSource);
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void aiStepPost(CallbackInfo ci) {
        if (ChainStyleWither.getConfig().isEnableSpinAndWhiteSummon()) {
            int it = getInvulnerableTimer();
            if (it > 0) {
                float par = 1f - ((float) it / 220f);
                float angle = (60f * par) + 5f;
                setBodyYaw(bodyYaw + angle);
                setHeadYaw(getHeadYaw() + angle);
                for (int i = 0; i < sideHeadYaws.length; i++) {
                    sideHeadYaws[i] = sideHeadYaws[i] + angle;
                }
            }
        }

        setChargeCoolDown(Math.max(0, getChargeCoolDown() - 1));
        if (getWorld().isClient()) {
            clientChargeTickOld = clientChargeTick;
            clientChargeTick = Math.max(0, clientChargeTick - 1);
        }
    }

    @Inject(method = "mobTick", at = @At("TAIL"))
    private void customServerAiStep(CallbackInfo ci) {
        if (getInvulnerableTimer() <= 0 && shouldRenderOverlay() && !isForcedPowered()) {
            if (ChainStyleWither.getConfig().isEnableExplodeByHalfHealth()) {
                var clip = getWorld().raycast(new RaycastContext(getPos(), getPos().add(0, -30, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));

                boolean flg = true;
                boolean exFlg = false;

                if (clip.getType() != HitResult.Type.MISS) {
                    flg = Math.sqrt(clip.squaredDistanceTo(this)) <= 1 || isOnGround() || isTouchingWater() || isInsideWall();
                    exFlg = true;
                }

                if (flg) {
                    setForcedPowered(true);
                    this.getWorld().createExplosion(this, this.getX(), this.getEyeY(), this.getZ(), 5.0F, false, World.ExplosionSourceType.MOB);

                    if (!this.isSilent())
                        this.getWorld().syncGlobalEvent(WorldEvents.WITHER_BREAKS_BLOCK, this.getBlockPos(), 0);

                    if (exFlg && (this.getWorld().getDifficulty() == Difficulty.NORMAL || this.getWorld().getDifficulty() == Difficulty.HARD)) {
                        int wc = 7;
                        if (random.nextInt(8) == 0) wc = 14;

                        for (int i = 0; i < wc; i++) {
                            WitherSkeletonEntity witherSkeleton = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, getWorld());
                            witherSkeleton.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), 0.0F);
                            witherSkeleton.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100000, 3, false, false));
                            witherSkeleton.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100000, 3, false, false));
                            witherSkeleton.initialize((ServerWorldAccess) getWorld(), getWorld().getLocalDifficulty(getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
                            getWorld().spawnEntity(witherSkeleton);
                        }
                    }
                }

            } else {
                setForcedPowered(true);
            }
        }
    }

    private void setForcedPowered(boolean powered) {
        this.dataTracker.set(DATA_ID_FORCED_POWER, powered);
    }

    private boolean isForcedPowered() {
        return this.dataTracker.get(DATA_ID_FORCED_POWER);
    }

    @Override
    protected void updatePostDeath() {
        if (!ChainStyleWither.getConfig().isEnableExplodeByDie()) {
            super.updatePostDeath();
            return;
        }

        deathTime++;
        bossBar.setPercent(this.getHealth() / this.getMaxHealth());

        if (this.deathTime % 4 == 0)
            setForcedPowered(random.nextInt((int) Math.max(5 - ((float) this.deathTime / (20f * 10f) * 5f), 1)) == 0);

        if (!this.getWorld().isClient()) {
            if (this.deathTime == MAX_WITHER_DEATH_TIME - 1) {
                this.getWorld().createExplosion(this, this.getX(), this.getEyeY(), this.getZ(), 8f, false, World.ExplosionSourceType.MOB);
                if (!this.isSilent())
                    this.getWorld().syncGlobalEvent(WorldEvents.WITHER_BREAKS_BLOCK, this.getBlockPos(), 0);

                SoundEvent soundevent = this.getDeathSound();
                if (soundevent != null)
                    this.playSound(soundevent, this.getSoundVolume() * 1.5f, this.getSoundPitch());
            } else if (this.deathTime == MAX_WITHER_DEATH_TIME) {
                this.playerHitTimer = Math.max(playerHitTimer, 1);
                var dmg = lastDeathDamageSource == null ? getWorld().getDamageSources().outOfWorld() : lastDeathDamageSource;
                drop(dmg);

                this.getWorld().sendEntityStatus(this, (byte) 60);
                this.remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public int getWitherDeathTime() {
        return deathTime;
    }

    @Override
    public float getWitherDeathTime(float delta) {
        return (deathTime + delta - 1.0F) / (float) (30 - 2);
    }

    @Override
    public int getBlockBreakingCooldown() {
        return this.blockBreakingCooldown;
    }

    @Override
    public void setBlockBreakingCooldown(int tick) {
        this.blockBreakingCooldown = tick;
    }

    @Override
    public int getChargeCoolDown() {
        return chargeTickCoolDown;
    }

    @Override
    public void setChargeCoolDown(int tick) {
        this.chargeTickCoolDown = tick;
    }

    @Override
    public void setClientCharge(int charge) {
        this.clientChargeTick = charge;
    }

    @Override
    public int getClientCharge() {
        return this.clientChargeTick;
    }

    @Override
    public float getClientCharge(float delta) {
        return MathHelper.lerp(delta, clientChargeTickOld, clientChargeTick);
    }
}