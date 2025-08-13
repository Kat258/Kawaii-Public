package dev.kizuna.mod.modules.impl.movement;

import dev.kizuna.api.events.Event;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.TickEvent;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BedrockPhase extends Module {
    private static final double EPSILON = 0.029;

    public BedrockPhase() {
        super("BedrockPhase", Category.Movement);
        setChinese("基岩穿墙绕过");
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (event.getStage() != Event.Stage.Post) return;

        PlayerEntity player = mc.player;
        if (player == null) return;

        Vec3d pos = player.getPos();
        double px = pos.x;
        double py = pos.y;
        double pz = pos.z;

        BlockPos center = new BlockPos(
            (int) Math.floor(px),
            (int) Math.floor(py),
            (int) Math.floor(pz)
        );
        double eps = EPSILON;
        double minX = Double.NEGATIVE_INFINITY;
        double maxX = Double.POSITIVE_INFINITY;
        double minZ = Double.NEGATIVE_INFINITY;
        double maxZ = Double.POSITIVE_INFINITY;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos checkPos = center.add(dx, 0, dz);
                if (mc.world.getBlockState(checkPos).getBlock() == Blocks.BEDROCK) {
                    if (checkPos.getX() > center.getX()) {
                        maxX = Math.min(maxX, checkPos.getX() - eps);
                    }
                    if (checkPos.getX() < center.getX()) {
                        minX = Math.max(minX, checkPos.getX() + 1 + eps);
                    }
                    if (checkPos.getZ() > center.getZ()) {
                        maxZ = Math.min(maxZ, checkPos.getZ() - eps);
                    }
                    if (checkPos.getZ() < center.getZ()) {
                        minZ = Math.max(minZ, checkPos.getZ() + 1 + eps);
                    }
                }
            }
        }

        double newX = px;
        double newZ = pz;

        if (px < minX) newX = minX;
        if (px > maxX) newX = maxX;
        if (pz < minZ) newZ = minZ;
        if (pz > maxZ) newZ = maxZ;

        if (newX != px || newZ != pz) {
            player.setVelocity(0, player.getVelocity().y, 0);
            player.setPos(newX, py, newZ);
        }
    }
}