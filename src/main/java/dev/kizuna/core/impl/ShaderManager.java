package dev.kizuna.core.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.kizuna.api.interfaces.IShaderEffect;
import dev.kizuna.api.utils.render.Render3DUtil;
import dev.kizuna.mod.modules.impl.client.Colors;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import dev.kizuna.api.utils.Wrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL30C;

import java.util.ArrayList;
import java.util.List;

public class ShaderManager implements Wrapper {

    private final static List<RenderTask> tasks = new ArrayList<>();
    private MyFramebuffer shaderBuffer;

    public float time = 0;

    public static ManagedShaderEffect DEFAULT_OUTLINE;
    public static ManagedShaderEffect SMOKE_OUTLINE;
    public static ManagedShaderEffect PULSE_OUTLINE;
    public static ManagedShaderEffect GRADIENT_OUTLINE;
    public static ManagedShaderEffect SNOW_OUTLINE;
    public static ManagedShaderEffect FLOW_OUTLINE;
    public static ManagedShaderEffect RAINBOW_OUTLINE;

    public static ManagedShaderEffect DEFAULT;
    public static ManagedShaderEffect SMOKE;
    public static ManagedShaderEffect GRADIENT;
    public static ManagedShaderEffect PULSE;
    public static ManagedShaderEffect SNOW;
    public static ManagedShaderEffect FLOW;
    public static ManagedShaderEffect RAINBOW;
    public void renderShader(Runnable runnable, Shader mode) {
        tasks.add(new RenderTask(runnable, mode));
    }
    public void renderShaders() {
        tasks.forEach(t -> applyShader(t.task(), t.shader()));
        tasks.clear();  
    }

    public void applyShader(Runnable runnable, Shader mode) {
        if (fullNullCheck()) return;
        Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
        RenderSystem.assertOnRenderThreadOrInit();
        if (shaderBuffer.textureWidth != MCBuffer.textureWidth || shaderBuffer.textureHeight != MCBuffer.textureHeight)
            shaderBuffer.resize(MCBuffer.textureWidth, MCBuffer.textureHeight, false);
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, shaderBuffer.fbo);
        shaderBuffer.beginWrite(true);
        runnable.run();
        shaderBuffer.endWrite();
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, MCBuffer.fbo);
        MCBuffer.beginWrite(false);
        ManagedShaderEffect shader = getShader(mode);
        Framebuffer mainBuffer = MinecraftClient.getInstance().getFramebuffer();
        PostEffectProcessor effect = shader.getShaderEffect();

        if (effect != null)
            ((IShaderEffect) effect).addHook("bufIn", shaderBuffer);

        Framebuffer outBuffer = shader.getShaderEffect().getSecondaryTarget("bufOut");
        setupShader(mode, shader);
        shaderBuffer.clear(false);
        mainBuffer.beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.backupProjectionMatrix();
        outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public ManagedShaderEffect getShader(@NotNull Shader mode) {
        return switch (mode) {
            case Gradient -> GRADIENT;
            case Smoke -> SMOKE;
            case Pulse -> PULSE;
            case Snow -> SNOW;
            case Flow -> FLOW;
            case Rainbow -> RAINBOW;
            default -> DEFAULT;
        };
    }

    public ManagedShaderEffect getShaderOutline(@NotNull Shader mode) {
        return switch (mode) {
            case Gradient -> GRADIENT_OUTLINE;
            case Smoke -> SMOKE_OUTLINE;
            case Pulse -> PULSE_OUTLINE;
            case Snow -> SNOW_OUTLINE;
            case Flow -> FLOW_OUTLINE;
            case Rainbow -> RAINBOW_OUTLINE;
            default -> DEFAULT_OUTLINE;
        };
    }

    public void setupShader(Shader shader, ManagedShaderEffect effect) {
        dev.kizuna.mod.modules.impl.render.Shader shaderChams = dev.kizuna.mod.modules.impl.render.Shader.INSTANCE;
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        if (shader == Shader.Rainbow) {
                effect.setUniformValue("alpha2", (shaderChams.fill.getValue().getAlpha() / 255f));
            effect.setUniformValue("radius", shaderChams.radius.getValueFloat());
            effect.setUniformValue("quality", shaderChams.smoothness.getValueFloat());
            effect.setUniformValue("divider", shaderChams.divider.getValueFloat());
            effect.setUniformValue("maxSample", shaderChams.maxSample.getValueFloat());

            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", time);
            effect.render(tickDelta);
            time += (float) shaderChams.speed.getValue() * 0.002f;
        } else if (shader == ShaderManager.Shader.Gradient) {
                effect.setUniformValue("alpha2", (shaderChams.fill.getValue().getAlpha() / 255f));
            effect.setUniformValue("oct", (int) shaderChams.octaves.getValue());

            effect.setUniformValue("radius", shaderChams.radius.getValueFloat());
            effect.setUniformValue("quality", shaderChams.smoothness.getValueFloat());
            effect.setUniformValue("divider", shaderChams.divider.getValueFloat());
            effect.setUniformValue("maxSample", shaderChams.maxSample.getValueFloat());

            effect.setUniformValue("factor", (float) shaderChams.factor.getValue());
            effect.setUniformValue("moreGradient", (float) shaderChams.gradient.getValue());

            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", time);
            effect.render(tickDelta);
            time += (float) shaderChams.speed.getValue() * 0.002f;
        } else if (shader == ShaderManager.Shader.Smoke) {
                effect.setUniformValue("alpha1", (shaderChams.fill.getValue().getAlpha() / 255f));
            effect.setUniformValue("radius", shaderChams.radius.getValueFloat());
            effect.setUniformValue("quality", shaderChams.smoothness.getValueFloat());
            effect.setUniformValue("divider", shaderChams.divider.getValueFloat());
            effect.setUniformValue("maxSample", shaderChams.maxSample.getValueFloat());

            effect.setUniformValue("first", shaderChams.smoke1.getValue().getRed() / 255f, shaderChams.smoke1.getValue().getGreen() / 255f, shaderChams.smoke1.getValue().getBlue() / 255f, shaderChams.smoke1.getValue().getAlpha() / 255f);
            effect.setUniformValue("second", shaderChams.smoke2.getValue().getRed() / 255f, shaderChams.smoke2.getValue().getGreen() / 255f, shaderChams.smoke2.getValue().getBlue() / 255f);
            effect.setUniformValue("third", shaderChams.smoke3.getValue().getRed() / 255f, shaderChams.smoke3.getValue().getGreen() / 255f, shaderChams.smoke3.getValue().getBlue() / 255f);
            effect.setUniformValue("oct", (int) shaderChams.octaves.getValue());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", time);
            effect.render(tickDelta);
            time += (float) shaderChams.speed.getValue() * 0.002f;
        } else if (shader == ShaderManager.Shader.Solid) {
                effect.setUniformValue("mixFactor", shaderChams.fill.getValue().getAlpha() / 255f);
            effect.setUniformValue("minAlpha", shaderChams.alpha.getValueFloat() / 255f);
            effect.setUniformValue("radius", shaderChams.radius.getValueFloat());
            effect.setUniformValue("quality", shaderChams.smoothness.getValueFloat());
            effect.setUniformValue("divider", shaderChams.divider.getValueFloat());
            effect.setUniformValue("maxSample", shaderChams.maxSample.getValueFloat());
            effect.setUniformValue("color", shaderChams.fill.getValue().getRed() / 255f, shaderChams.fill.getValue().getGreen() / 255f, shaderChams.fill.getValue().getBlue() / 255f);
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.render(tickDelta);
        } else if (shader == ShaderManager.Shader.Pulse) {
            effect.setUniformValue("alpha2", (shaderChams.fill.getValue().getAlpha() / 255f));
            effect.setUniformValue("oct", (int) shaderChams.octaves.getValue());

            effect.setUniformValue("radius", shaderChams.radius.getValueFloat());
            effect.setUniformValue("quality", shaderChams.smoothness.getValueFloat());
            effect.setUniformValue("divider", shaderChams.divider.getValueFloat());
            effect.setUniformValue("maxSample", shaderChams.maxSample.getValueFloat());
            effect.setUniformValue("factor", (float) shaderChams.factor.getValue());
            effect.setUniformValue("moreGradient", (float) shaderChams.gradient.getValue());

            effect.setUniformValue("color", shaderChams.fill.getValue().getRed() / 255f, shaderChams.fill.getValue().getGreen() / 255f, shaderChams.fill.getValue().getBlue() / 255f);
            effect.setUniformValue("color2", shaderChams.fill.getValue().getRed() / 255f, shaderChams.fill.getValue().getGreen() / 255f, shaderChams.fill.getValue().getBlue() / 255f);
            effect.setUniformValue("mixFactor", shaderChams.fill.getValue().getAlpha() / 255f);
            effect.setUniformValue("minAlpha", shaderChams.alpha.getValueFloat() / 255f);

            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", time);
            effect.render(tickDelta);
            time += (float) shaderChams.speed.getValue() * 0.002f;
        } else if (shader == ShaderManager.Shader.Snow) {
            effect.setUniformValue("color", shaderChams.fill.getValue().getRed() / 255f, shaderChams.fill.getValue().getGreen() / 255f, shaderChams.fill.getValue().getBlue() / 255f, shaderChams.fill.getValue().getAlpha() / 255f);
            effect.setUniformValue("radius", shaderChams.radius.getValueFloat());
            effect.setUniformValue("quality", shaderChams.smoothness.getValueFloat());
            effect.setUniformValue("divider", shaderChams.divider.getValueFloat());
            effect.setUniformValue("maxSample", shaderChams.maxSample.getValueFloat());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", time);
            effect.render(tickDelta);
            time += (float) shaderChams.speed.getValue() * 0.002f;
        } else if (shader == Shader.Flow) {
            effect.setUniformValue("mixFactor", shaderChams.fill.getValue().getAlpha() / 255f);
            effect.setUniformValue("radius", shaderChams.radius.getValueFloat());
            effect.setUniformValue("quality", shaderChams.smoothness.getValueFloat());
            effect.setUniformValue("divider", shaderChams.divider.getValueFloat());
            effect.setUniformValue("maxSample", shaderChams.maxSample.getValueFloat());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", time);
            effect.render(tickDelta);
            time += (float) shaderChams.speed.getValue() * 0.002f;
        }
    }

    public void reloadShaders() {
        DEFAULT = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/outline.json"));
        SMOKE = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/smoke.json"));
        PULSE = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/pulse.json"));
        GRADIENT = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/gradient.json"));
        SNOW = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/snow.json"));
        FLOW = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/flow.json"));
        RAINBOW = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/rainbow.json"));
        DEFAULT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/outline.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) return;

            ((IShaderEffect) effect).addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffect) effect).addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
        });

        SMOKE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/smoke.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) return;

            ((IShaderEffect) effect).addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffect) effect).addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
        });

        PULSE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/pulse.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) return;

            ((IShaderEffect) effect).addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffect) effect).addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
        });

        GRADIENT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/gradient.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) return;

            ((IShaderEffect) effect).addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffect) effect).addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
        });

        SNOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/snow.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) return;

            ((IShaderEffect) effect).addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffect) effect).addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
        });

        FLOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/flow.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) return;

            ((IShaderEffect) effect).addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffect) effect).addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        RAINBOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("minecraft", "shaders/post/rainbow.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) return;

            ((IShaderEffect) effect).addHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffect) effect).addHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
    }

    public static class MyFramebuffer extends Framebuffer {
        public MyFramebuffer(int width, int height) {
            super(false);
            RenderSystem.assertOnRenderThreadOrInit();
            resize(width, height, true);
            setClearColor(0f, 0f, 0f, 0f);
        }
    }

    public boolean fullNullCheck() {
        if (GRADIENT == null || SMOKE == null || PULSE == null || DEFAULT == null || FLOW == null || RAINBOW == null || SNOW == null
                || GRADIENT_OUTLINE == null || SMOKE_OUTLINE == null || PULSE_OUTLINE == null || DEFAULT_OUTLINE == null || FLOW_OUTLINE == null || RAINBOW_OUTLINE == null || SNOW_OUTLINE == null
                || shaderBuffer == null) {
            if (mc.getFramebuffer() == null) return true;
            shaderBuffer = new MyFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
            reloadShaders();
            return true;
        }

        return false;
    }

    public record RenderTask(Runnable task, Shader shader) {
    }

    public enum Shader {
        Solid,
        Smoke,
        Pulse,
        Gradient,
        Snow,
        Flow,
        Rainbow
    }
}
