package dev.kizuna.mod.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.RenderCrystalEvent;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import dev.kizuna.mod.modules.Module;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

import java.awt.*;

public class chamslastest extends Module {
    public static chamslastest INSTANCE;

    public chamslastest() {
        super("ChamsLatest", Category.Render);
        setChinese("模型");
        INSTANCE = this;
    }

    public final BooleanSetting crystalChams = add(new BooleanSetting("crystalChams", true).setParent());
    public final ColorSetting crystalColor = add(new ColorSetting("crystalColor", new Color(0x6EACAFFD, true), crystalChams::isOpen));
    public final EnumSetting<CMode> crystalModes = add(new EnumSetting<>("Crystals", CMode.One, crystalChams::isOpen));
    public final ColorSetting core =
            add(new ColorSetting("Core", new Color(255, 255, 255, 255), crystalChams::isOpen).injectBoolean(true));
    public final ColorSetting outerFrame =
            add(new ColorSetting("OuterFrame", new Color(255, 255, 255, 255), crystalChams::isOpen).injectBoolean(true));
    public final ColorSetting innerFrame =
            add(new ColorSetting("InnerFrame", new Color(255, 255, 255, 255), crystalChams::isOpen).injectBoolean(true));
    public final BooleanSetting texture = add(new BooleanSetting("Texture", true, crystalChams::isOpen));
    public final SliderSetting scale = add(new SliderSetting("Scale", 1, 0, 3f, 0.01, crystalChams::isOpen));
    public final SliderSetting spinValue = add(new SliderSetting("SpinSpeed", 1f, 0, 3f, 0.01, crystalChams::isOpen));
    public final SliderSetting bounceHeight = add(new SliderSetting("BounceHeight", 1, 0, 3f, 0.01, crystalChams::isOpen));
    public final SliderSetting floatValue = add(new SliderSetting("BounceSpeed", 1f, 0, 3f, 0.01, crystalChams::isOpen));
    public final SliderSetting floatOffset = add(new SliderSetting("YOffset", 0f, -1, 1f, 0.01, crystalChams::isOpen));

    public enum CMode {One, Two}
    private static final Identifier BLANK = Identifier.of("minecraft", "textures/blank.png");
    private static final Identifier VANILLA_CRYSTAL = Identifier.of("minecraft", "textures/entity/end_crystal/end_crystal.png");
    private final Identifier crystalTexture2 = Identifier.of("minecraft", "textures/end_crystal2.png");
    @EventHandler
    public void onRenderCrystal(RenderCrystalEvent event) {
        if (!this.isOn() || !this.crystalChams.getValue()) {
            return;
        }
        renderCrystal(event.endCrystalEntity, event.f, event.g, event.matrixStack, event.i, event.core, event.frame);
    }
    
    public void renderCrystal(EndCrystalEntity endCrystalEntity, float f, float g, MatrixStack matrixStack, int i, ModelPart core, ModelPart frame) {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        VertexConsumerProvider.Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
        Identifier tex = !texture.getValue()
                ? BLANK
                : (crystalModes.getValue() == CMode.Two ? crystalTexture2 : VANILLA_CRYSTAL);
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(tex));
        
        matrixStack.push();
        
        // 使用bounceHeight和floatValue计算弹跳效果
        float bounce = (float) ((float) Math.sin(((float) endCrystalEntity.endCrystalAge + g) * floatValue.getValue() * 0.5f) * bounceHeight.getValue() * 0.1f);
        float h = EndCrystalEntityRenderer.getYOffset(endCrystalEntity, g) + bounce;
        
        // 使用spinValue计算旋转速度
        float j = (float) (((float) endCrystalEntity.endCrystalAge + g) * spinValue.getValue() * 3.0f);
        
        // 使用floatOffset调整Y轴偏移
        matrixStack.translate(0.0f, floatOffset.getValue(), 0.0f);
        
        // 应用scale设置
        float scaleValue = (float) scale.getValue();
        matrixStack.scale(scaleValue, scaleValue, scaleValue);
        
        matrixStack.push();
        
        // 渲染外框架，使用outerFrame颜色
        Color outerColor = this.outerFrame.getValue();
        
        matrixStack.scale(2.0f, 2.0f, 2.0f);
        matrixStack.translate(0.0f, -0.5f, 0.0f);
        int k = OverlayTexture.DEFAULT_UV;
        
        // 应用旋转
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
        matrixStack.translate(0.0f, 1.5f + h / 2.0f, 0.0f);
        
        // 渲染第一个框架
        matrixStack.multiply(new Quaternionf().setAngleAxis(1.0471976f, (float) Math.sin(0.7853981633974483), (float) Math.sin(0.7853981633974483), (float) Math.sin(0.7853981633974483)));
        if (this.outerFrame.booleanValue) {
            frame.render(matrixStack, buffer, i, k);
        }
        
        // 渲染内框架，使用innerFrame颜色
        Color innerColor = this.innerFrame.getValue();
        
        matrixStack.scale(0.875f, 0.875f, 0.875f);
        matrixStack.multiply(new Quaternionf().setAngleAxis(1.0471976f, (float) Math.sin(0.7853981633974483), 0.0f, (float) Math.sin(0.7853981633974483)));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
        if (this.innerFrame.booleanValue) {
            frame.render(matrixStack, buffer, i, k);
        }
        
        // 渲染核心，使用core颜色
        Color coreColor = this.core.getValue();
        
        matrixStack.scale(0.875f, 0.875f, 0.875f);
        matrixStack.multiply(new Quaternionf().setAngleAxis(1.0471976f, (float) Math.sin(0.7853981633974483), 0.0f, (float) Math.sin(0.7853981633974483)));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
        if (this.core.booleanValue) {
            core.render(matrixStack, buffer, i, k);
        }
        
        // 应用整体crystalColor作为叠加效果
        Color overlayColor = this.crystalColor.getValue();
        if (overlayColor.getAlpha() > 0) {
            frame.render(matrixStack, buffer, i, k);
        }
        
        matrixStack.pop();
        matrixStack.pop();
        vertexConsumers.draw();
        
        // 重置渲染状态
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }
}
