package dev.kizuna.mod.modules.impl.hud;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class PotionHud
extends Module {
    public static PotionHud INSTANCE;

    public PotionHud() {
        super("PotionHud", Module.Category.Hud);
        this.setChinese("\u836f\u6c34\u663e\u793a");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (INSTANCE.isOn()) {
            Kawaii.GUI.potionHud.draw(drawContext, tickDelta, null);
        }
    }
}
