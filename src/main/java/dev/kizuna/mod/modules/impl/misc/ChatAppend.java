package dev.kizuna.mod.modules.impl.misc;

import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.SendMessageEvent;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;

import java.util.concurrent.ThreadLocalRandom;

public class ChatAppend extends Module {
    public static ChatAppend INSTANCE;
    private final BooleanSetting antiSpam = add(new BooleanSetting("AntiSpam", false));
    private final SliderSetting Length = add(new SliderSetting("Length", 3, 1, 10, 1));
    public ChatAppend() {
        super("ChatAppend", "misc", Category.Misc);
        setChinese("消息后缀");
        INSTANCE = this;
    }

    @EventHandler
    public void onSendMessage(SendMessageEvent event) {
        if (nullCheck() || event.isCancelled() || AutoQueue.inQueue) return;
        String message = event.message;
        if (message.startsWith("/") || message.startsWith("!") || message.endsWith("|_Kawaii")) return;
        String suffix = "|_Kawaii";
        if (antiSpam.getValue()) {
            suffix += " <" + randomCode(Length.getValueInt()) + ">";
        }
        event.message = message + " " + suffix;
    }

    private String randomCode(int length) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
