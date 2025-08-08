package dev.kizuna.mod.modules.impl.player;

import dev.kizuna.Kawaii;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.eventbus.EventPriority;
import dev.kizuna.api.events.impl.LookAtEvent;
import dev.kizuna.api.events.impl.MoveEvent;
import dev.kizuna.api.utils.entity.InventoryUtil;
import dev.kizuna.api.utils.entity.MovementUtil;
import dev.kizuna.api.utils.math.Timer;
import dev.kizuna.api.utils.world.BlockUtil;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.impl.client.AntiCheat;
import dev.kizuna.mod.modules.impl.movement.SafeWalk;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.util.math.*;

public class Scaffold extends Module {
    private final BooleanSetting tower = add(new BooleanSetting("Tower", true));
    private final BooleanSetting keepY = add(new BooleanSetting("KeepY", true));
    private final BooleanSetting autojump = add(new BooleanSetting("AutoJump", true));
    private final BooleanSetting packetPlace = add(new BooleanSetting("PacketPlace", false));
    private final BooleanSetting safeWalk = add(new BooleanSetting("SafeWalk", false));
    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", true).setParent());
    private final BooleanSetting yawStep = add(new BooleanSetting("YawStep", false, () -> rotate.isOpen()));
    private final SliderSetting steps = add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> rotate.isOpen()));
    private final BooleanSetting checkFov = add(new BooleanSetting("OnlyLooking", true, () -> rotate.isOpen()));
    private final SliderSetting fov = add(new SliderSetting("Fov", 5f, 0f, 30f, () -> checkFov.getValue() && rotate.isOpen()));
    private final SliderSetting priority = add(new SliderSetting("Priority", 10,0 ,100, () ->rotate.isOpen()));
    public final SliderSetting rotateTime = add(new SliderSetting("KeepRotate", 1000, 0, 3000, 10));

    public Scaffold() {
        super("Scaffold", Category.Player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMove(MoveEvent event) {
        if (!safeWalk.getValue()) return;
        SafeWalk.INSTANCE.onMove(event);
    }

    private final Timer timer = new Timer();

    private Vec3d vec;

    @EventHandler
    public void onRotation(LookAtEvent event) {
        if (rotate.getValue() && !timer.passedMs(rotateTime.getValueInt()) && vec != null) {
            event.setTarget(vec, steps.getValueFloat(), priority.getValueFloat());
        }
    }


    private final Timer towerTimer = new Timer();
    private int groundPosY;

    @Override
    public void onUpdate() {
        if (mc.player != null && autojump.getValue() && mc.player.isOnGround() && mc.options.forwardKey.isPressed()) {
            mc.player.jump();
        }
        int posY = 0;
        if (mc.player != null) {
            posY = (int) Math.round(mc.player.getY()) - 1;
        }
        if (keepY.getValue() && MovementUtil.isInputtingMovement()) {
            if (mc.player.isOnGround() || groundPosY == -1)
            {
                groundPosY = (int) Math.floor(mc.player.getY()) - 1;
            }
            posY = groundPosY;
        }
        BlockPos placePos = null;
        if (mc.player != null) {
            placePos = getRoundedBlockPos(mc.player.getX(), posY, mc.player.getZ());
        }
        int block = InventoryUtil.findBlock();
        if (block == -1) return;
        if (BlockUtil.clientCanPlace(placePos, false)) {
            int old = mc.player.getInventory().selectedSlot;
            if (BlockUtil.getPlaceSide(placePos) == null) {
                double distance = 1000;
                BlockPos bestPos = null;
                for (Direction i : Direction.values()) {
                    if (i == Direction.UP) continue;
                    if (placePos != null && BlockUtil.canPlace(placePos.offset(i))) {
                        if (bestPos == null || mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos()) < distance) {
                            bestPos = placePos.offset(i);
                            distance = mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos());
                        }
                    }
                }
                if (bestPos != null) {
                    placePos = bestPos;
                } else {
                    return;
                }
            }
            if (rotate.getValue()) {
                Direction side = BlockUtil.getPlaceSide(placePos);
                vec = (placePos.offset(side).toCenterPos().add(side.getOpposite().getVector().getX() * 0.5, side.getOpposite().getVector().getY() * 0.5, side.getOpposite().getVector().getZ() * 0.5));
                timer.reset();
                if (!faceVector(vec)) return;
            }
            InventoryUtil.switchToSlot(block);
            BlockUtil.placeBlock(placePos, false, packetPlace.getValue());
            InventoryUtil.switchToSlot(old);
            if (rotate.getValue() && AntiCheat.INSTANCE.snapBack.getValue()) {
                Kawaii.ROTATION.snapBack();
            }
            if (tower.getValue() && mc.options.jumpKey.isPressed() && !MovementUtil.isMoving()) {
                MovementUtil.setMotionY(0.42);
                MovementUtil.setMotionX(0);
                MovementUtil.setMotionZ(0);
                if (this.towerTimer.passedMs(1500L)) {
                    MovementUtil.setMotionY(-0.28);
                    this.towerTimer.reset();
                }
            } else {
                this.towerTimer.reset();
            }
        }
    }

    public static BlockPos getRoundedBlockPos(final double x, final double y, final double z) {
        final int flooredX = MathHelper.floor(x);
        final int flooredY = (int) Math.round(y);
        final int flooredZ = MathHelper.floor(z);
        return new BlockPos(flooredX, flooredY, flooredZ);
    }

    private boolean faceVector(Vec3d directionVec) {
        if (!yawStep.getValue()) {
            Kawaii.ROTATION.lookAt(directionVec);
            return true;
        } else {
            if (Kawaii.ROTATION.inFov(directionVec, fov.getValueFloat())) {
                return true;
            }
        }
        return !checkFov.getValue();
    }
}
