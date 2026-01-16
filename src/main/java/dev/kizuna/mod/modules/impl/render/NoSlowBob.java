package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;

public class NoSlowBob extends Module {
    public static NoSlowBob INSTANCE;

    public NoSlowBob() {
        super("NoSlowBob", Category.Render);
        setChinese("移除缓慢抖动");
        INSTANCE = this;
    }

    public float getBobMultiplier() {
        if (mc.player == null) return 1f;

        // If player is in air, bobbing should be minimal/default logic, 
        // forcibly amplifying it causes jitter on landing as speed changes rapidly.
        if (!mc.player.isOnGround()) return 1f;

        // "Directly cancel" the reduction means using the base speed (without modifiers like Slowness)
        // instead of the current speed.
        double currentSpeed = mc.player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        double baseSpeed = mc.player.getAttributeBaseValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);

        // If sprinting, the base speed should include the sprint modifier for accurate bobbing
        if (mc.player.isSprinting()) {
             // Sprinting adds roughly 30% speed
             baseSpeed *= 1.3; 
        }

        // Calculate the ratio to restore the speed to "normal"
        if (currentSpeed <= 0) return 1f;
        
        return (float) (baseSpeed / currentSpeed);
    }
}
