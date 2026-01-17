package dev.kizuna.api.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.awt.*;

import static dev.kizuna.api.utils.Wrapper.mc;
import static dev.kizuna.api.utils.render.Render2DUtil.capture;

public class CaptureMark {

    private static float espValue = 1f, prevEspValue;
    private static float espSpeed = 1f;
    private static boolean flipSpeed;

    public static void render(Entity target, Color color) {
        Camera camera = mc.gameRenderer.getCamera();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        double tPosX = Render2DUtil.interpolate(target.prevX, target.getX(), tickDelta) - camera.getPos().x;
        double tPosY = Render2DUtil.interpolate(target.prevY, target.getY(), tickDelta) - camera.getPos().y;
        double tPosZ = Render2DUtil.interpolate(target.prevZ, target.getZ(), tickDelta) - camera.getPos().z;

        MatrixStack matrices = new MatrixStack();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrices.translate(tPosX, (tPosY + target.getEyeHeight(target.getPose()) / 2f), tPosZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(Render2DUtil.interpolateFloat(prevEspValue, espValue, tickDelta)));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, capture);
        matrices.translate(-0.75, -0.75, -0.01);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(matrix, 0, 1.5f, 0).texture(0f, 1f).color(color.getRGB());
        bufferBuilder.vertex(matrix, 1.5f, 1.5f, 0).texture(1f, 1f).color(color.getRGB());
        bufferBuilder.vertex(matrix, 1.5f, 0, 0).texture(1f, 0).color(color.getRGB());
        bufferBuilder.vertex(matrix, 0, 0, 0).texture(0, 0).color(color.getRGB());
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static void update() {
        prevEspValue = espValue;
        espValue += espSpeed;
        if (espSpeed > 25) flipSpeed = true;
        if (espSpeed < -25) flipSpeed = false;
        espSpeed = flipSpeed ? espSpeed - 0.5f : espSpeed + 0.5f;
    }
}
