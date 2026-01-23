package dev.kizuna.mod.modules.impl.hud;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class KeyDisplayHud
        extends Module {
    public static KeyDisplayHud INSTANCE;

    public KeyDisplayHud() {
        super("KeyDisplayHud", Module.Category.Hud);
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (INSTANCE.isOn()) {
            Kawaii.GUI.keyDisplayHud.draw(drawContext, tickDelta, null);
        }
    }
}
