package dev.kizuna.mod.modules.impl.misc;

import dev.kizuna.api.utils.entity.MovementUtil;
import dev.kizuna.api.utils.math.Timer;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.network.PlayerListEntry;

import java.util.*;

public class MyPing extends Module {
    private final SliderSetting randoms = add(new SliderSetting("Random", 3, 0, 20,1));
    private final SliderSetting delay = add(new SliderSetting("Delay", 5, 0, 60,0.1).setSuffix("s"));

    public MyPing() {
        super("MyPing", Category.Misc);
    }

    @Override
    public void onLogout() {
        disable();
    }

    Random random = new Random();
    Timer timer = new Timer();
    private static final String CHARACTERS = "0123456789";

    @Override
    public void onUpdate() {
        if (!timer.passedS(delay.getValue())) return;
        timer.reset();
        String randomString = generateRandomString(randoms.getValueInt());
        if (!randomString.isEmpty()) {
        }
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        String ping;
        if (playerListEntry == null) {
            ping = "Unknown";
        } else {
            ping = String.valueOf(playerListEntry.getLatency());
        }
        mc.getNetworkHandler().sendChatMessage("My Ping: " + ping + "ms" + "("+ randomString + ")");
    }


    private String generateRandomString(int LENGTH) {
        StringBuilder sb = new StringBuilder(LENGTH);

        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }

        return sb.toString();
    }
}