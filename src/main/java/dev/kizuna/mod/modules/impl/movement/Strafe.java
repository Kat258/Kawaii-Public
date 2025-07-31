package dev.kizuna.mod.modules.impl.movement;

import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.MoveEvent;
import dev.kizuna.api.utils.entity.MovementUtil;
import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.entity.effect.StatusEffects;

import java.util.Objects;

import static dev.kizuna.api.utils.entity.MovementUtil.directionSpeed;

public class Strafe
        extends Module {
    public static Strafe INSTANCE;
    private final BooleanSetting airStop =
            add(new BooleanSetting("AirStop", true));
    private final BooleanSetting slowCheck =
            add(new BooleanSetting("SlowCheck", true));

    public Strafe() {
        super("Strafe", "Modifies sprinting", Category.Movement);
        setChinese("灵活移动");
        INSTANCE = this;
    }

    @EventHandler
    public void onStrafe(MoveEvent event) {
        if (mc.player.isSneaking() || HoleSnap.INSTANCE.isOn() || Speed.INSTANCE.isOn() || mc.player.isFallFlying() || Kawaii.PLAYER.insideBlock || mc.player.isInLava() || mc.player.isTouchingWater() || mc.player.getAbilities().flying)
            return;
        if (!MovementUtil.isMoving()) {
            if (airStop.getValue()) {
                MovementUtil.setMotionX(0);
                MovementUtil.setMotionZ(0);
            }
            return;
        }
        double[] dir = directionSpeed(getBaseMoveSpeed());
        event.setX(dir[0]);
        event.setZ(dir[1]);
    }

    public double getBaseMoveSpeed() {
        double n = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED) && (!this.slowCheck.getValue() || !mc.player.hasStatusEffect(StatusEffects.SLOWNESS))) {
            n *= 1.0 + 0.2 * (double) (Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.SPEED)).getAmplifier() + 1);
        }
        return n;
    }
}