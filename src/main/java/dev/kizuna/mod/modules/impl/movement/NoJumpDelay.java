package dev.kizuna.mod.modules.impl.movement;

import dev.kizuna.mod.modules.Module;

public final class NoJumpDelay
        extends Module {
    public static NoJumpDelay INSTANCE;
    public NoJumpDelay() {
        super("NoJumpDelay", Category.Movement);
        setChinese("无跳跃冷却");
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        mc.player.jumpingCooldown = 0;
    }
}
