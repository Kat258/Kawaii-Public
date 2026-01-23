package dev.kizuna.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import javax.imageio.ImageIO;
import dev.kizuna.api.utils.Wrapper;
import dev.kizuna.api.utils.render.GaussianFilter;
import dev.kizuna.api.utils.render.shaders.BlurShader;
import dev.kizuna.api.utils.render.shaders.MainMenuShader;
import dev.kizuna.api.utils.render.shaders.MainMenuShader2;
import dev.kizuna.api.utils.render.shaders.RectShader;
import dev.kizuna.mod.gui.font.Texture;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.RandomStringUtils;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

public class RenderShadersUtil implements Wrapper {
    public static MainMenuShader MAIN_MENU;
    public static MainMenuShader2 MAIN_MENU2;
    public static RectShader RECT;
    public static BlurShader BLUR;
    public static HashMap<Integer, BlurredShadow> shadowCache;

    public static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c1) {
        BufferBuilder bb = RenderShadersUtil.preShaderDraw(matrices, x - 10.0f, y - 10.0f, width + 20.0f, height + 20.0f);
        RECT.setParameters(x, y, width, height, radius, c1);
        RECT.use();
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderShadersUtil.endRender();
    }

    public static void drawRoundedBlur(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c1, float blurStrenth, float blurOpacity) {
        BufferBuilder bb = RenderShadersUtil.preShaderDraw(matrices, x - 10.0f, y - 10.0f, width + 20.0f, height + 20.0f);
        BLUR.setParameters(x, y, width, height, radius, c1, blurStrenth, blurOpacity);
        BLUR.use();
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderShadersUtil.endRender();
    }

    public static void drawMainMenuShader(MatrixStack matrices, float x, float y, float width, float height) {
        BufferBuilder bb = RenderShadersUtil.preShaderDraw(matrices, x, y, width, height);
        MAIN_MENU.setParameters(x, y, width, height);
        MAIN_MENU.use();
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderShadersUtil.endRender();
    }

    public static void drawMainMenuShader2(MatrixStack matrices, float x, float y, float width, float height) {
        BufferBuilder bb = RenderShadersUtil.preShaderDraw(matrices, x, y, width, height);
        MAIN_MENU2.setParameters(x, y, width, height);
        MAIN_MENU2.use();
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderShadersUtil.endRender();
    }

    public static BufferBuilder preShaderDraw(MatrixStack matrices, float x, float y, float width, float height) {
        RenderShadersUtil.setupRender();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        RenderShadersUtil.setRectanglePoints(buffer, matrix, x, y, x + width, y + height);
        return buffer;
    }

    public static void setRectanglePoints(BufferBuilder buffer, Matrix4f matrix, float x, float y, float x1, float y1) {
        buffer.vertex(matrix, x, y, 0.0f);
        buffer.vertex(matrix, x, y1, 0.0f);
        buffer.vertex(matrix, x1, y1, 0.0f);
        buffer.vertex(matrix, x1, y, 0.0f);
    }

    public static void drawBlurredShadow(MatrixStack matrices, float x, float y, float width, float height, int blurRadius, Color color) {
        int identifier;
        x -= (float)blurRadius;
        y -= (float)blurRadius;
        if (!shadowCache.containsKey(identifier = (int)((width += (float)(blurRadius * 2)) * (height += (float)(blurRadius * 2)) + width * (float)blurRadius))) {
            BufferedImage original = new BufferedImage((int)width, (int)height, 2);
            Graphics g = original.getGraphics();
            g.setColor(new Color(-1));
            g.fillRect(blurRadius, blurRadius, (int)(width - (float)(blurRadius * 2)), (int)(height - (float)(blurRadius * 2)));
            g.dispose();
            GaussianFilter op = new GaussianFilter(blurRadius);
            BufferedImage blurred = op.filter(original, null);
            shadowCache.put(identifier, new BlurredShadow(blurred));
            return;
        }
        shadowCache.get(identifier).bind();
        RenderShadersUtil.setupRender();
        RenderSystem.setShaderColor((float)((float)color.getRed() / 255.0f), (float)((float)color.getGreen() / 255.0f), (float)((float)color.getBlue() / 255.0f), (float)((float)color.getAlpha() / 255.0f));
        RenderShadersUtil.renderTexture(matrices, x, y, width, height, 0.0f, 0.0f, width, height, width, height);
        RenderShadersUtil.endRender();
    }

    public static void renderTexture(MatrixStack matrices, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight) {
        double x1 = x0 + width;
        double y1 = y0 + height;
        double z = 0.0;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, (float)x0, (float)y1, (float)z).texture(u / (float)textureWidth, (v + (float)regionHeight) / (float)textureHeight);
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z).texture((u + (float)regionWidth) / (float)textureWidth, (v + (float)regionHeight) / (float)textureHeight);
        buffer.vertex(matrix, (float)x1, (float)y0, (float)z).texture((u + (float)regionWidth) / (float)textureWidth, v / (float)textureHeight);
        buffer.vertex(matrix, (float)x0, (float)y0, (float)z).texture(u / (float)textureWidth, (v + 0.0f) / (float)textureHeight);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void registerTexture(Texture i, byte[] content) {
        try {
            ByteBuffer data = BufferUtils.createByteBuffer((int)content.length).put(content);
            data.flip();
            NativeImageBackedTexture tex = new NativeImageBackedTexture(NativeImage.read((ByteBuffer)data));
            mc.execute(() -> mc.getTextureManager().registerTexture(i.getId(), (AbstractTexture)tex));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void registerBufferedImageTexture(Texture i, BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write((RenderedImage)bi, "png", baos);
            byte[] bytes = baos.toByteArray();
            RenderShadersUtil.registerTexture(i, bytes);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void endRender() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void init() {
        MAIN_MENU = new MainMenuShader();
        MAIN_MENU2 = new MainMenuShader2();
        RECT = new RectShader();
        BLUR = new BlurShader();
    }

    static {
        shadowCache = new HashMap();
    }

    public static class BlurredShadow {
        Texture id = new Texture("texture/remote/" + RandomStringUtils.randomAlphanumeric(16));

        public BlurredShadow(BufferedImage bufferedImage) {
            RenderShadersUtil.registerBufferedImageTexture(this.id, bufferedImage);
        }

        public void bind() {
            RenderSystem.setShaderTexture(0, this.id.getId());
        }
    }
}
