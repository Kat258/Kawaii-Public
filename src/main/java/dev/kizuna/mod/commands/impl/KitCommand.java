package dev.kizuna.mod.commands.impl;

/*import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.kizuna.Kawaii;
import dev.kizuna.core.Manager;
import dev.kizuna.core.impl.CommandManager;
import dev.kizuna.core.impl.ConfigManager;
import dev.kizuna.mod.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.List;

import static dev.kizuna.core.Manager.getFile;
import static dev.kizuna.core.impl.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
public class KitCommand extends Command {
    public static AutoGear INSTANCE;
public KitCommand() {
    super("kit", "[create/set/del] [name]");
}

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            sendUsage();
            return;
        }
        switch (parameters[0]) {
            case "create" -> {
                if (parameters.length == 1) {
                    CommandManager.sendChatMessage("§fSaving config named " + parameters[0]);
                    getFile(parameters[0] + ".json");
                }
            }
            case "set" -> {
                if (parameters.length == 1) {
                    CommandManager.sendChatMessage("§fSaving config named " + parameters[0]);
                    getFile(parameters[0] + ".json");
                }
            }
        }

        if (parameters.length == 1) {
            CommandManager.sendChatMessage("§f" + parameters[0] + (Kawaii.FRIEND.isFriend(parameters[0]) ? " §ais friended" : " §cisn't friended"));
            return;
        }

        sendUsage();
    }


    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("list").executes(context -> {
            listMessage();
            return 1;
        }));
        builder.then(literal("create").then(argument("name", (ArgumentType)StringArgumentType.word()).executes(context -> {
            save((String)context.getArgument("name", String.class));
            return 1;
        })));
        builder.then(literal("set").then(argument("name", (ArgumentType)StringArgumentType.word()).executes(context -> {
            set((String)context.getArgument("name", String.class));
            return 1;
        })));
        builder.then(literal("del").then(argument("name", (ArgumentType)StringArgumentType.word()).executes(context -> {
            delete((String)context.getArgument("name", String.class));
            return 1;
        })));
    }

/*    public String getSelectedKit() {
        try {
            JsonObject json = (new JsonParser()).parse(new FileReader(file)).getAsJsonObject();
            if (!json.get("selected").getAsString().equals("none"))
                return json.get("selected").getAsString();
        } catch (Exception exception) {}
        CommandManager.sendChatMessage("Kit not found");
        return "";
    }

    public String getKitItems(String kit) {
        try {
            JsonObject json = (new JsonParser()).parse(new FileReader(file)).getAsJsonObject();
            return json.get(kit).getAsString();
        } catch (Exception exception) {
            CommandManager.sendChatMessage("Kit not found");
            return "";
        }
    }

    private void listMessage() {
        try {
            JsonObject json = (new JsonParser()).parse(new FileReader(file)).getAsJsonObject();
            CommandManager.sendChatMessage("Available kits:");
            for (int i = 0; i < json.entrySet().size(); i++) {
                String item = json.entrySet().toArray()[i].toString().split("=")[0];
                CommandManager.sendChatMessage("" + Formatting.GRAY + "-> " + Formatting.GRAY + item);
            }
        } catch (Exception e) {
            CommandManager.sendChatMessage("Error with kit cfg!");
        }
    }

    private void delete(String name) {
        try {
            JsonObject json = (new JsonParser()).parse(new FileReader(file)).getAsJsonObject();
            if (json.get(name) != null && !name.equals("selected")) {
                json.remove(name);
                if (json.get("selected").getAsString().equals(name))
                    json.addProperty("selected", "none");
                saveFile(json, name, "deleted");
            } else {
                CommandManager.sendChatMessage("Kit not found");
            }
        } catch (Exception e) {
            CommandManager.sendChatMessage("Kit not found");
        }
    }

    private void set(String name) {
        try {
            JsonObject json = (new JsonParser()).parse(new FileReader(file)).getAsJsonObject();
            if (json.get(name) != null && !name.equals("selected")) {
                json.addProperty("selected", name);
                saveFile(json, name, "selected");
                AutoGear.INSTANCE.setup();
            } else {
                CommandManager.sendChatMessage("Kit not found");
            }
        } catch (Exception e) {
            CommandManager.sendChatMessage("Kit not found");
        }
    }

    private void save(String name) {
        JsonObject json = new JsonObject();
        try {
            json = (new JsonParser()).parse(new FileReader(file)).getAsJsonObject();
            if (json.get(name) != null && !name.equals("selected")) {
                CommandManager.sendChatMessage("This kit arleady exist");
                return;
            }
        } catch (IOException e) {
            json.addProperty("selected", "none");
        }
        StringBuilder jsonInventory = new StringBuilder();
        for (ItemStack item : (mc.player.getInventory()).main)
            jsonInventory.append((item.getItem() instanceof net.minecraft.item.PotionItem) ? (item.getItem().getTranslationKey() + item.getItem().getTranslationKey()) : item.getItem().getTranslationKey()).append(" ");
        json.addProperty(name, jsonInventory.toString());
        saveFile(json, name, "saved");
    }

    private void saveFile(@NotNull JsonObject completeJson, String name, String operation) {
        try {
            File file = KitCommand.file;
            try {
                file.createNewFile();
            } catch (Exception exception) {}
            BufferedWriter bw = new BufferedWriter(new FileWriter(KitCommand.file));
            bw.write(completeJson.toString());
            bw.close();
            CommandManager.sendChatMessage("Kit " + Formatting.AQUA + name + Formatting.RESET + " " + operation);
        } catch (IOException e) {
            CommandManager.sendChatMessage("Error saving the file");
        }
    }
    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        return null;
    }
}*/
