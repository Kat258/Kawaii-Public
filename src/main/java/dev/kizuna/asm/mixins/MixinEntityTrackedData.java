package dev.kizuna.asm.mixins;

import dev.kizuna.mod.modules.impl.render.NameTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Entity.class)
public class MixinEntityTrackedData {

    @SuppressWarnings("unchecked")
    @Inject(method = "onTrackedDataSet", at = @At("HEAD"))
    private void kawaii$onTrackedDataSet(TrackedData<?> data, CallbackInfo ci) {
        try {
            Object selfObj = (Object) this;
            if (!(selfObj instanceof LivingEntity)) return;

            LivingEntity self = (LivingEntity) selfObj;

            if (!self.getWorld().isClient()) return;

            int newCount;
            try {
                newCount = self.getStuckArrowCount();
            } catch (Throwable t) {
                return;
            }

            UUID uuid = self.getUuid();
            int oldCount = NameTags.getLastStuckCount(uuid);

            NameTags.setLastStuckCount(uuid, newCount);

            if (self instanceof PlayerEntity && newCount > oldCount) {
                NameTags.markPlayerShot((PlayerEntity) self);
            }
        } catch (Throwable ignored) {
        }
    }
}
