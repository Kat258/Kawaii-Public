package dev.kizuna.mod.commands.impl;

import dev.kizuna.Kawaii;
import dev.kizuna.core.Manager;
import dev.kizuna.core.impl.CommandManager;
import dev.kizuna.core.impl.ConfigManager;
import dev.kizuna.mod.commands.Command;

import java.util.List;

public class LoadCommand extends Command {

	public LoadCommand() {
		super("load", "[config]");
	}

	@Override
	public void runCommand(String[] parameters) {
		if (parameters.length == 0) {
			sendUsage();
			return;
		}
		CommandManager.sendChatMessage("§fLoading..");
		ConfigManager.options = Manager.getFile(parameters[0] + ".cfg");
		Kawaii.CONFIG = new ConfigManager();
		Kawaii.PREFIX = Kawaii.CONFIG.getString("prefix", Kawaii.PREFIX);
		Kawaii.CONFIG.loadSettings();
        ConfigManager.options = Manager.getFile("options.txt");
		Kawaii.save();
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		return null;
	}
}
