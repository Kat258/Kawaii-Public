package dev.kizuna.mod.modules.impl.combat;

import com.google.common.collect.Lists;
import com.google.gson.*;
import dev.kizuna.api.events.Event;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.TickEvent;
import dev.kizuna.api.utils.math.Timer;
import dev.kizuna.core.impl.CommandManager;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static dev.kizuna.core.Manager.getFile;

public class AutoRegear extends Module {
    public static AutoRegear INSTANCE;
    private final SliderSetting delay = (new SliderSetting("Delay", 0.0f, 0.15f, 2.0f));
    private final Timer clickTimer = new Timer();
    private final Map<Integer, Item> regearInventory = new ConcurrentHashMap<>();

    public AutoRegear() {
        super("AutoRegear", "Automatically refills your inventory with gear", Category.Combat);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (regearInventory.isEmpty()) {
            CommandManager.sendChatMessage("No regear configuration set! Use the .regear command to set your regear inventory.");
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (event.getStage() != Event.Stage.Pre) return;

        // ---- 有人不判空有人不判空有人不判空有人不判空有人不判空有人不判空有人不判空有人不判空有人不判空 ----
        if (mc == null || mc.player == null) return;
        // ---- 有人不判空有人不判空有人不判空有人不判空有人不判空有人不判空有人不判空有人不判空有人不判空 ----

        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler == null) return;

        boolean hasContainerSlots = false;
        boolean hasPlayerSlots = false;
        for (Slot s : handler.slots) {
            if (s.inventory == mc.player.getInventory()) hasPlayerSlots = true;
            else hasContainerSlots = true;
        }

        if (hasContainerSlots && hasPlayerSlots) {
            processContainer(handler);
        }
    }

    private void processContainer(ScreenHandler handler) {
        if (mc == null || mc.player == null) return;

        for (Map.Entry<Integer, Item> entry : regearInventory.entrySet()) {
            final int playerSlotIndex = entry.getKey();
            final Item wanted = entry.getValue();
            if (wanted == Items.AIR) continue;

            if (playerSlotIndex < 0 || playerSlotIndex >= mc.player.getInventory().size()) continue;
            ItemStack playerStack = mc.player.getInventory().getStack(playerSlotIndex);
            if (playerStack.getCount() >= playerStack.getMaxCount()) continue;

            int playerScreenSlot = findScreenSlotForPlayerInventory(handler, playerSlotIndex);
            if (playerScreenSlot == -1) continue;

            final Queue<Integer> validSlots = new ArrayDeque<>();
            for (int i = 0; i < handler.slots.size(); i++) {
                Slot s = handler.slots.get(i);
                if (s.inventory != mc.player.getInventory() && s.hasStack() && s.getStack().getItem() == wanted) {
                    validSlots.add(i);
                }
            }

            while (!validSlots.isEmpty() && mc.player.getInventory().getStack(playerSlotIndex).getCount() < mc.player.getInventory().getStack(playerSlotIndex).getMaxCount()) {
                if (clickTimer.passed((long) (delay.getValue() * 1000))) {
                    int fromScreenSlot = validSlots.remove();
                    pickupSlot(fromScreenSlot);
                    pickupSlot(playerScreenSlot);
                    clickTimer.reset();
                } else {
                    break;
                }
            }
        }
    }

    private int findScreenSlotForPlayerInventory(ScreenHandler handler, int playerSlotIndex) {
        DefaultedList<Slot> slots = handler.slots;
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            try {
                if (s.inventory == mc.player.getInventory() && s.getIndex() == playerSlotIndex) {
                    return i;
                }
            } catch (Exception e) {
            }
        }
        return -1;
    }

    public static File regear = getFile("regear.json");

    public void clearPlayerInventory() {
        regearInventory.clear();
    }

    public void savePlayerInventory() {
        if (mc == null || mc.player == null) return;

        regearInventory.clear();
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            regearInventory.put(i, stack.getItem());
        }
    }

    public void saveRegearFile() {
        if (regearInventory.isEmpty()) return;
        try {
            Path regearFile = regear.toPath();
            if (!Files.exists(regearFile)) Files.createFile(regearFile);

            JsonArray jsonArray = new JsonArray();
            for (Map.Entry<Integer, Item> entry : regearInventory.entrySet()) {
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("slotId", entry.getKey());
                Identifier id = Registries.ITEM.getId(entry.getValue());
                jsonObject1.addProperty("item", id.toString());
                jsonArray.add(jsonObject1);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(regearFile.toFile())) {
                gson.toJson(jsonArray, writer);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void loadRegearFile() {
        Path regearFile = regear.toPath();
        if (!Files.exists(regearFile)) return;

        try (FileReader reader = new FileReader(regearFile.toFile())) {
            regearInventory.clear();
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            if (jsonArray == null) return;
            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                int slotId = jsonObject.get("slotId").getAsInt();
                String itemStr = jsonObject.get("item").getAsString();
                try {
                    Identifier id = Identifier.tryParse(itemStr);
                    if (id == null) continue;
                    Item item = Registries.ITEM.get(id);
                    if (item == null) continue;
                    regearInventory.put(slotId, item);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private int pickupSlot(final int slot) {
        return click(slot, 0, SlotActionType.PICKUP);
    }

    private int click(int slot, int button, SlotActionType type) {
        if (slot < 0) return -1;

        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        DefaultedList<Slot> defaultedList = screenHandler.slots;
        int i = defaultedList.size();
        ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
        for (Slot slot1 : defaultedList) {
            list.add(slot1.getStack().copy());
        }

        screenHandler.onSlotClick(slot, button, type, mc.player);

        Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();
        for (int j = 0; j < i; ++j) {
            ItemStack itemStack2;
            ItemStack itemStack = list.get(j);
            if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
            int2ObjectMap.put(j, itemStack2.copy());
        }

        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), slot, button, type, screenHandler.getCursorStack().copy(), int2ObjectMap));
        return screenHandler.getRevision();
    }
}
