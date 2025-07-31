package dev.kizuna.mod.commands.impl;

import dev.kizuna.Kawaii;
import dev.kizuna.core.impl.CommandManager;
import dev.kizuna.mod.commands.Command;

import java.util.List;

public class ReloadAllCommand extends Command {

	public ReloadAllCommand() {
		super("reloadall", "");
	}

	@Override
	public void runCommand(String[] parameters) {
		CommandManager.sendChatMessage("§fReloading..");
		Kawaii.unload();
        try {
            Kawaii.load();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		return null;
	}
}
