package dev.kizuna.mod.gui.elements;

import dev.kizuna.api.utils.render.RenderShaderUtil;
import dev.kizuna.api.utils.render.animation.NotificationUtil;
import dev.kizuna.mod.gui.clickgui.tabs.Tab;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.impl.client.HUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import dev.kizuna.api.utils.render.TextUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationHUD extends Tab {
    public static final ArrayList<NotificationUtil> notifyList = new ArrayList<>();
    public NotificationHUD() {
        this.width = 120;
        this.height = 30;
    }

    private int getTextWidth(String text, HUD.FontType fontType) {
        switch (fontType) {
            case UI -> { return (int) FontRenderers.ui.getWidth(text); }
            case ICON -> { return (int) FontRenderers.icon.getWidth(text); }
            case TROLL -> { return (int) FontRenderers.troll.getWidth(text); }
            case CALIBRI -> {
                if (FontRenderers.Calibri != null) {
                    return (int) FontRenderers.Calibri.getWidth(text);
                }
                return (int) FontRenderers.ui.getWidth(text);
            }
            default -> { return mc.textRenderer.getWidth(text); }
        }
    }

    private void drawText(MatrixStack matrices, String text, float x, float y, int color, HUD.FontType fontType) {
        DrawContext context = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());
        boolean useCustomFont = fontType != HUD.FontType.DEFAULT;
        TextUtil.drawString(context, text, x, y, color, useCustomFont);
    }

    @Override
    public void update(double mouseX, double mouseY) {
        updateNotifications();
    }

    public static void updateNotifications() {
        if (HUD.INSTANCE == null || !HUD.INSTANCE.notification.getValue()) {
            return;
        }
        Iterator<NotificationUtil> iterator = notifyList.iterator();
        while (iterator.hasNext()) {
            NotificationUtil notification = iterator.next();
            if (notification == null || notification.message == null) {
                iterator.remove();
                continue;
            }
            notification.update();
            if (notification.shouldRemove()) {
                iterator.remove();
            }
        }
    }

    public static void clearNotifications() {
        notifyList.clear();
    }

    @Override
    public void draw(DrawContext drawContext, float partialTicks, Color color) {
        drawNotifications(drawContext);
    }

    private void drawNotifications(DrawContext drawContext) {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        MatrixStack matrixStack = drawContext.getMatrices();
        List<NotificationUtil> activeNotifications = notifyList.stream()
                .filter(NotificationUtil::isActive)
                .toList();
        // 从HUD模块获取位置偏移设置
        int baseX = HUD.INSTANCE != null ? HUD.INSTANCE.notificationX.getValueInt() : 5;
        int baseY = HUD.INSTANCE != null ? HUD.INSTANCE.notificationY.getValueInt() : 5;

        for (int i = 0; i < activeNotifications.size(); i++) {
            NotificationUtil notification = activeNotifications.get(i);
            if (notification == null) continue;
            String message = notification.message;
            int textWidth = getTextWidth(message, HUD.INSTANCE.notificationFont.getValue());
            int backgroundWidthValue = Math.max(textWidth + 10, 100);
            // 使用基本位置偏移和可调整的间隔
            int spacing = HUD.INSTANCE != null ? HUD.INSTANCE.notificationSpacing.getValueInt() : 25;
            double offsetX = baseX + notification.getOffsetX(backgroundWidthValue, screenWidth) - (screenWidth - backgroundWidthValue - 10);
            double offsetY = baseY + notification.getOffsetY(20, screenHeight, i, activeNotifications.size()) - (i * spacing);
            RenderShaderUtil.drawRoundedBlur(matrixStack, (float) offsetX, (float) offsetY, backgroundWidthValue, 20, 3f);
            RenderShaderUtil.drawBlurredShadow(matrixStack, (float) offsetX - 2, (float) offsetY -2, backgroundWidthValue, 20, 20,new Color(0x4C000000, true));

            Color notifColor = HUD.INSTANCE.notificationColor.getValue();
            RenderShaderUtil.drawBlurredShadow(matrixStack, (float) offsetX + 2f, (float) offsetY + (20 - 12.5f) / 2f, 3f, 12.5f,15, notifColor);
            RenderShaderUtil.drawRect(matrixStack, (float) offsetX + 2f, (float) offsetY + (20 - 12.5f) / 2f, 3f, 12.5f, 1f, new Color(notifColor.getRed(), notifColor.getGreen(), notifColor.getBlue(), 142));
            int textColor = new Color(255, 255, 255, (int) (255 * notification.getCurrentAlpha())).getRGB();
            float textY = (float) offsetY + (20 - 8) / 2.0f;
            drawText(matrixStack, message, (float) offsetX + 8, textY + 1.0f, textColor, HUD.INSTANCE.notificationFont.getValue());
        }
    }
}