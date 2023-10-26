package dev.louis.chainstylewither.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;

@Mixin(DefaultAttributeContainer.Builder.class)
public interface AttributeSupplierBuilderAccessor {
    @Accessor
    Map<EntityAttribute, EntityAttributeInstance> getInstances();
}
