package dev.kizuna.mod.modules.impl.player;

import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;

public class AutoPotSneak extends Module {

    private final SliderSetting holdMs = add(new SliderSetting("HoldMs", 200, 0, 2000).setSuffix("ms"));

    private boolean sneaking = false;
    private long releaseTime = 0L;

    public AutoPotSneak() {
        super("AutoPotSneak", Category.Player);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        if (AutoPot.INSTANCE != null && AutoPot.INSTANCE.isOn()) {
            if (!sneaking) {
                mc.player.setSneaking(true);
                sneaking = true;
            }
            releaseTime = System.currentTimeMillis() + (long) holdMs.getValue();
        }

        if (sneaking && releaseTime > 0 && System.currentTimeMillis() >= releaseTime) {
            mc.player.setSneaking(false);
            sneaking = false;
            releaseTime = 0L;
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null && sneaking) {
            mc.player.setSneaking(false);
        }
        sneaking = false;
        releaseTime = 0L;
    }
}
