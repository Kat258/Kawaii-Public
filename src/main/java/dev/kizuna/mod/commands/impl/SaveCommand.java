package dev.kizuna.mod.commands.impl;

import dev.kizuna.core.Manager;
import dev.kizuna.Kawaii;
import dev.kizuna.core.impl.CommandManager;
import dev.kizuna.core.impl.ConfigManager;
import dev.kizuna.mod.commands.Command;

import java.util.List;

public class SaveCommand extends Command {

	public SaveCommand() {
		super("save", "");
	}

	@Override
	public void runCommand(String[] parameters) {
		if (parameters.length == 1) {
			CommandManager.sendChatMessage("§fSaving config named " + parameters[0]);
			ConfigManager.options = Manager.getFile(parameters[0] + ".cfg");
			Kawaii.save();
			ConfigManager.options = Manager.getFile("options.txt");
		} else {
			CommandManager.sendChatMessage("§fSaving..");
		}
		Kawaii.save();
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		return null;
	}
}
