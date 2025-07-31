package dev.kizuna.mod.modules.impl.movement;

import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;

public class AutoWalk extends Module {
    public AutoWalk() {
        super("AutoWalk", Category.Movement);
        INSTANCE = this;
    }
    public enum Mode {
        Forward,
        Path
    }

    EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Forward));
    public static AutoWalk INSTANCE;
    boolean start = false;

    @Override
    public void onEnable() {
        start = false;
    }

    @Override
    public void onLogout() {
        disable();
    }

    @Override
    public void onUpdate() {
        if (mode.is(Mode.Forward)) {
            mc.options.forwardKey.setPressed(true);
        }
    }

    public boolean forward() {
        return isOn() && mode.is(Mode.Forward);
    }
}
