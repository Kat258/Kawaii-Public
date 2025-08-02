package dev.kizuna.mod.modules.impl.misc;

import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.TickEvent;
import dev.kizuna.core.impl.CommandManager;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
/*
public class AutoGear extends Module {
    public static AutoGear INSTANCE;
    public final SliderSetting actionDelay = add(new SliderSetting("Speed", 1, 1, 50, 1));
    public final SliderSetting clicksPerAction = add(new SliderSetting("Speed", 1, 1, 108, 1));


    private HashMap<Integer, String> expectedInv;

    private int delay;

    public AutoGear() {
        super("AutoGear", Category.Misc);
        this.expectedInv = new HashMap<>();
        this.delay = 0;
        INSTANCE = this;
    }

    public void onActivate() {
        setup();
    }

    public void setup() {
        String selectedKit = KitCommand.INSTANCE.getSelectedKit();
        if (selectedKit.isEmpty()) {
            if (isOn())
                toggle();
            CommandManager.sendChatMessage("No kit is selected! Use the kit command");
            return;
        }
        CommandManager.sendChatMessage("Selected kit -> " + Formatting.AQUA + selectedKit);
        String kitItems = KitCommand.INSTANCE.getKitItems(selectedKit);
        if (kitItems.isEmpty() || (kitItems.split(" ")).length != 36) {
            if (isOn())
                toggle();
            CommandManager.sendChatMessage("There was an error in the kit configuration! Create the kit again");
            return;
        }
        String[] items = kitItems.split(" ");
        this.expectedInv = new HashMap<>();
        for (int i = 0; i < 36; i++) {
            if (!items[i].equals("block.minecraft.air"))
                this.expectedInv.put(Integer.valueOf(i), items[i]);
        }
    }

    public void clickSlot(int id) {
        if (id == -1 || this.mc.interactionManager == null || this.mc.player == null)
            return;
        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, id, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
    }

    @EventHandler
    public void onUpdate() {
        if (this.mc.player == null || this.mc.world == null)
            return;
        if (this.delay > 0) {
            this.delay--;
            return;
        }
        if (this.expectedInv.isEmpty()) {
            setup();
            return;
        }
        int actions = 0;
        ScreenHandler handler = this.mc.player.currentScreenHandler;
        if (handler.slots.size() != 63 && handler.slots.size() != 90)
            return;
        ArrayList<Integer> clickSequence = buildClickSequence(handler);
        for (Iterator<Integer> iterator = clickSequence.iterator(); iterator.hasNext(); ) {
            int s = iterator.next().intValue();
            clickSlot(s);
            actions++;
            if (actions >= this.clicksPerAction.getValueInt())
                break;
        }
        this.delay = this.actionDelay.getValueInt();
    }

    private int searchInContainer(String name, boolean lower, ScreenHandler handler) {
        ItemStack cursorStack = handler.getCursorStack();
        if (((cursorStack.getItem() instanceof net.minecraft.item.PotionItem) ? (
            cursorStack.getItem().getTranslationKey() + cursorStack.getItem().getTranslationKey()) :
            cursorStack.getItem().getTranslationKey()).equals(name))
            return -2;
        for (int i = 0; i < (lower ? 26 : 53); i++) {
            ItemStack stack = handler.getCursorStack();
            if (((stack.getItem() instanceof net.minecraft.item.PotionItem) ? (
                stack.getItem().getTranslationKey() + stack.getItem().getTranslationKey()) :
                stack.getItem().getTranslationKey()).equals(name))
                return i;
        }
        return -1;
    }

    private ArrayList<Integer> buildClickSequence(ScreenHandler handler) {
        ArrayList<Integer> clicks = new ArrayList<>();
        for (Iterator<Integer> iterator = this.expectedInv.keySet().iterator(); iterator.hasNext(); ) {
            int s = iterator.next().intValue();
            int lower = (s < 9) ? (s + 54) : (s + 18);
            int upper = (s < 9) ? (s + 81) : (s + 45);
            ItemStack itemInslot = handler.slots.get((handler.slots.size() == 63) ? lower : upper).getStack();
            if (((itemInslot.getItem() instanceof net.minecraft.item.PotionItem) ? (
                itemInslot.getItem().getTranslationKey() + itemInslot.getItem().getTranslationKey()) :
                itemInslot.getItem().getTranslationKey()).equals(this.expectedInv.get(Integer.valueOf(s))))
                continue;
            int slot = searchInContainer(this.expectedInv.get(Integer.valueOf(s)), (handler.slots.size() == 63), handler);
            if (slot == -2) {
                clicks.add(Integer.valueOf((handler.slots.size() == 63) ? lower : upper));
                continue;
            }
            if (slot != -1) {
                clicks.add(Integer.valueOf(slot));
                clicks.add(Integer.valueOf((handler.slots.size() == 63) ? lower : upper));
                clicks.add(Integer.valueOf(slot));
            }
        }
        return clicks;
    }
}*/
