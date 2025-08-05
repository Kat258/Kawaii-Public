package dev.kizuna.mod.modules.impl.movement;

import dev.kizuna.api.utils.Wrapper;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class MoveUp extends Module {
    private final BooleanSetting onlyMoveInBurrow = this.add(new BooleanSetting("StopOnGround", true));
    private final SliderSetting tryTime = this.add(new SliderSetting("TryTimes", 3, 1, 10, 1));
    private final BooleanSetting pEndChest = this.add(new BooleanSetting("EndChest", true));

    public MoveUp() {
        super("MoveUp", Category.Movement);
    }

    public static boolean isBurrowed(PlayerEntity entity, boolean Echest) {
        return doesBoxTouchBlock(entity.getBoundingBox(), Echest);
    }

    public static boolean doesBoxTouchBlock(Box box, boolean Echest) {
        int x = (int) Math.floor(box.minX);
        while ((double) x < Math.ceil(box.maxX)) {
            int y = (int) Math.floor(box.minY);
            while ((double) y < Math.ceil(box.maxY)) {
                int z = (int) Math.floor(box.minZ);
                while ((double) z < Math.ceil(box.maxZ)) {
                    if (Wrapper.mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.OBSIDIAN || Wrapper.mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.BEDROCK || Wrapper.mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.ENDER_CHEST && Echest || Wrapper.mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.RESPAWN_ANCHOR || Wrapper.mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.CRYING_OBSIDIAN) {
                        return true;
                    }
                    ++z;
                }
                ++y;
            }
            ++x;
        }
        return false;
    }

    @Override
    public void onEnable() {
        if (mc.player != null && (!this.onlyMoveInBurrow.getValue() || isBurrowed(mc.player, !this.pEndChest.getValue()))) {
            for (int i = 1; i <= tryTime.getValueInt(); i++) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
                mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ());
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
            }
        }
        disable();
    }
}