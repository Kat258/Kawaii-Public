package dev.kizuna.mod.gui.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.math.Timer;
import dev.kizuna.api.utils.render.Render2DUtil;
import dev.kizuna.api.utils.render.RenderShadersUtil;
import dev.kizuna.core.impl.GuiManager;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.tabs.Tab;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.impl.client.ClickGui;
import dev.kizuna.mod.modules.impl.client.HudEditor;
import dev.kizuna.mod.modules.impl.hud.SelfHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SelfHUD
extends Tab {
    private final Timer sessionTimer = new Timer();

    public SelfHUD() {
        this.width = 137;
        this.height = 55;
        this.x = (int)Kawaii.CONFIG.getFloat("self_hud_x", 1.0f);
        this.y = (int)Kawaii.CONFIG.getFloat("self_hud_y", 57.0f);
    }

    @Override
    public void update(double mouseX, double mouseY) {
        if (GuiManager.currentGrabbed == null && SelfHud.INSTANCE.isOn() && HudEditor.INSTANCE.isOn() && mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height) && ClickGuiScreen.clicked) {
            GuiManager.currentGrabbed = this;
        }
    }

    @Override
    public void draw(DrawContext drawContext, float partialTicks, Color color) {
        if (ClickGui.INSTANCE.isOn() && !HudEditor.INSTANCE.isOn()) {
            return;
        }
        if (SelfHud.INSTANCE.isOn() && this.mc.player != null) {
            this.renderSelfHud(drawContext, (AbstractClientPlayerEntity)this.mc.player);
        }
    }

    private void renderSelfHud(DrawContext drawContext, AbstractClientPlayerEntity player) {
        RenderShadersUtil.drawRoundedBlur(drawContext.getMatrices(), this.x, this.y, this.width, this.height, 9.0f, new Color(0x35000000, true), 15.0f, 0.55f);
        if (HudEditor.INSTANCE.isOff() && ClickGui.INSTANCE.isOff()) {
            RenderShadersUtil.drawBlurredShadow(drawContext.getMatrices(), this.x, this.y, this.width, this.height, 10, new Color(0x54000000, true));
        }
        FontRenderers.icon.drawString(drawContext.getMatrices(), "5", (float)this.x + 2.5f, this.y + 3, Color.WHITE.getRGB());
        FontRenderers.ui2.drawString(drawContext.getMatrices(), "Session Info:", this.x + 16, this.y + 6, Color.WHITE.getRGB());
        MatrixStack matrixStack = drawContext.getMatrices();
        matrixStack.push();
        RenderSystem.setShaderTexture((int)0, (Identifier)player.getSkinTextures().texture());
        RenderSystem.enableBlend();
        RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)true);
        RenderSystem.clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        RenderSystem.clear((int)16384, (boolean)false);
        RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Render2DUtil.renderRoundedQuadInternal(matrixStack.peek().getPositionMatrix(), 1.0f, 1.0f, 1.0f, 1.0f, (float)this.x + 3.0f, this.y + 17, (float)this.x + 3.0f + 33.0f, (float)(this.y + 17) + 33.0f, 7.0, 20.0);
        RenderSystem.blendFunc((int)772, (int)773);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderShadersUtil.renderTexture(drawContext.getMatrices(), (int)((float)this.x + 3.5f), (int)((float)this.y + 17.5f), 35.0, 35.0, 8.0f, 8.0f, 8.0, 8.0, 64.0, 64.0);
        RenderShadersUtil.renderTexture(drawContext.getMatrices(), (int)((float)this.x + 3.5f), (int)((float)this.y + 17.5f), 35.0, 35.0, 40.0f, 8.0f, 8.0, 8.0, 64.0, 64.0);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.defaultBlendFunc();
        matrixStack.pop();
        FontRenderers.ui2.drawString(drawContext.getMatrices(), player.getName().getString(), this.x + 45, this.y + 20, Color.WHITE.getRGB());
        FontRenderers.ui2.drawString(drawContext.getMatrices(), "Pops: " + Kawaii.POP.getPop(player.getName().getString()), this.x + 45, this.y + 32, Color.WHITE.getRGB());
        FontRenderers.ui2.drawString(drawContext.getMatrices(), "Played: " + this.formatTime(this.sessionTimer.getPassedTimeMs()), this.x + 45, this.y + 44, Color.GRAY.getRGB());
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        seconds %= 60L;
        minutes %= 60L;
        if (hours > 0L) {
            return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        }
        if (minutes > 0L) {
            return String.format("%dm %02ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }
}
