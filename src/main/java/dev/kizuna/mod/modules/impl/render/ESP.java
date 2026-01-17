package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import dev.kizuna.api.utils.math.MathUtil;
import dev.kizuna.api.utils.render.Render3DUtil;
import dev.kizuna.api.utils.world.BlockUtil;
import dev.kizuna.asm.accessors.IEntity;
import dev.kizuna.mod.modules.Module;
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

public class ESP extends Module {
	private final ColorSetting item = add(new ColorSetting("Item", new Color(255, 255, 255, 100)).injectBoolean(true));
	private final ColorSetting player = add(new ColorSetting("Player", new Color(255, 255, 255, 100)).injectBoolean(true));
	private final ColorSetting chest = add(new ColorSetting("Chest", new Color(255, 255, 255, 100)).injectBoolean(false));
	private final ColorSetting enderChest = add(new ColorSetting("EnderChest", new Color(255, 100, 255, 100)).injectBoolean(false));
	private final ColorSetting shulkerBox = add(new ColorSetting("ShulkerBox", new Color(15, 255, 255, 100)).injectBoolean(false));
	public ESP() {
		super("ESP", Category.Render);
		setChinese("透视");
	}

    @Override
	public void onRender3D(MatrixStack matrixStack) {
		float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
		if (item.booleanValue || player.booleanValue) {
			double radius = (mc.options.getClampedViewDistance() + 1) * 16.0;
			double radiusSq = radius * radius;
			Box scanBox = mc.player.getBoundingBox().expand(radius);

			if (item.booleanValue) {
				Color color = this.item.getValue();
				for (ItemEntity entity : mc.world.getEntitiesByClass(ItemEntity.class, scanBox, Entity::isAlive)) {
					Render3DUtil.draw3DBox(matrixStack, ((IEntity) entity).getDimensions().getBoxAt(new Vec3d(
							MathUtil.interpolate(entity.lastRenderX, entity.getX(), tickDelta),
							MathUtil.interpolate(entity.lastRenderY, entity.getY(), tickDelta),
							MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), tickDelta)
					)), color, false, true);
				}
			}

			if (player.booleanValue) {
				Color color = this.player.getValue();
				for (PlayerEntity entity : mc.world.getPlayers()) {
					if (mc.player.squaredDistanceTo(entity) > radiusSq) continue;
					Render3DUtil.draw3DBox(matrixStack, ((IEntity) entity).getDimensions().getBoxAt(new Vec3d(
							MathUtil.interpolate(entity.lastRenderX, entity.getX(), tickDelta),
							MathUtil.interpolate(entity.lastRenderY, entity.getY(), tickDelta),
							MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), tickDelta)
					)).expand(0, 0.1, 0), color, false, true);
				}
			}
		}
		ArrayList<BlockEntity> blockEntities = BlockUtil.getTileEntities();
		for (BlockEntity blockEntity : blockEntities) {
			if (blockEntity instanceof ChestBlockEntity && chest.booleanValue) {
				Box box = new Box(blockEntity.getPos());
				Render3DUtil.draw3DBox(matrixStack, box, chest.getValue());
			} else if (blockEntity instanceof EnderChestBlockEntity && enderChest.booleanValue) {
				Box box = new Box(blockEntity.getPos());
				Render3DUtil.draw3DBox(matrixStack, box, enderChest.getValue());
			} else if (blockEntity instanceof ShulkerBoxBlockEntity && shulkerBox.booleanValue) {
				Box box = new Box(blockEntity.getPos());
				Render3DUtil.draw3DBox(matrixStack, box, shulkerBox.getValue());
			}
		}
	}
}
