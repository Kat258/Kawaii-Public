package dev.kizuna.mod.gui.elements;

import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.render.RenderShaderUtil;
import dev.kizuna.core.impl.GuiManager;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.tabs.Tab;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.impl.client.HUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import dev.kizuna.api.utils.render.TextUtil;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.awt.*;

public class PotionHUD extends Tab {
    public PotionHUD() {
        this.width = 120;
        this.height = 25;
        this.x = (int) Kawaii.CONFIG.getFloat("potion_x", 10);
        this.y = (int) Kawaii.CONFIG.getFloat("potion_y", 260);
    }
    
    private float getTextWidth(String text, HUD.FontType fontType) {
        switch (fontType) {
            case UI -> { return FontRenderers.ui.getWidth(text); }
            case ICON -> { return FontRenderers.icon.getWidth(text); }
            case TROLL -> { return FontRenderers.troll.getWidth(text); }
            case CALIBRI -> { 
                if (FontRenderers.Calibri != null) { 
                    return FontRenderers.Calibri.getWidth(text); 
                }
                return FontRenderers.ui.getWidth(text);
            }
            default -> { return mc.textRenderer.getWidth(text); }
        }
    }
    
    private float getFontHeight(HUD.FontType fontType) {
        switch (fontType) {
            case UI -> { return FontRenderers.ui.getFontHeight(); }
            case ICON -> { return FontRenderers.icon.getFontHeight(); }
            case TROLL -> { return FontRenderers.troll.getFontHeight(); }
            case CALIBRI -> { 
                if (FontRenderers.Calibri != null) { 
                    return FontRenderers.Calibri.getFontHeight(); 
                }
                return FontRenderers.ui.getFontHeight();
            }
            default -> { return mc.textRenderer.fontHeight; }
        }
    }
    
    private void drawText(MatrixStack matrices, String text, float x, float y, int color, HUD.FontType fontType) {
        DrawContext context = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());
        boolean useCustomFont = fontType != HUD.FontType.DEFAULT;
        TextUtil.drawString(context, text, x, y, color, useCustomFont);
    }

    @Override
    public void update(double mouseX, double mouseY) {
        if (GuiManager.currentGrabbed == null && HUD.INSTANCE.isOn() && HUD.INSTANCE.potionHud.getValue()) {
            if (mouseX >= x && mouseX <= x + width) {
                if (mouseY >= y && mouseY <= y + height) {
                    if (ClickGuiScreen.clicked) {
                        GuiManager.currentGrabbed = this;
                    }
                }
            }
        }
    }

    @Override
    public void draw(DrawContext drawContext, float partialTicks, Color color) {
        MatrixStack matrixStack = drawContext.getMatrices();
        if (HUD.INSTANCE.isOn() && HUD.INSTANCE.potionHud.getValue()) {
            drawPotionHUD(matrixStack, drawContext);
        }
    }

    private void drawPotionHUD(MatrixStack matrixStack, DrawContext drawContext) {
        float maxWidth = 125;
        int effectCount = 0;
        for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            String name = effect.getEffectType().value().getName().getString();
            String amplifier = getAmplifierString(effect.getAmplifier());
            String duration = getDurationString(effect);
            float nameWidth = getTextWidth(name + " " + amplifier, HUD.INSTANCE.potionHudFont.getValue());
            float timeWidth = getTextWidth(duration, HUD.INSTANCE.potionHudFont.getValue());
            float totalWidth = Math.max(nameWidth, timeWidth) + 25;
            if (totalWidth > maxWidth) {
                maxWidth = totalWidth;
            }
            effectCount++;
        }
        float effectHeight = 25;
        float exactHeight = effectCount * (effectHeight + 5);
        this.width = (int) maxWidth;
        this.height = (int) exactHeight;
        if (effectCount <= 0) {
            this.width = 0;
            this.height = 0;
            return;
        }
        // 从HUD模块获取位置偏移设置
        int currentX = HUD.INSTANCE != null ? HUD.INSTANCE.potionHudX.getValueInt() : x;
        int currentY = HUD.INSTANCE != null ? HUD.INSTANCE.potionHudY.getValueInt() : y;
        
        float yOffset = 0;
        for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            StatusEffect statusEffect = effect.getEffectType().value();
            String name = statusEffect.getName().getString();
            String amplifier = getAmplifierString(effect.getAmplifier());
            String duration = getDurationString(effect);
            float effectY = currentY + yOffset;
            RenderShaderUtil.drawRoundedBlur(matrixStack, currentX, effectY, maxWidth, effectHeight, 3f,new Color(0x84000000, true),10.0f,0.60f);
            RenderShaderUtil.drawBlurredShadow(matrixStack, currentX, effectY, maxWidth, effectHeight, 15,new Color(0x8B0C0C0C, true));
            Color effectColor = new Color(statusEffect.getColor());
            float colorBarHeight = effectHeight / 2;
            float colorBarX = currentX + maxWidth - 3 - 2;
            float colorBarY = effectY + (effectHeight - colorBarHeight) / 2;
            RenderShaderUtil.drawRect(matrixStack, colorBarX, colorBarY, 3, colorBarHeight, 1f, effectColor);
            RenderShaderUtil.drawBlurredShadow(matrixStack, colorBarX, colorBarY, 3, colorBarHeight, 15, effectColor);
            float textHeight = getFontHeight(HUD.INSTANCE.potionHudFont.getValue());
            float totalTextHeight = textHeight * 2;
            float textStartY = effectY + (effectHeight - totalTextHeight) / 2;
            float timeY = textStartY + textHeight;
            float iconY = effectY + (effectHeight - 15) / 2;
            matrixStack.push();
            matrixStack.translate(currentX + 8, iconY -0.5f, 0);
            drawContext.drawSprite(0, 0, 0,  15,  15, mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType()));
            matrixStack.pop();
            String nameWithLevel = name + " " + amplifier;
            drawText(matrixStack, nameWithLevel, (float)(currentX + 8 + 15 + 4), textStartY + 2, effectColor.getRGB(), HUD.INSTANCE.potionHudFont.getValue());
            drawText(matrixStack, duration, (float)(currentX + 8 + 15 + 4), (float)(timeY + 1.0), new Color(0xFFFFFF).getRGB(), HUD.INSTANCE.potionHudFont.getValue());
            yOffset += effectHeight + 5;
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
            return "∞";
        }
        int seconds = effect.getDuration() / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
