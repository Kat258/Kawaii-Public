package dev.kizuna.mod.modules.impl.client;

import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;

import java.awt.*;

public class Colors extends Module {
    public static Colors INSTANCE;
    public Colors() {
        super("Colors", Category.Client);
        setChinese("颜色");
        INSTANCE = this;
    }

    public final ColorSetting clientColor = add(new ColorSetting("ThemesColor", new Color(123, 168, 255)).injectBoolean(true));

    private final BooleanSetting module = add(new BooleanSetting("Module",true).setParent2());
    public final BooleanSetting kawaiiAura = add(new BooleanSetting("KawaiiAura", false,module::isOpen2));
    public final BooleanSetting hud = add(new BooleanSetting("HUD", false,module::isOpen2));
    public final BooleanSetting arrayList = add(new BooleanSetting("ArrayList", false,module::isOpen2));
    public final BooleanSetting placeRender = add(new BooleanSetting("PlaceRender", false,module::isOpen2));


    @Override
    public void enable() {
        this.state = true;
    }

    @Override
    public void disable() {
        this.state = true;
    }

    @Override
    public boolean isOn() {
        return true;
    }
}
