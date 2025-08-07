package dev.kizuna.mod.modules.impl.misc;

//为什么不用onPlayerJoin onPlayerLeave?
//因为在xin服没效果

import dev.kizuna.mod.modules.Module;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.Event;
import dev.kizuna.api.events.impl.TickEvent;
import dev.kizuna.core.impl.CommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerNotifier extends Module {
    public static PlayerNotifier INSTANCE;
    private long lastTime;
    private Map<UUID, String> lastPlayers;
    private boolean firstRun;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public PlayerNotifier() {
        super("PlayerNotifier", Category.Misc);
        setChinese("玩家通知");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        lastTime = System.currentTimeMillis();
        firstRun = true;
        lastPlayers = new HashMap<>();
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                UUID id = entry.getProfile().getId();
                lastPlayers.put(id, entry.getProfile().getName());
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (event.getStage() != Event.Stage.Post) return;
        if (mc.getNetworkHandler() == null) return;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < 1000) return;
        Map<UUID, String> currentPlayers = new HashMap<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            currentPlayers.put(entry.getProfile().getId(), entry.getProfile().getName());}
        if (firstRun) {firstRun = false;lastPlayers = currentPlayers;lastTime = currentTime;return;}
        for (Map.Entry<UUID, String> e : currentPlayers.entrySet()) {UUID id = e.getKey();
            if (!lastPlayers.containsKey(id)) {
                CommandManager.sendChatMessage("§r[§dPlayer Notifier§r]§8[§a+§8]§7 " + e.getValue());
            }
        }
        for (Map.Entry<UUID, String> e : lastPlayers.entrySet()) {UUID id = e.getKey();
            if (!currentPlayers.containsKey(id)) {
                CommandManager.sendChatMessage("§r[§dPlayer Notifier§r]§8[§c-§8]§7 " + e.getValue());
            }
        }
        lastPlayers = currentPlayers;
        lastTime = currentTime;
    }

    @Override
    public void onDisable() {
        if (lastPlayers != null) lastPlayers.clear();
    }
}