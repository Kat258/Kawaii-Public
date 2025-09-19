package dev.kizuna.mod.modules.impl.player;

import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.entity.EntityUtil;
import dev.kizuna.api.utils.entity.InventoryUtil;
import dev.kizuna.api.utils.math.Timer;
import dev.kizuna.api.utils.world.BlockUtil;
import dev.kizuna.core.impl.CommandManager;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import java.util.concurrent.atomic.AtomicInteger;

public class EnderchestStealer extends Module {
    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true));
    private final SliderSetting disableTime = add(new SliderSetting("DisableTime", 500, 0, 1000));
    public final BooleanSetting rotate = add(new BooleanSetting("Rotate", true));
    private final BooleanSetting place = add(new BooleanSetting("Place", true));
    private final BooleanSetting detectMining = add(new BooleanSetting("DetectMining", true));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting preferOpen = add(new BooleanSetting("PerferOpen", true));
    private final BooleanSetting open = add(new BooleanSetting("Open", true));
    private final BooleanSetting close = add(new BooleanSetting("Close", true));
    private final SliderSetting range = add(new SliderSetting("Range", 4.0f, 0.0f, 6f));
    private final SliderSetting minRange = add(new SliderSetting("MinRange", 1.0f, 0.0f, 3f));
    private final BooleanSetting mine = add(new BooleanSetting("Mine", true));
    private final BooleanSetting take = add(new BooleanSetting("Take", true));
    private final BooleanSetting mergeStacks = add(new BooleanSetting("MergeStacks", true, take::getValue));
    private final SliderSetting empty = add(new SliderSetting("Empty", 1, 0, 36, take::getValue));
    private final SliderSetting maxTakeCount = add(new SliderSetting("TakeCount", 64, 1, 64, take::getValue));

    public EnderchestStealer() {
        super("EnderchestStealer", "Auto place and steal from enderchests", Category.Player);
    }

    private final Timer timer = new Timer();
    BlockPos placePos = null;
    private final Timer disableTimer = new Timer();

    @Override
    public void onEnable() {
        placePos = null;
        disableTimer.reset();
        if (nullCheck()) {
            return;
        }
        int oldSlot = mc.player.getInventory().selectedSlot;
        if (!this.place.getValue()) {
            return;
        }
        double distance = 100;
        BlockPos bestPos = null;
        for (BlockPos pos : BlockUtil.getSphere((float) range.getValue())) {
            if (!BlockUtil.isAir(pos.up())) continue;
            if (preferOpen.getValue() && mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) return;
            if (MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos())) < minRange.getValue()) continue;
            if (!BlockUtil.clientCanPlace(pos, false)
                    || !BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP)
                    || !BlockUtil.canClick(pos.offset(Direction.DOWN))
            ) continue;
            if (detectMining.getValue() && (Kawaii.BREAK.isMining(pos) || pos.equals(PacketMine.breakPos))) continue;
            if (bestPos == null || MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos())) < distance) {
                distance = MathHelper.sqrt((float) mc.player.squaredDistanceTo(pos.toCenterPos()));
                bestPos = pos;
            }
        }
        if (bestPos != null) {
            int slot = findEnderchest();
            if (slot == -1) {
                CommandManager.sendChatMessage("§c[!] No enderchest found");
                return;
            }
            doSwap(slot);
            placeBlock(bestPos);
            placePos = bestPos;
            if (inventory.getValue()) {
                doSwap(slot);
                EntityUtil.syncInventory();
            } else {
                doSwap(oldSlot);
            }
            timer.reset();
        } else {
            CommandManager.sendChatMessage("§c[!] No place pos found");
        }
    }

    public int findEnderchest() {
        final AtomicInteger atomicInteger = new AtomicInteger(-1);
        if (findClass(Blocks.ENDER_CHEST.getClass()) != -1) {
            atomicInteger.set(findClass(Blocks.ENDER_CHEST.getClass()));
        }
        return atomicInteger.get();
    }

    public int findClass(Class clazz) {
        if (inventory.getValue()) {
            return InventoryUtil.findClassInventorySlot(clazz);
        } else {
            return InventoryUtil.findClass(clazz);
        }
    }

    private void doSwap(int slot) {
        if (inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    @Override
    public void onDisable() {
        if (mine.getValue()) {
            if (placePos != null) {
                PacketMine.INSTANCE.mine(placePos);
            }
        }
    }

    BlockPos openPos;
    boolean opend = false;

    @Override
    public void onUpdate() {
        if (!(mc.currentScreen instanceof HandledScreen)) {
            if (opend) {
                opend = false;
                if (autoDisable.getValue()) disable2();
                return;
            }
            if (open.getValue()) {
                if (placePos != null && MathHelper.sqrt((float) mc.player.squaredDistanceTo(placePos.toCenterPos())) <= range.getValue()) {
                    if (mc.world.getBlockState(placePos).getBlock() == Blocks.ENDER_CHEST) {
                        openPos = placePos;
                        BlockUtil.clickBlock(placePos, BlockUtil.getClickSide(placePos), rotate.getValue());
                    }
                }
            } else if (!this.take.getValue()) {
                if (autoDisable.getValue()) this.disable2();
            }
            return;
        }

        opend = true;
        if (!this.take.getValue()) {
            if (autoDisable.getValue()) this.disable2();
            return;
        }

        boolean take = false;
        if (mc.player.currentScreenHandler != null) {
            int takeCount = 0;
            for (int i = mc.player.currentScreenHandler.slots.size() - 1; i >= 0; i--) {
                Slot slot = mc.player.currentScreenHandler.slots.get(i);
                if (slot.id < 27 && !slot.getStack().isEmpty() && takeCount < maxTakeCount.getValueInt()) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                    take = true;
                    takeCount++;
                    if (autoDisable.getValue()) {
                        disable2();
                        break;
                    }
                    if (takeCount >= maxTakeCount.getValueInt()) {
                        break;
                    }
                }
            }
        }

        if (autoDisable.getValue() && !take) this.disable2();
    }

    private void disable2() {
        if (close.getValue()) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            mc.player.closeHandledScreen();
        }
        disable();
    }

    private void placeBlock(BlockPos pos) {
        BlockUtil.clickBlock(pos.offset(Direction.DOWN), Direction.UP, rotate.getValue());
    }
}
