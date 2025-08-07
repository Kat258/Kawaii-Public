package dev.kizuna.mod.modules.impl.player;

import dev.kizuna.api.events.eventbus.EventPriority;
import dev.kizuna.api.events.impl.PacketEvent;
import dev.kizuna.api.events.impl.RotateEvent;
import dev.kizuna.api.utils.math.*;
import dev.kizuna.mod.modules.impl.combat.PacketThrow;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.utils.entity.EntityUtil;
import dev.kizuna.mod.modules.Module;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.BowItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;

public class SpinBot extends Module {
    public SpinBot() {
        super("SpinBot", "fun", Category.Player);
    }

    private final EnumSetting<Mode> pitchMode = add(new EnumSetting<>("PitchMode", Mode.None));
    private final EnumSetting<Mode> yawMode = add(new EnumSetting<>("YawMode", Mode.None));

    public enum Mode {None, RandomAngle, Spin, Static}

    public final SliderSetting yawDelta = this.add(new SliderSetting("YawDelta", 60, -360, 360));
    public final SliderSetting pitchDelta = this.add(new SliderSetting("PitchDelta", 10, -90, 90));
    public final BooleanSetting allowInteract = add(new BooleanSetting("AllowInteract", true));

    private float rotationYaw, rotationPitch;

    @EventHandler
    public void onPacket(PacketEvent.Send event) {
            if (event.getPacket() instanceof PlayerActionC2SPacket packet && packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM && mc.player.getActiveItem().getItem() instanceof BowItem) {
            EntityUtil.sendYawAndPitch(mc.player.getYaw(), mc.player.getPitch());
        }
            if (PacketThrow.INSTANCE.throwing && PacketThrow.INSTANCE.isThrow() && PacketThrow.INSTANCE.isOn());
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUpdateWalkingPlayerPre(RotateEvent event) {
            if (pitchMode.getValue() == Mode.RandomAngle)
                rotationPitch = MathUtil.random(90, -90);

        if (yawMode.getValue() == Mode.RandomAngle)
            rotationYaw = MathUtil.random(0, 360);

        if (yawMode.getValue() == Mode.Spin)
            rotationYaw += yawDelta.getValue();
        if (rotationYaw > 360) rotationYaw = 0;
        if (rotationYaw < 0) rotationYaw = 360;

        if (pitchMode.getValue() == Mode.Spin)
            rotationPitch += pitchDelta.getValue();
        if (rotationPitch > 90) rotationPitch = -90;
        if (rotationPitch < -90) rotationPitch = 90;

        if (pitchMode.getValue() == Mode.Static) {
            rotationPitch = mc.player.getPitch() + pitchDelta.getValueFloat();
            rotationPitch = MathUtil.clamp(rotationPitch, -90, 90);
        }
        if (yawMode.getValue() == Mode.Static)
            rotationYaw = mc.player.getYaw() % 360 + yawDelta.getValueFloat();
        if (allowInteract.getValue() && ((mc.options.useKey.isPressed() && !EntityUtil.isUsing()) || mc.options.attackKey.isPressed()))
            return;
        if (yawMode.getValue() != Mode.None) {
            event.setYaw(rotationYaw);
        }
        if (pitchMode.getValue() != Mode.None)
            event.setPitch(rotationPitch);
    }
}