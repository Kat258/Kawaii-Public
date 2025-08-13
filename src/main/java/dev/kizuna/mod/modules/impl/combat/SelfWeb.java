package dev.kizuna.mod.modules.impl.combat;

import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.UpdateWalkingPlayerEvent;
import dev.kizuna.api.utils.entity.EntityUtil;
import dev.kizuna.api.utils.entity.InventoryUtil;
import dev.kizuna.api.utils.math.Timer;
import dev.kizuna.api.utils.world.BlockUtil;
import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.Module;

import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;

import java.util.ArrayList;

import static dev.kizuna.api.utils.world.BlockUtil.*;

public class SelfWeb extends Module {

    public SelfWeb() {
        super("SelfWeb", Category.Combat);
        setChinese("反活塞");
    }


    public final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    public final SliderSetting placeDelay = add(new SliderSetting("PlaceDelay", 60, 0, 500, () -> page.getValue() == Page.General));
    public final SliderSetting blocksPer = add(new SliderSetting("BlocksPerTick", 1, 1, 4, () -> page.getValue() == Page.General));
    public final BooleanSetting inventorySwap = add(new BooleanSetting("InventorySwap", true, () -> page.getValue() == Page.General));
    public final BooleanSetting usingPause = add(new BooleanSetting("UsingPause", true, () -> page.getValue() == Page.General));
    public final BooleanSetting rotate = add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotate));
    public final BooleanSetting silentSwitch = add(new BooleanSetting("SilentSwitch", true, () -> page.getValue() == Page.General));
    public final SliderSetting placeRange = add(new SliderSetting("PlaceRange", 5.0, 0.0, 6.0, 0.1, () -> page.getValue() == Page.General));
    public final SliderSetting targetRange = add(new SliderSetting("TargetRange", 8.0, 0.0, 8.0, 0.1, () -> page.getValue() == Page.General));

    public final BooleanSetting extraPlace = add(new BooleanSetting("ExtraPlace", true, () -> page.getValue() == Page.General));
    public final BooleanSetting onlyOneExtra = add(new BooleanSetting("OnlyOneExtra", true, () -> page.getValue() == Page.General));
    public final BooleanSetting extraPlaceII = add(new BooleanSetting("ExtraPlaceII", false, () -> page.getValue() == Page.General));
    public final BooleanSetting onlyOneExtraII = add(new BooleanSetting("OnlyOneExtraII", false, () -> page.getValue() == Page.General));

    public final BooleanSetting renderPreview = add(new BooleanSetting("RenderPreview", true, () -> page.getValue() == Page.General));

    private final Timer timer = new Timer();
    private int progress = 0;

    private final ArrayList<BlockPos> placed = new ArrayList<>();

    @Override
    public void onUpdate() {
        if (usingPause.getValue() && mc.player.isUsingItem()) return;
        if (!timer.passedMs(placeDelay.getValueInt())) return;

        progress = 0;

        BlockPos foot = mc.player.getBlockPos();

        tryPlace(foot);

        for (int dx = -1; dx <= 1 && progress < blocksPer.getValueInt(); dx += 2) {
            for (int dz = -1; dz <= 1 && progress < blocksPer.getValueInt(); dz += 2) {
                tryPlace(foot.add(dx, 0, dz));
            }
        }

        if (extraPlace.getValue() && progress < blocksPer.getValueInt()) {
            if (onlyOneExtra.getValue()) {
                BlockPos nearest = findNearestAir(foot);
                if (nearest != null) tryPlace(nearest);
            } else {
                for (int dx = -1; dx <= 1 && progress < blocksPer.getValueInt(); dx += 2) {
                    for (int dz = -1; dz <= 1 && progress < blocksPer.getValueInt(); dz += 2) {
                        BlockPos c = foot.add(dx, 0, dz);
                        if (mc.world.getBlockState(c).isAir()) tryPlace(c);
                    }
                }
            }
        }

        if (extraPlaceII.getValue() && progress < blocksPer.getValueInt()) {
            if (onlyOneExtraII.getValue()) {
                BlockPos near = findNearestAir(foot);
                if (near != null) tryPlace(near);
            } else {
                for (int dx = -1; dx <= 1 && progress < blocksPer.getValueInt(); dx += 2) {
                    for (int dz = -1; dz <= 1 && progress < blocksPer.getValueInt(); dz += 2) {
                        BlockPos c = foot.add(dx, 1, dz);
                        if (mc.world.getBlockState(c).isAir()) tryPlace(c);
                    }
                }
            }
        }

        timer.reset();
    }

    private void tryPlace(BlockPos pos) {
        if (progress >= blocksPer.getValueInt()) return;
        if (!mc.world.getBlockState(pos).isAir()) return;

        double dx = (pos.getX() + 0.5) - mc.player.getX();
        double dy = (pos.getY() + 0.5) - mc.player.getY();
        double dz = (pos.getZ() + 0.5) - mc.player.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        double range = targetRange.getValue();
        if (distSq > range * range) return;

        if (BlockUtil.getPlaceSide(pos, placeRange.getValue()) == null && !airPlace()) return;

        int webSlot = getWebSlot();
        if (webSlot == -1) return;

        boolean placedNow = placeBlock(pos, rotate.getValue(), webSlot);
        if (placedNow) {
            progress++;
            placed.add(pos);
            if (inventorySwap.getValue()) {
                doSwap(webSlot);
                EntityUtil.syncInventory();
            } else {
                doSwap(mc.player.getInventory().selectedSlot);
            }
        }
    }

    private BlockPos findNearestAir(BlockPos foot) {
        Vec3d eye = mc.player.getEyePos();
        double best = Double.MAX_VALUE;
        BlockPos detected = null;
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                BlockPos c = foot.add(dx, 0, dz);
                if (!mc.world.getBlockState(c).isAir()) continue;
                double d = Vec3d.ofCenter(c).squaredDistanceTo(eye);
                if (d < best) {
                    best = d; detected = c;
                }
            }
        }
        return detected;
    }

    private boolean placeBlock(BlockPos pos, boolean rotate, int slot) {
        Direction side = getPlaceSide(pos);
        if (side == null) {
            if (airPlace()) {
                return clickBlock(pos, Direction.DOWN, rotate, slot);
            }
            return false;
        }
        return clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
    }

    private boolean clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
        if (rotate) {
        }

        int old = mc.player.getInventory().selectedSlot;
        if (inventorySwap.getValue()) {
            if (silentSwitch.getValue()) InventoryUtil.inventorySwap(slot, old);
            else InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }

        boolean success = false;
        try {
            Vec3d hit = Vec3d.ofCenter(pos);
            BlockHitResult bhr = new BlockHitResult(hit, side, pos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            success = true;
        } catch (Throwable t) {
            try {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
                success = true;
            } catch (Throwable ignored) {
                success = false;
            }
        }

        return success;
    }

    private void doSwap(int slot) {
        if (silentSwitch.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getWebSlot() {
        if (inventorySwap.getValue()) {
            return InventoryUtil.findBlockInventorySlot(Blocks.COBWEB);
        } else {
            return InventoryUtil.findBlock(Blocks.COBWEB);
        }
    }

    public enum Page {
        General,
        Rotate
    }
}
