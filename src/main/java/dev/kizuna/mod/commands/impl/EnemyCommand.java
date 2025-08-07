package dev.kizuna.mod.commands.impl;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.commands.Command;
import dev.kizuna.core.impl.CommandManager;

import java.util.ArrayList;
import java.util.List;

public class EnemyCommand extends Command {

    public EnemyCommand() {
        super("enemy", "[name/reset/list] | [add/remove] [name]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            sendUsage();
            return;
        }
        switch (parameters[0]) {
            case "reset" -> {
                Kawaii.ENEMY.enemyList.clear();
                CommandManager.sendChatMessage("§fEnemies list got reset");
                return;
            }
            case "list" -> {
                if (Kawaii.ENEMY.enemyList.isEmpty()) {
                    CommandManager.sendChatMessage("§fEnemies list is empty");
                    return;
                }
                StringBuilder enemies = new StringBuilder();
                int time = 0;
                boolean first = true;
                boolean start = true;
                for (String name : Kawaii.ENEMY.enemyList) {
                    if (!first) {
                        enemies.append(", ");
                    }
                    enemies.append(name);
                    first = false;
                    time++;
                    if (time > 3) {
                        CommandManager.sendChatMessage((start ? "§eenemies §a" : "§a") + enemies);
                        enemies = new StringBuilder();
                        start = false;
                        first = true;
                        time = 0;
                    }
                }
                if (first) {
                    CommandManager.sendChatMessage("§a" + enemies);
                }
                return;
            }
            case "add" -> {
                if (parameters.length == 2) {
                    Kawaii.ENEMY.addEnemy(parameters[1]);
                    CommandManager.sendChatMessage("§f" + parameters[1] + (Kawaii.ENEMY.isEnemy(parameters[1]) ? " §ahas been enemy" : " §cremove enemy"));
                    return;
                }
                sendUsage();
                return;
            }
            case "remove" -> {
                if (parameters.length == 2) {
                    Kawaii.ENEMY.removeEnemy(parameters[1]);
                    CommandManager.sendChatMessage("§f" + parameters[1] + (Kawaii.ENEMY.isEnemy(parameters[1]) ? " §ahas been enemy" : " §cremove enemy"));
                    return;
                }
                sendUsage();
                return;
            }
        }

        if (parameters.length == 1) {
            CommandManager.sendChatMessage("§f" + parameters[0] + (Kawaii.ENEMY.isEnemy(parameters[0]) ? " §ais enemy" : " §cisn't enemy"));
            return;
        }

        sendUsage();
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        if (count == 1) {
            String input = seperated.get(seperated.size() - 1).toLowerCase();
            List<String> correct = new ArrayList<>();
            List<String> list = List.of("add", "remove", "list", "reset");
            for (String x : list) {
                if (input.equalsIgnoreCase(Kawaii.PREFIX + "Enemy") || x.toLowerCase().startsWith(input)) {
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
