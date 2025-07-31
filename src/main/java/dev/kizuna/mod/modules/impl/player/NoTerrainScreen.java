package dev.kizuna.mod.modules.impl.player;

import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.TickEvent;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.impl.exploit.PortalGod;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;

public class NoTerrainScreen extends Module {
    public NoTerrainScreen() {
        super("NoTerrainScreen", Category.Player);
        setChinese("没有加载界面");
    }

    @EventHandler
    public void onEvent(TickEvent event) {
        if (PortalGod.INSTANCE.isOn()) return;
        if (mc.currentScreen instanceof DownloadingTerrainScreen) {
            mc.currentScreen = null;
        }
    }
}
