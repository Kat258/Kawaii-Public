package dev.kizuna.mod.commands.impl;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.commands.Command;
import dev.kizuna.core.impl.CommandManager;

import java.util.List;

public class PrefixCommand extends Command {

	public PrefixCommand() {
		super("prefix", "[prefix]");
	}

	@Override
	public void runCommand(String[] parameters) {
		if (parameters.length == 0) {
			sendUsage();
			return;
		}
		if (parameters[0].startsWith("/")) {
			CommandManager.sendChatMessage("§fPlease specify a valid §bprefix.");
			return;
		}
		Kawaii.PREFIX = parameters[0];
		CommandManager.sendChatMessage("§bPrefix §fset to §e" + parameters[0]);
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		return null;
	}
}
