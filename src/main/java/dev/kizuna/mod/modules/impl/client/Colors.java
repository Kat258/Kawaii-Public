package dev.kizuna.mod.modules.impl.client;

import dev.kizuna.api.utils.render.ColorUtil;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;

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
    private final EnumSetting<ColorMode> colorMode = add(new EnumSetting<>("ColorMode", ColorMode.Pulse));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(0xFFACAFFD,true), () -> colorMode.getValue() == ColorMode.Custom));

    private final SliderSetting rainbowSpeed = add(new SliderSetting("RainbowSpeed", 4, 1, 10, 0.1, () -> colorMode.getValue() == ColorMode.Rainbow ));
    private final SliderSetting saturation = add(new SliderSetting("Saturation", 130.0f, 1.0f, 255.0f, () -> colorMode.getValue() == ColorMode.Rainbow ));
    private final SliderSetting rainbowDelay = add(new SliderSetting("Delay", 350, 0, 1000, () -> colorMode.getValue() == ColorMode.Rainbow));

    private final ColorSetting startColor = add(new ColorSetting("StartColor", new Color(0x6EACAFFD,true), () -> colorMode.getValue() == ColorMode.Pulse));
    private final ColorSetting endColor = add(new ColorSetting("EndColor", new Color(0xFFACAFFD,true), () -> colorMode.getValue() == ColorMode.Pulse));
    private final SliderSetting pulseSpeed = add(new SliderSetting("PulseSpeed", 1, 0, 5, 0.1, () -> colorMode.getValue() == ColorMode.Pulse ));
    private final SliderSetting pulseCounter = add(new SliderSetting("Counter", 10, 1, 50, () -> colorMode.getValue() == ColorMode.Pulse));


    private enum ColorMode {
        Custom,
        Pulse,
        Rainbow
    }

    public int getColor(int counter) {
        if (colorMode.getValue() != ColorMode.Custom) {
            return rainbow(counter).getRGB();
        }
        return color.getValue().getRGB();
    }
    private Color rainbow(int delay) {
        if (colorMode.getValue() == ColorMode.Pulse) {
            return ColorUtil.pulseColor(startColor.getValue(), endColor.getValue(), delay, pulseCounter.getValueInt(), pulseSpeed.getValue());
        } else if (colorMode.getValue() == ColorMode.Rainbow) {
            double rainbowState = Math.ceil((System.currentTimeMillis() * rainbowSpeed.getValue() + delay * rainbowDelay.getValue()) / 20.0);
            return Color.getHSBColor((float) (rainbowState % 360.0 / 360), saturation.getValueFloat() / 255.0f, 1.0f);
        }
        return color.getValue();
    }


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
