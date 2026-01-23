package dev.kizuna.mod.modules.impl.hud;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class ArmorHud
extends Module {
    public static ArmorHud INSTANCE;

    public ArmorHud() {
        super("ArmorHud", Module.Category.Hud);
        this.setChinese("\u88c5\u5907\u663e\u793a");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (INSTANCE.isOn()) {
            Kawaii.GUI.armorHud.draw(drawContext, tickDelta, null);
        }
    }
}
