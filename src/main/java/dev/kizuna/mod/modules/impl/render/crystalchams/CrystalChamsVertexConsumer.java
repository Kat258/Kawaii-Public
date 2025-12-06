package dev.kizuna.mod.modules.impl.render.crystalchams;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import net.minecraft.util.math.Vec3d;
import java.awt.*;

/**
 * Adapted CrystalChamsVertexConsumer for Kawaii.
 * Draws quads immediately (fill) and lines (outline) when 4 vertices are collected.
 */
public class CrystalChamsVertexConsumer implements VertexConsumer {
    public static final CrystalChamsVertexConsumer INSTANCE = new CrystalChamsVertexConsumer();

    private final float[] xs = new float[4];
    private final float[] ys = new float[4];
    private final float[] zs = new float[4];
    public static Matrix4f matrix4f;
    public static Vec3d offset;
    public static Vec3d camera;
    public static Color fillColor;
    public static Color outlineColor;
    public static boolean fill;
    public static boolean outline;
    public static float width;

    private int i = 0;

    // Mapping expects vertex(Matrix4f, float, float, float)
    @Override
    public VertexConsumer vertex(Matrix4f mat, float x, float y, float z) {
        // store the incoming vertex coords relative to the model
        xs[i] = x;
        ys[i] = y;
        zs[i] = z;
        // keep matrix if provided
        matrix4f = mat;

        i++;

        if (i == 4) {
            // compute world positions relative to camera
            float[] wx = new float[4];
            float[] wy = new float[4];
            float[] wz = new float[4];
            for (int n = 0; n < 4; n++) {
                wx[n] = (float) (offset.getX() + xs[n] - camera.getX());
                wy[n] = (float) (offset.getY() + ys[n] - camera.getY());
                wz[n] = (float) (offset.getZ() + zs[n] - camera.getZ());
            }

            if (fill && fillColor != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableDepthTest();
                RenderSystem.setShader(GameRenderer::getPositionColorProgram);

                BufferBuilder buffer = Tessellator.getInstance().getBuffer();
                buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                for (int n = 0; n < 4; n++) {
                    buffer.vertex(matrix4f, wx[n], wy[n], wz[n]).color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), fillColor.getAlpha()).next();
                }

                BufferRenderer.drawWithGlobalProgram(buffer.end());
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
            }

            if (outline && outlineColor != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableDepthTest();
                RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
                RenderSystem.lineWidth(width);

                BufferBuilder buffer = Tessellator.getInstance().getBuffer();
                buffer.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

                // edges 0-1,1-2,2-3,3-0
                int[][] edges = {{0,1},{1,2},{2,3},{3,0}};
                for (int[] e : edges) {
                    int a = e[0];
                    int b = e[1];
                    buffer.vertex(matrix4f, wx[a], wy[a], wz[a]).color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), outlineColor.getAlpha()).next();
                    buffer.vertex(matrix4f, wx[b], wy[b], wz[b]).color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), outlineColor.getAlpha()).next();
                }

                BufferRenderer.drawWithGlobalProgram(buffer.end());
                RenderSystem.lineWidth(1.0f);
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
            }

            i = 0;
        }

        return this;
    }

    // Some mappings expect this variant as well; keep for compatibility (non-overriding helper)
    public VertexConsumer vertex(float x, float y, float z) {
        return vertex(matrix4f, x, y, z);
    }

    // Some mappings use double precision vertex parameters
    public VertexConsumer vertex(double x, double y, double z) {
        return vertex((float) x, (float) y, (float) z);
    }

    @Override
    public VertexConsumer color(float red, float green, float blue, float alpha) {
        return this;
    }

    @Override
    public VertexConsumer color(int color) {
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer normal(org.joml.Matrix3f normalMatrix, float x, float y, float z) {
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return this;
    }

    @Override
    public void fixedColor(int r, int g, int b, int a) {
        // no-op
    }

    @Override
    public void unfixColor() {
        // no-op
    }

    @Override
    public void next() {
        // no-op
    }
}
