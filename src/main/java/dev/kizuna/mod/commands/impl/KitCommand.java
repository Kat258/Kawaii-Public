package dev.kizuna.mod.commands.impl;

import dev.kizuna.Kawaii;
import dev.kizuna.core.impl.CommandManager;
import dev.kizuna.mod.modules.impl.combat.AutoRegear;
import dev.kizuna.mod.commands.Command;

import java.util.ArrayList;
import java.util.List;

public class KitCommand extends Command {

    public KitCommand() {
        super("kit", "[save/clear]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length != 2) {
            sendUsage();
            return;
        }
        switch (parameters[0]) {
            case "save" -> {
                AutoRegear.INSTANCE.savePlayerInventory();
                CommandManager.sendChatMessage("Saved current regear inventory!");
            }
            case "clear" -> {
                AutoRegear.INSTANCE.clearPlayerInventory();
                CommandManager.sendChatMessage("Cleared current regear inventory!");
                return;
            }
        }
        sendUsage();
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        if (count == 1) {
            String input = seperated.get(seperated.size() - 1).toLowerCase();
            List<String> correct = new ArrayList<>();
            List<String> list = List.of("save", "clear");
            for (String x : list) {
                if (input.equalsIgnoreCase(Kawaii.PREFIX) || x.toLowerCase().startsWith(input)) {
                    correct.add(x);
                }
            }
            int numCmds = correct.size();
            String[] commands = new String[numCmds];

            int i = 0;
            for (String x : correct) {
                commands[i++] = x;
            }

            return commands;
        }
        return null;
    }
}
