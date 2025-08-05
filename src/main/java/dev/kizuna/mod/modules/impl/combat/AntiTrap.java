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
        boolean foundBlock = false;
        BlockPos targetPos = null;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos checkPos = headPos.add(x, 0, z);
                if (hasBlock(checkPos)) {targetPos = checkPos;foundBlock = true;break;}
            }
            if (foundBlock) break;
        }

        if (!foundBlock && hasBlock(headPos)) {
            targetPos = headPos;
            foundBlock = true;
        }
        if (foundBlock && targetPos != null) {
            teleport(targetPos.up());
            disable();
        } else {disable();}
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