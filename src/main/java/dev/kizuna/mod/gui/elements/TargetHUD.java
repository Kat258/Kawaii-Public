package dev.kizuna.mod.gui.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.math.EaseOutCirc;
import dev.kizuna.api.utils.render.Render2DUtil;
import dev.kizuna.api.utils.render.RenderShadersUtil;
import dev.kizuna.api.utils.render.TextUtil;
import dev.kizuna.core.impl.GuiManager;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.tabs.Tab;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.impl.client.ClickGui;
import dev.kizuna.mod.modules.impl.client.HudEditor;
import dev.kizuna.mod.modules.impl.combat.AutoAnchor;
import dev.kizuna.mod.modules.impl.combat.KawaiiAura;
import dev.kizuna.mod.modules.impl.combat.KillAura;
import dev.kizuna.mod.modules.impl.hud.TargetHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class TargetHUD
extends Tab {
    private PlayerEntity lastTarget = null;
    public final EaseOutCirc healthAnimation = new EaseOutCirc();
    public static TargetHUD INSTANCE;

    public TargetHUD() {
        this.width = 137;
        this.height = 48;
        this.x = (int)Kawaii.CONFIG.getFloat("target_x", 0.0f);
        this.y = (int)Kawaii.CONFIG.getFloat("target_y", 250.0f);
        INSTANCE = this;
    }

    @Override
    public void update(double mouseX, double mouseY) {
        PlayerEntity target;
        if (GuiManager.currentGrabbed == null && TargetHud.INSTANCE.isOn() && HudEditor.INSTANCE.isOn() && mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height) && ClickGuiScreen.clicked) {
            GuiManager.currentGrabbed = this;
        }
        if ((target = this.getTarget()) != this.lastTarget) {
            this.lastTarget = target;
        }
    }

    @Override
    public void draw(DrawContext drawContext, float partialTicks, Color color) {
        PlayerEntity target;
        if (TargetHud.INSTANCE.isOn() && (target = this.getTarget()) != null) {
            if (TargetHud.INSTANCE.follow.getValue()) {
                this.updateFollowTarget(target);
            }
            this.renderTargetHud(drawContext, target);
        }
    }

    private void updateFollowTarget(PlayerEntity target) {
        double x = target.prevX + (target.getX() - target.prevX) * (double)this.mc.getRenderTickCounter().getTickDelta(true);
        double y = target.prevY + (target.getY() - target.prevY) * (double)this.mc.getRenderTickCounter().getTickDelta(true) + (double)target.getStandingEyeHeight();
        double z = target.prevZ + (target.getZ() - target.prevZ) * (double)this.mc.getRenderTickCounter().getTickDelta(true);
        Vec3d worldPos = new Vec3d(x, y, z);
        Vec3d screenPos = TextUtil.worldSpaceToScreenSpace(worldPos);
        if (screenPos.z > 0.0 && screenPos.z < 1.0) {
            this.x = (int)screenPos.x + 15;
            this.y = (int)screenPos.y - this.height / 2;
        }
    }

    private PlayerEntity getTarget() {
        double distance;
        Entity entity;
        PlayerEntity target = null;
        if (KawaiiAura.INSTANCE.isOn() && KawaiiAura.INSTANCE.displayTarget instanceof PlayerEntity && !KawaiiAura.INSTANCE.displayTarget.isDead() && KawaiiAura.INSTANCE.displayTarget.getWorld() != null && !KawaiiAura.INSTANCE.displayTarget.isRemoved()) {
            target = (PlayerEntity) KawaiiAura.INSTANCE.displayTarget;
        } else if (KillAura.INSTANCE.isOn() && (entity = KillAura.target) instanceof PlayerEntity) {
            PlayerEntity killAuraTarget = (PlayerEntity)entity;
            if (!killAuraTarget.isDead() && killAuraTarget.getWorld() != null && !killAuraTarget.isRemoved()) {
                target = killAuraTarget;
            }
        } else if (this.mc.currentScreen instanceof ChatScreen || HudEditor.INSTANCE.isOn()) {
            target = this.mc.player;
        }
        if (target == null && AutoAnchor.INSTANCE.isOn() && AutoAnchor.INSTANCE.displayTarget instanceof PlayerEntity && !AutoAnchor.INSTANCE.displayTarget.isDead() && AutoAnchor.INSTANCE.displayTarget.getWorld() != null && !AutoAnchor.INSTANCE.displayTarget.isRemoved()) {
            target = (PlayerEntity) AutoAnchor.INSTANCE.displayTarget;
        }
        if (target != null && this.mc.player != null && (distance = (double)this.mc.player.distanceTo((Entity)target)) > 12.0) {
            target = null;
        }
        return target;
    }

    private void renderTargetHud(DrawContext drawContext, PlayerEntity target) {
        RenderShadersUtil.drawRoundedBlur(drawContext.getMatrices(), this.x, this.y, this.width, this.height, 9.0f, new Color(0x35000000, true), 15.0f, 0.55f);
        if (HudEditor.INSTANCE.isOff() && ClickGui.INSTANCE.isOff()) {
            RenderShadersUtil.drawBlurredShadow(drawContext.getMatrices(), this.x, this.y, this.width, this.height, 10, new Color(0x54000000, true));
        }
        float hurtPercent = this.interpolateFloat(Math.max(target.hurtTime == 0 ? 0 : target.hurtTime + 1, 0), target.hurtTime, this.mc.getRenderTickCounter().getTickDelta(true)) / 8.0f;
        this.healthAnimation.setValue(target.getHealth() + target.getAbsorptionAmount());
        if (target instanceof AbstractClientPlayerEntity) {
            MatrixStack matrixStack = drawContext.getMatrices();
            matrixStack.push();
            matrixStack.translate((float)this.x + 3.5f + 20.0f, (float)this.y + 3.5f + 20.0f, 0.0f);
            matrixStack.scale(1.0f - hurtPercent / 15.0f, 1.0f - hurtPercent / 15.0f, 1.0f);
            matrixStack.translate(-((float)this.x + 3.5f + 20.0f), -((float)this.y + 3.5f + 20.0f), 0.0f);
            RenderSystem.setShaderTexture((int)0, (Identifier)((AbstractClientPlayerEntity)target).getSkinTextures().texture());
            RenderSystem.enableBlend();
            RenderSystem.colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)true);
            RenderSystem.clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
            RenderSystem.clear((int)16384, (boolean)false);
            RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            Render2DUtil.renderRoundedQuadInternal(matrixStack.peek().getPositionMatrix(), 1.0f, 1.0f, 1.0f, 1.0f, (float)this.x + 3.0f, (float)this.y + 3.0f, (float)this.x + 3.0f + 39.0f, (float)this.y + 3.5f + 39.0f, 7.0, 10.0);
            RenderSystem.blendFunc((int)772, (int)773);
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor((float)1.0f, (float)(1.0f - hurtPercent / 2.0f), (float)(1.0f - hurtPercent / 2.0f), (float)1.0f);
            RenderShadersUtil.renderTexture(drawContext.getMatrices(), (int)((float)this.x + 3.5f), (int)((float)this.y + 3.5f), 40.0, 40.0, 8.0f, 8.0f, 8.0, 8.0, 64.0, 64.0);
            RenderShadersUtil.renderTexture(drawContext.getMatrices(), (int)((float)this.x + 3.5f), (int)((float)this.y + 3.5f), 40.0, 40.0, 40.0f, 8.0f, 8.0, 8.0, 64.0, 64.0);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            RenderSystem.defaultBlendFunc();
            matrixStack.pop();
        }
        RenderShadersUtil.drawRect(drawContext.getMatrices(), this.x + 48, this.y + 32, 85.0f, 11.0f, 4.0f, new Color(0x4D000000, true));
        RenderShadersUtil.drawBlurredShadow(drawContext.getMatrices(), this.x + 48 - 2, this.y + 32 - 2, 89.0f, 15.0f, 10, new Color(0x4F000000, true));
        float healthRatio = Math.max(0.0f, Math.min((float)this.healthAnimation.getAnimationD() / target.getMaxHealth(), 1.0f));
        RenderShadersUtil.drawRect(drawContext.getMatrices(), this.x + 48, this.y + 32, Math.max(8.0f, 85.0f * healthRatio), 11.0f, 4.0f, TargetHud.INSTANCE.color.getValue());
        FontRenderers.ui.drawString(drawContext.getMatrices(), target.getName().getString(), this.x + 48, this.y + 7, Color.WHITE.getRGB());
        FontRenderers.ui.drawCenteredString(drawContext.getMatrices(), String.format("%.1f", Float.valueOf((float)this.healthAnimation.getAnimationD())), (float)(this.x + 48) + 42.5f, (float)this.y + 32.5f, Color.WHITE.getRGB());
    }

    private float interpolateFloat(float current, float last, float partialTicks) {
        return last + (current - last) * partialTicks;
    }
}
