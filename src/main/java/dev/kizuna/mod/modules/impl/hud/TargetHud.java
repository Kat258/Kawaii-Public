package dev.kizuna.mod.modules.impl.hud;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.gui.elements.TargetHUD;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

public class TargetHud
extends Module {
    public static TargetHud INSTANCE;
    public final BooleanSetting follow = this.add(new BooleanSetting("Follow", false));

    public TargetHud() {
        super("TargetHud", Module.Category.Hud);
        this.setChinese("\u76ee\u6807\u73a9\u5bb6\u663e\u793a");
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        TargetHUD.INSTANCE.healthAnimation.update();
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (INSTANCE.isOn()) {
            Kawaii.GUI.targetHud.draw(drawContext, tickDelta, null);
        }
    }
}
