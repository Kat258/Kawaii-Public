package dev.kizuna.mod.gui.elements;

import java.awt.Color;
import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.render.RenderShadersUtil;
import dev.kizuna.core.impl.GuiManager;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.tabs.Tab;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.impl.client.ClickGui;
import dev.kizuna.mod.modules.impl.client.HudEditor;
import dev.kizuna.mod.modules.impl.hud.PotionHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

public class PotionHUD
extends Tab {
    public PotionHUD() {
        this.width = 120;
        this.height = 120;
        this.x = (int)Kawaii.CONFIG.getFloat("potion_x", 10.0f);
        this.y = (int)Kawaii.CONFIG.getFloat("potion_y", 260.0f);
    }

    @Override
    public void update(double mouseX, double mouseY) {
        if (GuiManager.currentGrabbed == null && PotionHud.INSTANCE.isOn() && HudEditor.INSTANCE.isOn() && mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height) && ClickGuiScreen.clicked) {
            GuiManager.currentGrabbed = this;
        }
    }

    @Override
    public void draw(DrawContext drawContext, float partialTicks, Color color) {
        MatrixStack matrixStack = drawContext.getMatrices();
        if (ClickGui.INSTANCE.isOn() && !HudEditor.INSTANCE.isOn()) {
            return;
        }
        if (PotionHud.INSTANCE.isOn()) {
            this.drawPotionHUD(matrixStack, drawContext);
        }
    }

    private void drawPotionHUD(MatrixStack matrixStack, DrawContext drawContext) {
        assert this.mc.player != null;
        if (this.mc.player.getStatusEffects().isEmpty()) {
            return;
        }
        float maxWidth = 125.0f;
        float yOffset = 0.0f;
        for (StatusEffectInstance effect : this.mc.player.getStatusEffects()) {
            maxWidth = Math.max(maxWidth, Math.max(FontRenderers.ui.getWidth(effect.getEffectType() + " " + this.getAmplifierString(effect.getAmplifier())), FontRenderers.ui.getWidth(this.getDurationString(effect))) + 25.0f);
        }
        for (StatusEffectInstance effect : this.mc.player.getStatusEffects()) {
            StatusEffect statusEffect = (StatusEffect) effect.getEffectType();
            RenderShadersUtil.drawRect(matrixStack, this.x, (float)this.y + yOffset, maxWidth, 25.0f, 3.0f, new Color(-1962934272, true));
            RenderShadersUtil.drawRect(matrixStack, (float)this.x + maxWidth - 5.0f, (float)this.y + yOffset + 6.25f, 3.0f, 12.5f, 1.0f, new Color(statusEffect.getColor()));
            RenderShadersUtil.drawBlurredShadow(matrixStack, (float)this.x + maxWidth - 5.0f, (float)this.y + yOffset + 6.25f, 3.0f, 12.5f, 10, new Color(statusEffect.getColor()));
            matrixStack.push();
            matrixStack.translate((float)(this.x + 8), (float)this.y + yOffset + 5.0f, 0.0f);
            drawContext.drawSprite(0, 0, 0, 15, 15, this.mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType()));
            matrixStack.pop();
            FontRenderers.ui.drawString(matrixStack, statusEffect.getName().getString() + " " + this.getAmplifierString(effect.getAmplifier()), this.x + 27, (float)this.y + yOffset + 15.0f - FontRenderers.ui.getFontHeight(), new Color(statusEffect.getColor()).getRGB());
            FontRenderers.ui.drawString(matrixStack, this.getDurationString(effect), this.x + 27, (float)this.y + yOffset + 15.0f, new Color(0xFFFFFF).getRGB());
            yOffset += 30.0f;
        }
    }

    private String getAmplifierString(int amplifier) {
        return switch (amplifier) {
            case 0 -> "I";
            case 1 -> "II";
            case 2 -> "III";
            case 3 -> "IV";
            case 4 -> "V";
            default -> String.valueOf(amplifier + 1);
        };
    }

    private String getDurationString(StatusEffectInstance effect) {
        if (effect.isInfinite()) {
            return "\u221e";
        }
        int seconds = effect.getDuration() / 20;
        int minutes = seconds / 60;
        return String.format("%d:%02d", minutes, seconds %= 60);
    }
}
