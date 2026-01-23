package dev.kizuna.mod.modules.impl.hud;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class ItemsCountHud
extends Module {
    public static ItemsCountHud INSTANCE;

    public ItemsCountHud() {
        super("ItemsCountHud", Module.Category.Hud);
        this.setChinese("\u7269\u54c1\u663e\u793a");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (INSTANCE.isOn()) {
            Kawaii.GUI.itemsCountHud.draw(drawContext, tickDelta, null);
        }
    }
}
