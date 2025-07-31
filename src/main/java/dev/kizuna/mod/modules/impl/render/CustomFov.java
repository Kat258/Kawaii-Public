package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;

public class CustomFov extends Module {
    public static CustomFov INSTANCE;

    public final BooleanSetting fov = add(new BooleanSetting("Fov",false));
    public final SliderSetting fovValue = add(new SliderSetting("Fov", 90, 30, 170, 1, fov::getValue));
    public final BooleanSetting itemFov = add(new BooleanSetting("ItemFov",false));
    public final SliderSetting itemFovValue = add(new SliderSetting("ItemFovValue", 70, 30, 170, 1, itemFov::getValue));
    public final BooleanSetting aspectRatio = add(new BooleanSetting("AspectRatio",false));
    public final SliderSetting aspectRatioValue = add(new SliderSetting("Ratio", 1.78, 0.0, 5.0, 0.01, aspectRatio::getValue));
    public CustomFov() {
        super("Fov", Category.Render);
        INSTANCE = this;
    }
}
