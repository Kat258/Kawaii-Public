package dev.kizuna.mod.modules.impl.movement;

import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.world.BlockUtil;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

public class VClip extends Module {
    public VClip() {
        super("VClip", Category.Movement);
        setChinese("纵向穿墙");
    }
    final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Jump));
    final SliderSetting yaw = add(new SliderSetting("Yaw",1,1,10, () -> mode.getValue() == Mode.Teleport));

    public enum Mode {
        Glitch,
        Teleport,
        Jump,
        EscapeTrap
    }

    @Override
    public void onUpdate() {
        disable();
        switch (mode.getValue()) {
            case Teleport -> {
                mc.player.setPosition(mc.player.getX(), mc.player.getY() + yaw.getValue(), mc.player.getZ());
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
            }
            case Jump -> {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
                //mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.9999957640154541, mc.player.getZ(), false));
                mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ());
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
            }
            case Glitch -> {
                double posX = mc.player.getX();
                double posY = Math.round(mc.player.getY());
                double posZ = mc.player.getZ();
                boolean onGround = mc.player.isOnGround();

                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX,
                        posY,
                        posZ,
                        onGround));

                double halfY = 2 / 400.0;
                posY -= halfY;

                mc.player.setPosition(posX, posY, posZ);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX,
                        posY,
                        posZ,
                        onGround));

                posY -= halfY * 300.0;
                mc.player.setPosition(posX, posY, posZ);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX,
                        posY,
                        posZ,
                        onGround));
            }
            case EscapeTrap -> {
                if (Kawaii.beta) {
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
                }
            }
        }
    }
    private boolean hasBlock(BlockPos pos) {
        return !mc.world.isAir(pos);
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
