package dev.kizuna.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.HeldItemRendererEvent;
import dev.kizuna.api.events.impl.RenderArmEvent;
import dev.kizuna.api.events.impl.RenderCrystalEvent;
import dev.kizuna.api.events.impl.RenderEntityEvent;
import dev.kizuna.api.utils.entity.EntityUtil;
import dev.kizuna.api.utils.render.Render2DUtil;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

import java.awt.*;

public class Chams extends Module {
    public static Chams INSTANCE;

    public final BooleanSetting hands = add(new BooleanSetting ("Hands", true));
    public final BooleanSetting items = add(new BooleanSetting ("Items", true));
    public final BooleanSetting self = add(new BooleanSetting("Self", false));
    public final BooleanSetting players = add(new BooleanSetting("Players", false));
    public final BooleanSetting monsters = add(new BooleanSetting("Monsters", false));
    public final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    public final BooleanSetting crystals = add(new BooleanSetting("Crystals", false));
    public final BooleanSetting invisibles = add(new BooleanSetting("Invisibles", true));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(0x59B380FF, true)));

    public final BooleanSetting throughWall = add(new BooleanSetting("ThroughWall", false).setParent());
    public final BooleanSetting playersB = add(new BooleanSetting("Player", true, throughWall::isOpen));
    public final BooleanSetting crystalsB = add(new BooleanSetting("Crystal", true, throughWall::isOpen));
    public final BooleanSetting itemsB = add(new BooleanSetting("Item", true, throughWall::isOpen));
    public final BooleanSetting monstersB = add(new BooleanSetting("Monster", false, throughWall::isOpen));
    public final BooleanSetting animalsB = add(new BooleanSetting("Animal", false, throughWall::isOpen));
    public final BooleanSetting slimesB = add(new BooleanSetting("Slime", false, throughWall::isOpen));
    public final BooleanSetting villagersB = add(new BooleanSetting("Villager", false, throughWall::isOpen));

    public boolean throughWall(Entity entity) {
        if (entity instanceof EndCrystalEntity) return crystalsB.getValue();
        if (entity instanceof SlimeEntity) return slimesB.getValue();
        if (entity instanceof PlayerEntity) return playersB.getValue();
        if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) return villagersB.getValue();
        if (entity instanceof AnimalEntity) return animalsB.getValue();
        if (entity instanceof MobEntity) return monstersB.getValue();
        if (entity instanceof ItemEntity) return itemsB.getValue();
        return false;
    }

    private static final float SINE_45_DEGREES = (float)Math.sin(0.7853981633974483);

    public Chams() {
        super("Chams", "Renders entity models through walls", Category.Render);
        setChinese("模型修改");
        INSTANCE = this;
    }

    private static float getYaw(Direction direction) {
        switch (direction) {
            case SOUTH: {
                return 90.0f;
            }
            case WEST: {
                return 0.0f;
            }
            case NORTH: {
                return 270.0f;
            }
            case EAST: {
                return 180.0f;
            }
        }
        return 0.0f;
    }

    @EventHandler
    public void onRenderEntity(RenderEntityEvent event) {
        float n;
        Direction direction;
        float l;
        if (!this.checkChams(event.entity)) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        Tessellator tessellator = Tessellator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        RenderSystem.lineWidth(2.0f);
        BufferBuilder vertexConsumer = tessellator.getBuffer(); // 获取 BufferBuilder 实例
        vertexConsumer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION); // 在 BufferBuilder 上调用 begin 方法
        Color color = this.color.getValue();
        event.matrixStack.push();
        RenderSystem.setShaderColor((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, (float)color.getAlpha() / 255.0f);
        event.model.handSwingProgress = event.entity.getHandSwingProgress(event.g);
        event.model.riding = event.entity.hasVehicle();
        event.model.child = event.entity.isBaby();
        float h = MathHelper.lerpAngleDegrees(event.g, event.entity.prevBodyYaw, event.entity.bodyYaw);
        float j = MathHelper.lerpAngleDegrees(event.g, event.entity.prevHeadYaw, event.entity.headYaw);
        float k = j - h;
        if (event.entity.hasVehicle() && event.entity.getVehicle() instanceof LivingEntity livingEntity2) {
            h = MathHelper.lerpAngleDegrees(event.g, livingEntity2.prevBodyYaw, livingEntity2.bodyYaw);
            k = j - h;
            l = MathHelper.wrapDegrees(k);
            if (l < -85.0f) {
                l = -85.0f;
            }
            if (l >= 85.0f) {
                l = 85.0f;
            }
            h = j - l;
            if (l * l > 2500.0f) {
                h += l * 0.2f;
            }
            k = j - h;
        }
        float m = MathHelper.lerp(event.g, event.entity.prevPitch, event.entity.getPitch());
        if (LivingEntityRenderer.shouldFlipUpsideDown(event.entity)) {
            m *= -1.0f;
            k *= -1.0f;
        }
        if (event.entity.isInPose(EntityPose.SLEEPING) && (direction = event.entity.getSleepingDirection()) != null) {
            n = event.entity.getEyeHeight(EntityPose.STANDING) - 0.1f;
            event.matrixStack.translate((float)(-direction.getOffsetX()) * n, 0.0f, (float)(-direction.getOffsetZ()) * n);
        }
        l = (float)event.entity.age + event.g;
        this.setupTransforms(event.entity, event.matrixStack, h, event.g);
        event.matrixStack.scale(-1.0f, -1.0f, 1.0f);
        event.matrixStack.scale(0.9375f, 0.9375f, 0.9375f);
        event.matrixStack.translate(0.0f, -1.501f, 0.0f);
        n = 0.0f;
        float o = 0.0f;
        if (!event.entity.hasVehicle() && event.entity.isAlive()) {
            n = event.entity.limbAnimator.getSpeed(event.g);
            o = event.entity.limbAnimator.getPos(event.g);
            if (event.entity.isBaby()) {
                o *= 3.0f;
            }
            if (n > 1.0f) {
                n = 1.0f;
            }
        }
        event.model.animateModel(event.entity, o, n, event.g);
        event.model.setAngles(event.entity, o, n, l, k, m);
        boolean bl = !event.entity.isInvisible();
        boolean bl2 = !bl && !event.entity.isInvisibleTo(mc.player);
        int p = LivingEntityRenderer.getOverlay(event.entity, 0.0f);
        Color color1 = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        float red = color1.getRed() / 255.0f;
        float green = color1.getGreen() / 255.0f;
        float blue = color1.getBlue() / 255.0f;

        event.model.render(event.matrixStack, vertexConsumer, event.i, p, red, green, blue, 1.0f);
        Render2DUtil.endBuilding(vertexConsumer);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        if (!event.entity.isSpectator()) {
            for (Object featureRenderer : event.features) {
                ((FeatureRenderer) featureRenderer).render(event.matrixStack, event.vertexConsumerProvider, event.i,
                        event.entity, o, n, event.g, l, k, m);
            }
        }
        event.matrixStack.pop();
        event.cancel();
    }

    @EventHandler
    public void onRenderCrystal(RenderCrystalEvent event) {
        if (!this.crystals.getValue()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        event.matrixStack.push();
        float h = EndCrystalEntityRenderer.getYOffset(event.endCrystalEntity, event.g);
        float j = ((float)event.endCrystalEntity.endCrystalAge + event.g) * 3.0f;
        Tessellator tessellator = Tessellator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        RenderSystem.lineWidth(2.0f);
        BufferBuilder vertexConsumer = tessellator.getBuffer(); // 获取 BufferBuilder 实例
        vertexConsumer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION); // 在 BufferBuilder 上调用 begin 方法
        event.matrixStack.push();
        Color color = this.color.getValue();
        RenderSystem.setShaderColor((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, (float)color.getAlpha() / 255.0f);
        event.matrixStack.scale(2.0f, 2.0f, 2.0f);
        event.matrixStack.translate(0.0f, -0.5f, 0.0f);
        int k = OverlayTexture.DEFAULT_UV;
        event.matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
        event.matrixStack.translate(0.0f, 1.5f + h / 2.0f, 0.0f);
        event.matrixStack.multiply(new Quaternionf().setAngleAxis(1.0471976f, SINE_45_DEGREES, 0.0f, SINE_45_DEGREES));
        event.frame.render(event.matrixStack, vertexConsumer, event.i, k);
        float l = 0.875f;
        event.matrixStack.scale(0.875f, 0.875f, 0.875f);
        event.matrixStack.multiply(new Quaternionf().setAngleAxis(1.0471976f, SINE_45_DEGREES, 0.0f, SINE_45_DEGREES));
        event.matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
        event.frame.render(event.matrixStack, vertexConsumer, event.i, k);
        event.matrixStack.scale(0.875f, 0.875f, 0.875f);
        event.matrixStack.multiply(new Quaternionf().setAngleAxis(1.0471976f, SINE_45_DEGREES, 0.0f, SINE_45_DEGREES));
        event.matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
        event.core.render(event.matrixStack, vertexConsumer, event.i, k);
        event.matrixStack.pop();
        event.matrixStack.pop();
        Render2DUtil.endBuilding(vertexConsumer);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        event.cancel();
    }

    protected void setupTransforms(LivingEntity entity, MatrixStack matrices, float bodyYaw, float tickDelta) {
        if (entity.isFrozen()) {
            bodyYaw += (float)(Math.cos((double)entity.age * 3.25) * Math.PI * (double)0.4f);
        }
        if (!entity.isInPose(EntityPose.SLEEPING)) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        }
        if (entity.deathTime > 0) {
            float f = 0f;
            float f2 = ((float)entity.deathTime + tickDelta - 1.0f) / 20.0f * 1.6f;
            f2 = MathHelper.sqrt(f2);
            if (f > 1.0f) {
                f2 = 1.0f;
            }
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f2 * 90.0f));
        } else if (entity.isUsingRiptide()) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f - entity.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((float)entity.age + tickDelta) * -75.0f));
        } else if (entity.isInPose(EntityPose.SLEEPING)) {
            Direction direction = entity.getSleepingDirection();
            float g = direction != null ? getYaw(direction) : bodyYaw;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270.0f));
        } else if (LivingEntityRenderer.shouldFlipUpsideDown(entity)) {
            matrices.translate(0.0f, entity.getHeight() + 0.1f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        }
    }

    @EventHandler
    public void onRenderArm(RenderArmEvent event) {
        if (this.hands.getValue()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Tessellator tessellator = Tessellator.getInstance();
            RenderSystem.setShader(GameRenderer::getPositionProgram);
            RenderSystem.lineWidth(2.0f);
            BufferBuilder vertexConsumer = tessellator.getBuffer(); // 获取 BufferBuilder 实例
            vertexConsumer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION); // 在 BufferBuilder 上调用 begin 方法
            event.matrices.push();
            Color color = this.color.getValue();
            RenderSystem.setShaderColor((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, MathHelper.clamp(((float)color.getAlpha() + 40.0f) / 255.0f, 0.0f, 1.0f));
            boolean bl = event.arm != Arm.LEFT;
            float f = bl ? 1.0f : -1.0f;
            float g = MathHelper.sqrt(event.swingProgress);
            float h = -0.3f * MathHelper.sin(g * (float)Math.PI);
            float i = 0.4f * MathHelper.sin(g * ((float)Math.PI * 2));
            float j = -0.4f * MathHelper.sin(event.swingProgress * (float)Math.PI);
            event.matrices.translate(f * (h + 0.64000005f), i + -0.6f + event.equipProgress * -0.6f, j + -0.71999997f);
            event.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * 45.0f));
            float k = MathHelper.sin(event.swingProgress * event.swingProgress * (float)Math.PI);
            float l = MathHelper.sin(g * (float)Math.PI);
            event.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * l * 70.0f));
            event.matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * k * -20.0f));
            event.matrices.translate(f * -1.0f, 3.6f, 3.5f);
            event.matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 120.0f));
            event.matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200.0f));
            event.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * -135.0f));
            event.matrices.translate(f * 5.6f, 0.0f, 0.0f);
//            event.playerEntityRenderer.setModelPose(mc.player);
            event.playerEntityRenderer.getModel().handSwingProgress = 0.0f;
            event.playerEntityRenderer.getModel().sneaking = false;
            event.playerEntityRenderer.getModel().leaningPitch = 0.0f;
            event.playerEntityRenderer.getModel().setAngles(mc.player, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
            if (event.arm == Arm.RIGHT) {
                event.playerEntityRenderer.getModel().rightArm.pitch = 0.0f;
                event.playerEntityRenderer.getModel().rightArm.render(event.matrices, vertexConsumer, event.light, OverlayTexture.DEFAULT_UV);
                event.playerEntityRenderer.getModel().rightSleeve.pitch = 0.0f;
                event.playerEntityRenderer.getModel().rightSleeve.render(event.matrices, vertexConsumer, event.light, OverlayTexture.DEFAULT_UV);
            } else {
                event.playerEntityRenderer.getModel().leftArm.pitch = 0.0f;
                event.playerEntityRenderer.getModel().leftArm.render(event.matrices, vertexConsumer, event.light, OverlayTexture.DEFAULT_UV);
                event.playerEntityRenderer.getModel().leftSleeve.pitch = 0.0f;
                event.playerEntityRenderer.getModel().leftSleeve.render(event.matrices, vertexConsumer, event.light, OverlayTexture.DEFAULT_UV);
            }
            Render2DUtil.endBuilding(vertexConsumer);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            event.matrices.pop();
            event.cancel();
        }
    }

    private boolean checkChams(LivingEntity entity) {
        if (entity instanceof PlayerEntity && this.players.getValue()) {
            return this.self.getValue() || entity != mc.player;
        }
        return (!entity.isInvisible() || this.invisibles.getValue()) && (EntityUtil.isMonster(entity) && this.monsters.getValue() || (EntityUtil.isNeutral(entity) || EntityUtil.isPassive(entity)) && this.animals.getValue());
    }

    @EventHandler
    public void onRenderHands(HeldItemRendererEvent e) {
        if (items.getValue()) {
            RenderSystem.setShaderColor(color.getValue().getRed() / 255f, color.getValue().getGreen() / 255f, color.getValue().getBlue() / 255f, color.getValue().getAlpha() / 255f);
        }
    }
}
