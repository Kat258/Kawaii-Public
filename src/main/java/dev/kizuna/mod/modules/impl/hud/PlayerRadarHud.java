package dev.kizuna.mod.modules.impl.hud;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

public class PlayerRadarHud
extends Module {
    public static PlayerRadarHud INSTANCE;
    public final BooleanSetting icons = this.add(new BooleanSetting("Icons", true));
    public final BooleanSetting radarDistance = this.add(new BooleanSetting("Distance", true));
    public final BooleanSetting radarPing = this.add(new BooleanSetting("Ping", true));
    public final BooleanSetting radarHealth = this.add(new BooleanSetting("Health", true));

    public PlayerRadarHud() {
        super("PlayerRadarHud", Module.Category.Hud);
        this.setChinese("\u73a9\u5bb6\u96f7\u8fbe");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (INSTANCE.isOn()) {
            Kawaii.GUI.playerRadarHud.draw(drawContext, tickDelta, null);
        }
    }
}
