package dev.kizuna.mod.modules.impl.combat;

import dev.kizuna.mod.modules.Module;
import dev.kizuna.api.utils.world.BlockUtil;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

public class AntiTrap extends Module {
    public AntiTrap() {
        super("AntiTrap", Category.Combat);
    }

    @Override
    public void onUpdate() {
        if (nullCheck()) {
            disable();
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos headPos = playerPos.up(2);
        java.util.List<BlockPos> candidatePositions = new java.util.ArrayList<>();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos checkPos = headPos.add(x, 0, z);
                if (hasBlock(checkPos)) {
                    candidatePositions.add(checkPos.up());
                }
            }
        }

        if (hasBlock(headPos)) {
            candidatePositions.add(headPos.up());
        }

        for (BlockPos pos : candidatePositions) {
            if (!hasBlock(pos) && !hasBlock(pos.up()) && !hasBlock(pos.up(2))) {
                teleport(pos);
                disable();
                return;
            }
        }

        disable();
    }

    private boolean hasBlock(BlockPos pos) {
        return !mc.world.isAir(pos) && BlockUtil.getBlock(pos).getHardness() >= 0;
    }

    private void teleport(BlockPos pos) {
        if (hasBlock(pos) || hasBlock(pos.up()) || hasBlock(pos.up(2))) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        mc.player.setPosition(x, y, z);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true));
    }
}