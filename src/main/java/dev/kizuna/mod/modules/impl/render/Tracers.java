package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.render.Render3DUtil;
import dev.kizuna.api.utils.world.BlockUtil;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;

public class Tracers extends Module {
	private final ColorSetting item = add(new ColorSetting("Item", new Color(255, 255, 255, 100)).injectBoolean(true));
	private final ColorSetting player = add(new ColorSetting("Player", new Color(255, 255, 255, 100)).injectBoolean(true));
	private final ColorSetting chest = add(new ColorSetting("Chest", new Color(255, 255, 255, 100)).injectBoolean(false));
	private final ColorSetting enderChest = add(new ColorSetting("EnderChest", new Color(255, 100, 255, 100)).injectBoolean(false));
	private final ColorSetting shulkerBox = add(new ColorSetting("ShulkerBox", new Color(15, 255, 255, 100)).injectBoolean(false));
	public Tracers() {
		super("Tracers", Category.Render);
		setChinese("追踪者");
	}

	@Override
	public void onRender3D(MatrixStack matrixStack) {
		boolean prev_bob = mc.options.getBobView().getValue();
		mc.options.getBobView().setValue(false);
		float tickDelta = mc.getTickDelta();
		Vec3d start = mc.player.getCameraPosVec(tickDelta).add(Vec3d.fromPolar(mc.player.getPitch(tickDelta), mc.player.getYaw(tickDelta)).multiply(0.2));
		if (item.booleanValue || player.booleanValue) {
			double radius = (mc.options.getClampedViewDistance() + 1) * 16.0;
			double radiusSq = radius * radius;
			Box scanBox = mc.player.getBoundingBox().expand(radius);

			if (item.booleanValue) {
				Color color = item.getValue();
				for (ItemEntity entity : mc.world.getEntitiesByClass(ItemEntity.class, scanBox, Entity::isAlive)) {
					drawLine(entity.getPos(), start, color);
				}
			}

			if (player.booleanValue) {
				Color color = player.getValue();
				for (PlayerEntity entity : mc.world.getPlayers()) {
					if (entity == mc.player) continue;
					if (mc.player.squaredDistanceTo(entity) > radiusSq) continue;
					if (Kawaii.FRIEND.isFriend(entity)) continue;
					drawLine(entity.getPos(), start, color);
				}
			}
		}
		ArrayList<BlockEntity> blockEntities = BlockUtil.getTileEntities();
		for (BlockEntity blockEntity : blockEntities) {
			if (blockEntity instanceof ChestBlockEntity && chest.booleanValue) {
				drawLine(blockEntity.getPos().toCenterPos(), start, chest.getValue());
			} else if (blockEntity instanceof EnderChestBlockEntity && enderChest.booleanValue) {
				drawLine(blockEntity.getPos().toCenterPos(), start, enderChest.getValue());
			} else if (blockEntity instanceof ShulkerBoxBlockEntity && shulkerBox.booleanValue) {
				drawLine(blockEntity.getPos().toCenterPos(), start, shulkerBox.getValue());
			}
		}
		mc.options.getBobView().setValue(prev_bob);
	}


	private void drawLine(Vec3d pos, Vec3d start, Color color) {
		Render3DUtil.drawLine(pos, start, color);
	}
}
