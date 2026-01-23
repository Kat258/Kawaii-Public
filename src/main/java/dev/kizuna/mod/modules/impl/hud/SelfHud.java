package dev.kizuna.mod.modules.impl.hud;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class SelfHud
extends Module {
    public static SelfHud INSTANCE;

    public SelfHud() {
        super("SelfHud", Module.Category.Hud);
        this.setChinese("\u81ea\u8eab\u663e\u793a");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (INSTANCE.isOn()) {
            Kawaii.GUI.selfHud.draw(drawContext, tickDelta, null);
        }
    }
}
