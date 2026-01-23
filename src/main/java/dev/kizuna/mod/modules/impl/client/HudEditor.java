package dev.kizuna.mod.modules.impl.client;

import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.modules.Module;

public class HudEditor
extends Module {
    public static HudEditor INSTANCE;

    public HudEditor() {
        super("HudEditor", "HudEditor", Module.Category.Client);
        this.setChinese("\u754c\u9762\u7f16\u8f91\u5668");
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        if (!(HudEditor.mc.currentScreen instanceof ClickGuiScreen)) {
            this.disable();
        }
    }
}
