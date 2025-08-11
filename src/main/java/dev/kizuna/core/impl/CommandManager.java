package dev.kizuna.core.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.kizuna.Kawaii;
import dev.kizuna.api.interfaces.IChatHudHook;
import dev.kizuna.api.utils.Wrapper;
import dev.kizuna.mod.commands.Command;
import dev.kizuna.mod.commands.impl.*;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.impl.client.ClientSetting;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.HashMap;

public class CommandManager implements Wrapper {
    public static final String syncCode = "§)";
    private final HashMap<String, Command> commands = new HashMap<>();

    public CommandManager() {
        registerCommand(new AimCommand());
        registerCommand(new BindCommand());
        registerCommand(new BindsCommand());
        registerCommand(new ClipCommand());
        registerCommand(new KitCommand());
        registerCommand(new EnemyCommand());
        registerCommand(new FriendCommand());
        registerCommand(new XrayCommand());
        registerCommand(new GamemodeCommand());
        registerCommand(new LoadCommand());
        registerCommand(new PingCommand());
        registerCommand(new PrefixCommand());
        registerCommand(new RejoinCommand());
        registerCommand(new ReloadCommand());
        registerCommand(new ReloadAllCommand());
        registerCommand(new SaveCommand());
        registerCommand(new TeleportCommand());
        registerCommand(new TCommand());
        registerCommand(new ToggleCommand());
        registerCommand(new TradeCommand());
    }

    private void registerCommand(Command command) {
        commands.put(command.getName(), command);
    }

    public Command getCommandBySyntax(String string) {
        return this.commands.get(string);
    }

    public HashMap<String, Command> getCommands() {
        return this.commands;
    }

    public void command(String[] commandIn) {

        // Get the command from the user's message. (Index 0 is Username)
        Command command = commands.get(commandIn[0].substring(Kawaii.PREFIX.length()).toLowerCase());

        // If the command does not exist, throw an error.
        if (command == null)
            sendChatMessage("§cInvalid Command");
        else {
            // Otherwise, create a new parameter list.
            String[] parameterList = new String[commandIn.length - 1];
            System.arraycopy(commandIn, 1, parameterList, 0, commandIn.length - 1);
            if (parameterList.length == 1 && parameterList[0].equals("help")) {
                command.sendUsage();
                return;
            }
            // Runs the command.
            command.runCommand(parameterList);
        }
    }
    public static LiteralArgumentBuilder<CommandSource> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }
    public static void sendChatMessage(String message) {
        if (Module.nullCheck()) return;
        if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
            mc.inGameHud.getChatHud().addMessage(Text.of("§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] " + message));
            return;
        }
        mc.inGameHud.getChatHud().addMessage(Text.of(syncCode + "§r" + ClientSetting.INSTANCE.hackName.getValue() + "§f " + message));
    }

    public static void sendChatMessageWidthId(String message, int id) {
        if (Module.nullCheck()) return;
        if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
            ((IChatHudHook) mc.inGameHud.getChatHud()).addMessage(Text.of("§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] " + message), id);
            return;
        }
        ((IChatHudHook) mc.inGameHud.getChatHud()).addMessage(Text.of(syncCode + "§r" + ClientSetting.INSTANCE.hackName.getValue() + "§f " + message), id);
    }

    public static void sendChatMessageWidthIdNoSync(String message, int id) {
        if (Module.nullCheck()) return;
        ((IChatHudHook) mc.inGameHud.getChatHud()).addMessage(Text.of("§f" + message), id);
    }
}
