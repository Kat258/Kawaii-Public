package dev.kizuna.mod.modules.impl.misc;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.settings.impl.StringSetting;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.SendMessageEvent;
import dev.kizuna.mod.modules.Module;

public class ChatAppend extends Module {
	public static ChatAppend INSTANCE;
	private final StringSetting message = add(new StringSetting("append", Kawaii.NAME));
	public ChatAppend() {
		super("ChatAppend", Category.Misc);
		setChinese("消息后缀");
		INSTANCE = this;
	}

	@EventHandler
	public void onSendMessage(SendMessageEvent event) {
		if (nullCheck() || event.isCancelled() || AutoQueue.inQueue) return;
		String message = event.message;

		if (message.startsWith("/") || message.startsWith("!") || message.endsWith(this.message.getValue())) {
			return;
		}
		String suffix = this.message.getValue();
		message = message + " " + suffix;
		event.message = message;
	}
}