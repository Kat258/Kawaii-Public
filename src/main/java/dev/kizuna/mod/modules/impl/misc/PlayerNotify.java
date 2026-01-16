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

public class PlayerNotify extends Module {
    public static PlayerNotify INSTANCE;
    private long lastTime;
    private HashMap<UUID, String> lastPlayers;
    private HashMap<UUID, String> currentPlayers;
    private boolean firstRun;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private static final String PREFIX = "§r[§dPlayer Notifier§r]§8";
    private static final String JOIN_PREFIX = PREFIX + "[§a+§8]§7 ";
    private static final String LEAVE_PREFIX = PREFIX + "[§c-§8]§7 ";

    public PlayerNotify() {
        super("PlayerNotify", Category.Misc);
        setChinese("玩家通知");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        lastTime = System.currentTimeMillis();
        lastPlayers = new HashMap<>();
        currentPlayers = new HashMap<>();

        firstRun = true;
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                var profile = entry.getProfile();
                lastPlayers.put(profile.getId(), profile.getName());
            }
            firstRun = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (event.getStage() != Event.Stage.Post) return;
        var network = mc.getNetworkHandler();
        if (network == null) return;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < 1000) return;

        currentPlayers.clear();
        for (PlayerListEntry entry : network.getPlayerList()) {
            var profile = entry.getProfile();
            currentPlayers.put(profile.getId(), profile.getName());
        }

        if (firstRun) {
            firstRun = false;
            HashMap<UUID, String> tmp = lastPlayers;
            lastPlayers = currentPlayers;
            currentPlayers = tmp;
            currentPlayers.clear();
            lastTime = currentTime;
            return;
        }

        for (Map.Entry<UUID, String> e : currentPlayers.entrySet()) {
            UUID id = e.getKey();
            if (!lastPlayers.containsKey(id)) {
                CommandManager.sendChatMessage(JOIN_PREFIX + e.getValue());
            }
        }
        for (Map.Entry<UUID, String> e : lastPlayers.entrySet()) {
            UUID id = e.getKey();
            if (!currentPlayers.containsKey(id)) {
                CommandManager.sendChatMessage(LEAVE_PREFIX + e.getValue());
            }
        }
        HashMap<UUID, String> tmp = lastPlayers;
        lastPlayers = currentPlayers;
        currentPlayers = tmp;
        currentPlayers.clear();
        lastTime = currentTime;
    }

    @Override
    public void onDisable() {
        lastPlayers.clear();
        currentPlayers.clear();
    }
}
