/*package dev.kizuna.mod.modules.impl.combat;

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
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
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
    private final SliderSetting delay = (new SliderSetting("Delay",0.0f, 0.15f, 2.0f));
    private final Timer clickTimer = new Timer();
    private final Map<Integer, Item> regearInventory = new ConcurrentHashMap<>();

    public AutoRegear() {
        super("AutoRegear", "Automatically refills your inventory with gear", Category.Combat);
        INSTANCE = this;
    }


    @Override
    public void onEnable()
    {
        if (regearInventory.isEmpty())
        {
            CommandManager.sendChatMessage("No regear configuration set! Use the .regear command to set your regear inventory.");
        }
    }

    @EventHandler
    public void onTick(TickEvent event)
    {
        if (event.getStage() != Event.Stage.Pre)
        {
            return;
        }

        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler containerScreen) {
            for (Map.Entry<Integer, Item> entry : regearInventory.entrySet())
            {
                if (entry.getValue() == Items.AIR)
                {
                    continue;
                }

                int slotId = containerScreen.getInventory().size() + entry.getKey() - 9;
                ItemStack playerStack = mc.player.getInventory().getStack(slotId);
                if (playerStack.getCount() >= playerStack.getMaxCount())
                {
                    continue;
                }
                final Queue<Integer> validSlots = new ArrayDeque<>();

                // First organize player inventory

                // Then we can take from containers
                for (int i = 0; i < containerScreen.getInventory().size(); i++)
                {
                    Slot slot = containerScreen.getSlot(i);
                    if (slot.hasStack() && slot.getStack().getItem() == entry.getValue())
                    {
                        validSlots.add(i);
                    }
                }
                while (!validSlots.isEmpty() && playerStack.getCount() < playerStack.getMaxCount())
                {
                    if (clickTimer.passed(delay.getValue() * 1000))
                    {
                        int swapSlot = validSlots.remove();
                        pickupSlot(swapSlot);
                        pickupSlot(slotId);
                        clickTimer.reset();
                    }

                    playerStack = mc.player.getInventory().getStack(slotId);
                }
            }
        }

        if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulkerBoxScreenHandler)
        {
            for (Map.Entry<Integer, Item> entry : regearInventory.entrySet())
            {
                if (entry.getValue() == Items.AIR)
                {
                    continue;
                }

                int slotId = shulkerBoxScreenHandler.getStacks().size() + entry.getKey() - 9;
                ItemStack playerStack = mc.player.getInventory().getStack(slotId);
                if (playerStack.getCount() >= playerStack.getMaxCount())
                {
                    continue;
                }
                final Queue<Integer> validSlots = new ArrayDeque<>();

                // First organize player inventory

                // Then we can take from containers
                for (int i = 0; i < shulkerBoxScreenHandler.slots.size(); i++)
                {
                    Slot slot = shulkerBoxScreenHandler.slots.get(i);
                    if (slot.hasStack() && slot.getStack().getItem() == entry.getValue())
                    {
                        validSlots.add(i);
                    }
                }
                while (!validSlots.isEmpty() && playerStack.getCount() < playerStack.getMaxCount())
                {
                    if (clickTimer.passed(delay.getValue() * 1000))
                    {
                        int swapSlot = validSlots.remove();
                        pickupSlot(swapSlot);
                        pickupSlot(slotId);
                        clickTimer.reset();
                    }

                    playerStack = mc.player.getInventory().getStack(slotId);
                }
            }
        }
    }
    public static File regear = getFile("regear.json");

    public void clearPlayerInventory()
    {
        regearInventory.clear();
    }

    public void savePlayerInventory() {
        regearInventory.clear();
        for (int i = 0; i < 35; i++)
        {
            ItemStack stack = mc.player.getInventory().getStack(i);
            regearInventory.put(i < 9 ? i + 36 : i, stack.getItem());
        }
    }

    public void saveRegearFile()
    {
        if (regearInventory.isEmpty())
        {
            return;
        }
        try
        {
            Path regearFile = regear.toPath();
            if (!Files.exists(regearFile))
            {
                Files.createFile(regearFile);
            }
            FileWriter writer = new FileWriter(regearFile.toFile());
            JsonArray jsonArray = new JsonArray();
            for (Map.Entry<Integer, Item> entry : regearInventory.entrySet())
            {
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("slotId", entry.getKey());
                jsonObject1.addProperty("item", Registries.ITEM.getId(entry.getValue()).toString());
                jsonArray.add(jsonObject1);
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonArray, writer);
        }
        catch (IOException exception)
        {

        }
    }

    public void loadRegearFile()
    {
        Path regearFile = regear.toPath();
        if (!Files.exists(regearFile))
        {
            return;
        }
        try
        {
            regearInventory.clear();
            FileReader reader = new FileReader(regearFile.toFile());
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            for (JsonElement jsonElement : jsonArray)
            {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                int slotId = jsonObject.get("slotId").getAsInt();
                Item item = Registries.ITEM.get(Identifier.of(jsonObject.get("namespace").getAsString(), jsonObject.get("item").getAsString()));
                regearInventory.put(slotId, item);
            }
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
        }
    }
    private int pickupSlot(final int slot) {
        return click(slot, 0, SlotActionType.PICKUP);
    }
    private int click(int slot, int button, SlotActionType type) {
        if (slot < 0) {
            return -1;
        }
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        DefaultedList<Slot> defaultedList = screenHandler.slots;
        int i = defaultedList.size();
        ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
        for (Slot slot1 : defaultedList)
        {
            list.add(slot1.getStack().copy());
        }
        screenHandler.onSlotClick(slot, button, type, mc.player);
        Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();
        for (int j = 0; j < i; ++j)
        {
            ItemStack itemStack2;
            ItemStack itemStack = list.get(j);
            if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
            int2ObjectMap.put(j, itemStack2.copy());
        }
        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), slot, button, type, screenHandler.getCursorStack().copy(), int2ObjectMap));
        return screenHandler.getRevision();
    }
}
*/

