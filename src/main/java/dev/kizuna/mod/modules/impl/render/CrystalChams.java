package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import dev.kizuna.mod.modules.Module;

import java.awt.*;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.joml.Matrix4f;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import dev.kizuna.api.utils.render.Render3DUtil;
import dev.kizuna.mod.modules.impl.render.crystalchams.CrystalChamsVertexConsumer;

public class CrystalChams extends Module {
    public static CrystalChams INSTANCE;
    public CrystalChams() {
        super("CrystalChams", Category.Render);
        setChinese("末影水晶模型");
        INSTANCE = this;
    }

    public final ColorSetting core =
            add(new ColorSetting("Core", new Color(255, 255, 255, 255)).injectBoolean(true));
    public final ColorSetting outerFrame =
            add(new ColorSetting("OuterFrame", new Color(255, 255, 255, 255)).injectBoolean(true));
    public final ColorSetting innerFrame =
            add(new ColorSetting("InnerFrame", new Color(255, 255, 255, 255)).injectBoolean(true));
    public final BooleanSetting texture = add(new BooleanSetting("Texture", true));
    public final SliderSetting scale = add(new SliderSetting("Scale", 1, 0, 3f, 0.01));
    public final SliderSetting spinValue = add(new SliderSetting("SpinSpeed", 1f, 0, 3f, 0.01));
    public final SliderSetting bounceHeight = add(new SliderSetting("BounceHeight", 1, 0, 3f, 0.01));
    public final SliderSetting floatValue = add(new SliderSetting("BounceSpeed", 1f, 0, 3f, 0.01));
    public final SliderSetting floatOffset = add(new SliderSetting("YOffset", 0f, -1, 1f, 0.01));
        // Penumbra-like settings
        public final BooleanSetting fill = add(new BooleanSetting("Fill", true));
        public final ColorSetting fillColor = add(new ColorSetting("Fill Color", new Color(255, 0, 0, 70), () -> fill.getValue()));
        public final BooleanSetting outline = add(new BooleanSetting("Outline", true));
        public final ColorSetting outlineColor = add(new ColorSetting("Outline Color", new Color(255, 0, 0, 255), () -> outline.getValue()));
        public final SliderSetting outlineWidth = add(new SliderSetting("OutlineWidth", 0.1f, 1f, 10f, 0.1f, () -> outline.getValue()));

        @Override
        public void onRender3D(MatrixStack matrixStack) {
                if (nullCheck()) return;

                Tessellator tessellator = Tessellator.getInstance();

                CrystalChamsVertexConsumer.fillColor = fillColor.getValue();
                CrystalChamsVertexConsumer.outlineColor = outlineColor.getValue();
                CrystalChamsVertexConsumer.width = outlineWidth.getValueFloat();
                CrystalChamsVertexConsumer.fill = fill.getValue();
                CrystalChamsVertexConsumer.outline = outline.getValue();

                double radius = (mc.options.getClampedViewDistance() + 1) * 16.0;
                Box scanBox = mc.player.getBoundingBox().expand(radius);
                for (EndCrystalEntity crystalEntity : mc.world.getEntitiesByClass(EndCrystalEntity.class, scanBox, Entity::isAlive)) {
                        if (!Render3DUtil.isFrustumVisible(crystalEntity.getBoundingBox())) continue;
                        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(crystalEntity);
                        if (renderer instanceof EndCrystalEntityRenderer crystalRenderer) {
                                CrystalChamsVertexConsumer.matrix4f = matrixStack.peek().getPositionMatrix();
                                CrystalChamsVertexConsumer.offset = new Vec3d(crystalEntity.getX(), crystalEntity.getY(), crystalEntity.getZ());
                                CrystalChamsVertexConsumer.camera = mc.gameRenderer.getCamera().getPos();
                                float yaw = 0f;
                                float tickDelta = mc.getTickDelta();
                                int light = 15728880;
                                crystalRenderer.render(crystalEntity, yaw, tickDelta, matrixStack, CustomVertexConsumerProvider.INSTANCE, light);
                        }
                }

        }

        private static class CustomVertexConsumerProvider implements VertexConsumerProvider {
                public static final CustomVertexConsumerProvider INSTANCE = new CustomVertexConsumerProvider();

                @Override
                public VertexConsumer getBuffer(net.minecraft.client.render.RenderLayer layer) {
                        return CrystalChamsVertexConsumer.INSTANCE;
                }
        }
}
