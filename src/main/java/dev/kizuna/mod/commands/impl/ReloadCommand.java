package dev.kizuna.mod.commands.impl;

import dev.kizuna.Kawaii;
import dev.kizuna.core.impl.CommandManager;
import dev.kizuna.core.impl.ConfigManager;
import dev.kizuna.mod.commands.Command;

import java.util.List;

public class ReloadCommand extends Command {

	public ReloadCommand() {
		super("reload", "");
	}

	@Override
	public void runCommand(String[] parameters) {
		CommandManager.sendChatMessage("§fReloading..");
		Kawaii.CONFIG = new ConfigManager();
		Kawaii.PREFIX = Kawaii.CONFIG.getString("prefix", Kawaii.PREFIX);
		Kawaii.CONFIG.loadSettings();
		Kawaii.XRAY.read();
		Kawaii.TRADE.read();
		Kawaii.FRIEND.read();
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		return null;
	}
}
