package dev.louis.chainstylewither.mixin;

import dev.louis.chainstylewither.ChainStyleWither;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherSkullEntity.class)
public abstract class WitherSkullMixin extends ExplosiveProjectileEntity {
    protected WitherSkullMixin(EntityType<? extends ExplosiveProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    public abstract boolean isCharged();

    @ModifyConstant(method = "onCollision", constant = @Constant(floatValue = 1F))
    private float biggerBoom(float constant) {
        return constant* 1.8F;
    }


    @Inject(method = "getDrag", at = @At("RETURN"), cancellable = true)
    private void getInertia(CallbackInfoReturnable<Float> cir) {
        if (!ChainStyleWither.getConfig().isEnableMoreInertialBlueWitherSkull())
            return;

        if (isCharged())
            cir.setReturnValue(0.90F);
    }

    @Inject(method = "canHit", at = @At("RETURN"), cancellable = true)
    private void isPickable(CallbackInfoReturnable<Boolean> cir) {
        if (!ChainStyleWither.getConfig().isEnableBounceBlueWitherSkull()) return;

        if (isCharged() && !cir.getReturnValue()) cir.setReturnValue(true);
    }

    @Inject(method = "damage", at = @At("RETURN"), cancellable = true)
    private void hurt(DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        if (!ChainStyleWither.getConfig().isEnableBounceBlueWitherSkull()) return;

        if (isCharged() && !cir.getReturnValue()) {
            WitherSkullEntity ths = (WitherSkullEntity) (Object) this;
            if (ths.isInvulnerableTo(damageSource))
                return;

            ths.velocityModified = true;
            var entity = damageSource.getAttacker();

            if (entity != null && !(entity instanceof WitherEntity) && !(damageSource.getSource() instanceof WitherSkullEntity)) {
                if (!ths.getWorld().isClient) {
                    Vec3d vec3 = entity.getRotationVector();
                    ths.setVelocity(vec3);
                    ths.powerX = vec3.x * 0.1;
                    ths.powerY = vec3.y * 0.1;
                    ths.powerZ = vec3.z * 0.1;
                    ths.setOwner(entity);

                    WorldChunk lch = (WorldChunk) ths.getWorld().getChunk(ths.getBlockPos());
                }
                cir.setReturnValue(true);
            }
        }
    }

    @ModifyConstant(method = "onEntityHit", constant = @Constant(intValue = 1))
    private int onEntityHit(int constant) {
        return 3;
    }

    @Override
    public void applyDamageEffects(LivingEntity attacker, Entity target) {
        if(target.isTouchingWater()) {
            Vec3d vec3d = target.getVelocity();
            Vec3d velocity = new Vec3d(vec3d.x, -2, vec3d.z);
            target.setVelocity(velocity);
            target.velocityModified = true;
        }

        super.applyDamageEffects(attacker, target);
    }
}
