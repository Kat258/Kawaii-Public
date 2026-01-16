package dev.kizuna.api.utils.world;

import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.Wrapper;
import dev.kizuna.api.utils.entity.EntityUtil;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.impl.client.AntiCheat;
import dev.kizuna.mod.modules.impl.client.ClientSetting;
import dev.kizuna.mod.modules.impl.combat.KawaiiAura;
import dev.kizuna.mod.modules.impl.combat.WebAura;
import dev.kizuna.mod.modules.impl.player.PacketMine;
import dev.kizuna.mod.modules.settings.Placement;
import dev.kizuna.mod.modules.settings.SwingSide;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockUtil implements Wrapper {
    private static long tileEntitiesCacheTimeMs = 0L;
    private static int tileEntitiesCacheChunkX = Integer.MIN_VALUE;
    private static int tileEntitiesCacheChunkZ = Integer.MIN_VALUE;
    private static int tileEntitiesCacheViewDistance = -1;
    private static ArrayList<BlockEntity> tileEntitiesCache = new ArrayList<>();

    public static final List<Block> shiftBlocks = Arrays.asList(
            Blocks.ENDER_CHEST, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.CRAFTING_TABLE,
            Blocks.BIRCH_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.CHERRY_TRAPDOOR,
            Blocks.ANVIL, Blocks.BREWING_STAND, Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER,
            Blocks.ACACIA_TRAPDOOR, Blocks.ENCHANTING_TABLE, Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX,
            Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX
    );

    public static boolean isAir(BlockPos pos) {
        return mc.world.isAir(pos);
    }
    public static boolean canPlace(BlockPos pos) {
        return canPlace(pos, 1000);
    }
    public static BlockState getState(BlockPos pos) {
        return mc.world.getBlockState(pos);
    }

    public static boolean canPlace(BlockPos pos, double distance) {
        if (getPlaceSide(pos, distance) == null) return false;
        if (!canReplace(pos)) return false;
        return !hasEntity(pos, false);
    }

    public static boolean canPlace(BlockPos pos, double distance, boolean ignoreCrystal) {
        if (getPlaceSide(pos, distance) == null) return false;
        if (!canReplace(pos)) return false;
        return !hasEntity(pos, ignoreCrystal);
    }

    public static boolean clientCanPlace(BlockPos pos) {
        return clientCanPlace(pos, false);
    }
    public static boolean clientCanPlace(BlockPos pos, boolean ignoreCrystal) {
        if (!canReplace(pos)) return false;
        return !hasEntity(pos, ignoreCrystal);
    }

    public static List<Entity> getEntities(Box box) {
        return mc.world.getOtherEntities(null, box);
    }

    public static List<EndCrystalEntity> getEndCrystals(Box box) {
        return mc.world.getEntitiesByClass(EndCrystalEntity.class, box, entity -> true);
    }
    public static boolean hasEntity(BlockPos pos, boolean ignoreCrystal) {
        for (Entity entity : getEntities(new Box(pos))) {
            if (!entity.isAlive() || entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof ExperienceBottleEntity || entity instanceof ArrowEntity || ignoreCrystal && entity instanceof EndCrystalEntity || entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.obsMode.getValue())
                continue;
            return true;
        }
        return false;
    }

    public static boolean hasCrystal(BlockPos pos) {
        for (Entity entity : getEndCrystals(new Box(pos))) {
            if (!entity.isAlive() || !(entity instanceof EndCrystalEntity))
                continue;
            return true;
        }
        return false;
    }

    public static boolean hasEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal) {
        for (Entity entity : getEntities(new Box(pos))) {
            if (!entity.isAlive() || ignoreCrystal && entity instanceof EndCrystalEntity || entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.obsMode.getValue())
                continue;
            return true;
        }
        return false;
    }

    public static boolean hasEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        for (Entity entity : getEntities(new Box(pos))) {
            if (!entity.isAlive() || ignoreItem && entity instanceof ItemEntity || ignoreCrystal && entity instanceof EndCrystalEntity || entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.obsMode.getValue())
                continue;
            return true;
        }
        return false;
    }


    public static Direction getBestNeighboring(BlockPos pos, Direction facing) {
        for (Direction i : Direction.values()) {
            if (facing != null && pos.offset(i).equals(pos.offset(facing, -1)) || i == Direction.DOWN) continue;
            if (getPlaceSide(pos, false, true) != null) return i;
        }
        Direction bestFacing = null;
        double distance = 0;
        for (Direction i : Direction.values()) {
            if (facing != null && pos.offset(i).equals(pos.offset(facing, -1)) || i == Direction.DOWN) continue;
            if (getPlaceSide(pos) != null) {
                if (bestFacing == null || mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos()) < distance) {
                    bestFacing = i;
                    distance = mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos());
                }
            }
        }
        return bestFacing;
    }
    public static boolean canPlaceCrystal(BlockPos pos) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN)
                && getClickSideStrict(obsPos) != null
                && (mc.world.isAir(boost))
                && !hasEntityBlockCrystal(boost, false)
                && !hasEntityBlockCrystal(boost.up(), false)
                && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost.up()));
    }
    public static void placeCrystal(BlockPos pos, boolean rotate) {
        boolean offhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
        BlockPos obsPos = pos.down();
        Direction facing = getClickSide(obsPos);
        Vec3d vec = obsPos.toCenterPos().add(facing.getVector().getX() * 0.5,facing.getVector().getY() * 0.5,facing.getVector().getZ() * 0.5);
        if (rotate) {
            Kawaii.ROTATION.lookAt(vec);
        }
        clickBlock(obsPos, facing, false, offhand ? Hand.OFF_HAND : Hand.MAIN_HAND);
    }
    public static final CopyOnWriteArrayList<BlockPos> placedPos = new CopyOnWriteArrayList<>();

    public static void placeBlock(BlockPos pos, boolean rotate) {
        placeBlock(pos, rotate, AntiCheat.INSTANCE.packetPlace.getValue());
    }

    public static void placeBlock(BlockPos pos, boolean rotate, boolean packet) {
        if (airPlace()) {
            placedPos.add(pos);
            clickBlock(pos, Direction.DOWN, rotate, Hand.MAIN_HAND, packet);
            return;
        }
        Direction side = getPlaceSide(pos);
        if (side == null) return;
        placedPos.add(pos);
        clickBlock(pos.offset(side), side.getOpposite(), rotate, Hand.MAIN_HAND, packet);
    }

    public static void clickBlock(BlockPos pos, Direction side, boolean rotate) {
        clickBlock(pos, side, rotate, Hand.MAIN_HAND);
    }

    public static void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand) {
        clickBlock(pos, side, rotate, hand, AntiCheat.INSTANCE.packetPlace.getValue());
    }

    public static void clickBlock(BlockPos pos, Direction side, boolean rotate, boolean packet) {
        clickBlock(pos, side, rotate, Hand.MAIN_HAND, packet);
    }

    public static void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, boolean packet) {
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
        if (rotate) {
            Kawaii.ROTATION.lookAt(directionVec);
        }
        EntityUtil.swingHand(hand, AntiCheat.INSTANCE.swingMode.getValue());
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        if (packet) {
            Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, result, id));
        } else {
            mc.interactionManager.interactBlock(mc.player, hand, result);
        }
        if (rotate && AntiCheat.INSTANCE.snapBack.getValue()) {
            Kawaii.ROTATION.snapBack();
        }
    }

    public static void clickBlock(BlockPos pos, Direction side, boolean rotate, Hand hand, SwingSide swingSide) {
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
        if (rotate) {
            Kawaii.ROTATION.lookAt(directionVec);
        }
        EntityUtil.swingHand(hand, swingSide);
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, result, id));
        if (rotate && AntiCheat.INSTANCE.snapBack.getValue()) {
            Kawaii.ROTATION.snapBack();
        }
    }

    public static Direction getPlaceSide(BlockPos pos) {
        return getPlaceSide(pos, AntiCheat.INSTANCE.placement.getValue() == Placement.Strict, AntiCheat.INSTANCE.placement.getValue() == Placement.Legit);
    }

    public static Direction getPlaceSide(BlockPos pos, boolean strict, boolean legit) {
        if (pos == null) return null;
        double dis = 114514;
        Direction side = null;
        for (Direction i : Direction.values()) {
            if (canClick(pos.offset(i)) && !canReplace(pos.offset(i))) {
                if (legit) {
                    if (!EntityUtil.canSee(pos.offset(i), i.getOpposite())) continue;
                }
                if (strict) {
                    if (!isStrictDirection(pos.offset(i), i.getOpposite())) continue;
                }
                double vecDis = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos().add(i.getVector().getX() * 0.5, i.getVector().getY() * 0.5, i.getVector().getZ() * 0.5));
                if (side == null || vecDis < dis) {
                    side = i;
                    dis = vecDis;
                }
            }
        }
        if (airPlace()) return Direction.DOWN;
        return side;
    }

    public static double distanceToXZ(final double x, final double z, double x2, double z2) {
        final double dx = x2 - x;
        final double dz = z2 - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double distanceToXZ(final double x, final double z) {
        return distanceToXZ(x, z, mc.player.getX(), mc.player.getZ());
    }
    public static Direction getPlaceSide(BlockPos pos, double distance) {
        if (airPlace()) return Direction.DOWN;
        double dis = 114514;
        Direction side = null;
        for (Direction i : Direction.values()) {
            if (canClick(pos.offset(i)) && !canReplace(pos.offset(i))) {
                if (AntiCheat.INSTANCE.placement.getValue() == Placement.Legit) {
                    if (!EntityUtil.canSee(pos.offset(i), i.getOpposite())) continue;
                } else if (AntiCheat.INSTANCE.placement.getValue() == Placement.Strict) {
                    if (!isStrictDirection(pos.offset(i), i.getOpposite())) continue;
                }
                double vecDis = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos().add(i.getVector().getX() * 0.5, i.getVector().getY() * 0.5, i.getVector().getZ() * 0.5));
                if (MathHelper.sqrt((float) vecDis) > distance) {
                    continue;
                }
                if (side == null || vecDis < dis) {
                    side = i;
                    dis = vecDis;
                }
            }
        }
        return side;
    }

    public static Direction getClickSide(BlockPos pos) {
        Direction side = null;
        double range = 100;
        for (Direction i : Direction.values()) {
            if (!EntityUtil.canSee(pos, i)) continue;
            if (MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range) continue;
            side = i;
            range = MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos()));
        }
        if (side != null)
            return side;
        side = Direction.UP;
        for (Direction i : Direction.values()) {
            if (AntiCheat.INSTANCE.placement.getValue() == Placement.Strict) {
                if (!isStrictDirection(pos, i)) continue;
                if (AntiCheat.INSTANCE.blockCheck.getValue() && !mc.world.isAir(pos.offset(i))) continue;
            }
            if (MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range) continue;
            side = i;
            range = MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos()));
        }
        return side;
    }

    public static Direction getClickSideStrict(BlockPos pos) {
        Direction side = null;
        double range = 100;
        for (Direction i : Direction.values()) {
            if (!EntityUtil.canSee(pos, i)) continue;
            if (MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range) continue;
            side = i;
            range = MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos()));
        }
        if (side != null)
            return side;
        side = null;
        for (Direction i : Direction.values()) {
            if (AntiCheat.INSTANCE.placement.getValue() == Placement.Strict) {
                if (!isStrictDirection(pos, i)) continue;
                if (AntiCheat.INSTANCE.blockCheck.getValue() && !mc.world.isAir(pos.offset(i))) continue;
            }
            if (MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range) continue;
            side = i;
            range = MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos()));
        }
        return side;
    }

    public static boolean isStrictDirection(BlockPos pos, Direction side) {
        if (mc.player.getBlockY() - pos.getY() >= 0 && side == Direction.DOWN) return false;
        if (!AntiCheat.INSTANCE.oldNCP.getValue()) {
            if (side == Direction.UP && pos.getY() + 1 > mc.player.getEyePos().getY()) {
                return false;
            }
        } else {
            if (side == Direction.UP && pos.getY() > mc.player.getEyePos().getY()) {
                return false;
            }
        }

        if (AntiCheat.INSTANCE.blockCheck.getValue() && (getBlock(pos.offset(side)) == Blocks.OBSIDIAN || getBlock(pos.offset(side)) == Blocks.BEDROCK || getBlock(pos.offset(side)) == Blocks.RESPAWN_ANCHOR)) return false;
        Vec3d eyePos = EntityUtil.getEyesPos();
        Vec3d blockCenter = pos.toCenterPos();
        ArrayList<Direction> validAxis = new ArrayList<>();
        validAxis.addAll(checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, false));
        validAxis.addAll(checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true));
        validAxis.addAll(checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, false));
        return validAxis.contains(side);
    }

    public static ArrayList<Direction> checkAxis(double diff, Direction negativeSide, Direction positiveSide, boolean bothIfInRange) {
        ArrayList<Direction> valid = new ArrayList<>();
        if (diff < -0.5) {
            valid.add(negativeSide);
        }
        if (diff > 0.5) {
            valid.add(positiveSide);
        }
        if (bothIfInRange) {
            if (!valid.contains(negativeSide)) valid.add(negativeSide);
            if (!valid.contains(positiveSide)) valid.add(positiveSide);
        }
        return valid;
    }

    public static ArrayList<BlockEntity> getTileEntities(){
        if (mc.player == null || mc.world == null) return new ArrayList<>();

        int viewDistance = mc.options.getClampedViewDistance();
        ChunkPos center = mc.player.getChunkPos();
        long now = System.currentTimeMillis();

        if (now - tileEntitiesCacheTimeMs < 250L
                && tileEntitiesCacheChunkX == center.x
                && tileEntitiesCacheChunkZ == center.z
                && tileEntitiesCacheViewDistance == viewDistance) {
            return tileEntitiesCache;
        }

        int radius = Math.max(2, viewDistance) + 3;
        int minX = center.x - radius;
        int maxX = center.x + radius;
        int minZ = center.z - radius;
        int maxZ = center.z + radius;

        ArrayList<BlockEntity> result = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!mc.world.isChunkLoaded(x, z)) continue;
                WorldChunk chunk = mc.world.getChunk(x, z);
                if (chunk == null) continue;
                result.addAll(chunk.getBlockEntities().values());
            }
        }

        tileEntitiesCache = result;
        tileEntitiesCacheTimeMs = now;
        tileEntitiesCacheChunkX = center.x;
        tileEntitiesCacheChunkZ = center.z;
        tileEntitiesCacheViewDistance = viewDistance;
        return tileEntitiesCache;
    }

    public static Stream<WorldChunk> getLoadedChunks(){
        int radius = Math.max(2, mc.options.getClampedViewDistance()) + 3;
        int diameter = radius * 2 + 1;

        ChunkPos center = mc.player.getChunkPos();
        ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
        ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);

        return Stream.iterate(min, pos -> {
                    int x = pos.x;
                    int z = pos.z;
                    x++;

                    if(x > max.x)
                    {
                        x = min.x;
                        z++;
                    }

                    return new ChunkPos(x, z);

                }).limit((long) diameter *diameter)
                .filter(c -> mc.world.isChunkLoaded(c.x, c.z))
                .map(c -> mc.world.getChunk(c.x, c.z)).filter(Objects::nonNull);
    }

    public static ArrayList<BlockPos> getSphere(float range) {
        return getSphere(range, mc.player.getEyePos());
    }
    public static ArrayList<BlockPos> getSphere(float range, Vec3d pos) {
        if (range <= 0) return new ArrayList<>();

        double px = pos.getX();
        double py = pos.getY();
        double pz = pos.getZ();
        double rangeSq = (double) range * (double) range;

        int minX = MathHelper.floor(px - range);
        int maxX = MathHelper.ceil(px + range);
        int minY = MathHelper.floor(py - range);
        int maxY = MathHelper.ceil(py + range);
        int minZ = MathHelper.floor(pz - range);
        int maxZ = MathHelper.ceil(pz + range);

        int estimated = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        ArrayList<BlockPos> list = new ArrayList<>(Math.max(estimated / 3, 16));

        for (int x = minX; x <= maxX; x++) {
            double dx = (x + 0.5) - px;
            double dxSq = dx * dx;
            for (int z = minZ; z <= maxZ; z++) {
                double dz = (z + 0.5) - pz;
                double dzSq = dz * dz;
                double dxzSq = dxSq + dzSq;
                if (dxzSq > rangeSq) continue;
                for (int y = minY; y <= maxY; y++) {
                    double dy = (y + 0.5) - py;
                    if (dxzSq + dy * dy > rangeSq) continue;
                    list.add(new BlockPos(x, y, z));
                }
            }
        }

        return list;
    }

    public static Block getBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }

    public static boolean canReplace(BlockPos pos) {
        if (pos.getY() >= 320) return false;
        if (AntiCheat.INSTANCE.multiPlace.getValue() && placedPos.contains(pos)) {
            return true;
        }
        if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
            if (WebAura.ignore && KawaiiAura.INSTANCE.replace.getValue()) return true;
        }
        return mc.world.getBlockState(pos).isReplaceable();
    }

    public static boolean canClick(BlockPos pos) {
        if (AntiCheat.INSTANCE.multiPlace.getValue() && placedPos.contains(pos)) {
            return true;
        }
        if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
            if (WebAura.ignore) {
                return KawaiiAura.INSTANCE.airPlace.getValue();
            }
        }
        return mc.world.getBlockState(pos).isSolid() && (!(shiftBlocks.contains(getBlock(pos)) || getBlock(pos) instanceof BedBlock) || mc.player.isSneaking());
    }

    public static boolean airPlace() {
        return AntiCheat.INSTANCE.placement.getValue() == Placement.AirPlace;
    }

    public static boolean isMining(BlockPos pos) {
        return Kawaii.BREAK.isMining(pos) || pos.equals(PacketMine.breakPos);
    }
}
