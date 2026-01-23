package dev.kizuna.mod.modules.impl.hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import dev.kizuna.api.utils.DynamicIslandUtil.DynamicIslandAnimationUtil;
import dev.kizuna.api.utils.DynamicIslandUtil.NotificationUtil;
import dev.kizuna.api.utils.math.Easing;
import dev.kizuna.api.utils.render.RenderShadersUtil;
import dev.kizuna.api.utils.render.TextUtil;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.Formatting;

public class DynamicIsland
extends Module {
    public static DynamicIsland INSTANCE;
    private final List<NotificationUtil> notifications = new ArrayList<NotificationUtil>();
    private final DynamicIslandAnimationUtil widthAnimation = new DynamicIslandAnimationUtil();
    private final DynamicIslandAnimationUtil heightAnimation = new DynamicIslandAnimationUtil();

    public DynamicIsland() {
        super("DynamicIsland", Module.Category.Hud);
        this.setChinese("\u7075\u52a8\u5c9b");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (DynamicIsland.mc.player == null || DynamicIsland.mc.world == null) {
            return;
        }
        this.notifications.removeIf(notification -> System.currentTimeMillis() - notification.startTime > 2000L);
        if (!this.notifications.isEmpty()) {
            this.renderNotifications(context);
        } else {
            this.renderNormal(context);
        }
    }

    private void renderNormal(DrawContext context) {
        int ping = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(DynamicIsland.mc.player.getUuid()).getLatency() : 0;
        String text = String.format("%s | %s | %s | %s | %dFPS", "Catrix", mc.getSession().getUsername(), this.getServerIP(), this.getPingText(ping), mc.getCurrentFps());
        double animatedWidth = this.widthAnimation.get((int)(TextUtil.getWidth(text) + (FontRenderers.icon.getWidth("5") + FontRenderers.icon.getWidth("4") + FontRenderers.icon2.getWidth("d") + FontRenderers.icon.getWidth("O") + FontRenderers.icon2.getWidth("a") + 12.0f) + 20.0f));
        double animatedHeight = this.heightAnimation.get(20.0);
        RenderShadersUtil.drawRoundedBlur(context.getMatrices(), (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f - 2.0f, 15.0f, (float)animatedWidth, (float)animatedHeight, 10.0f, new Color(0x35000000, true), 15.0f, 0.55f);
        RenderShadersUtil.drawRect(context.getMatrices(), (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f - 2.0f, 15.0f, (float)animatedWidth, (float)animatedHeight, 10.0f, new Color(0x4F000000, true));
        if (Math.min(animatedWidth / (double)((int)(TextUtil.getWidth(text) + (FontRenderers.icon.getWidth("5") + FontRenderers.icon.getWidth("4") + FontRenderers.icon.getWidth("L") + FontRenderers.icon.getWidth("O") + FontRenderers.icon2.getWidth("a") + 12.0f) + 20.0f)), 1.0) > 0.93) {
            String[] parts = text.split(" \\| ");
            FontRenderers.icon2.drawString(context.getMatrices(), "a", (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + -4.0f, 16.5f + (float)((int)((animatedHeight - (double)FontRenderers.icon.getFontHeight()) / 2.0)) + 2.0f, this.getRainbowColor());
            TextUtil.drawString(context, parts[0], ((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0 + 10.0 + (double)((int)(FontRenderers.icon2.getWidth("a") + 2.0f)), (double)(15 + (int)((animatedHeight - (double)TextUtil.getHeight()) / 2.0) + 1), this.getRainbowColor());
            FontRenderers.icon.drawString(context.getMatrices(), "5", (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + (float)((int)(FontRenderers.icon2.getWidth("a") + 2.0f + TextUtil.getWidth(parts[0]) + TextUtil.getWidth(" | "))), 15 + (int)((animatedHeight - (double)FontRenderers.icon.getFontHeight()) / 2.0) + 2, Color.WHITE.getRGB());
            TextUtil.drawString(context, parts[1], ((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0 + 10.0 + (double)((int)(FontRenderers.icon2.getWidth("a") + 2.0f + TextUtil.getWidth(parts[0]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("5") + 2.0f)), (double)(15 + (int)((animatedHeight - (double)TextUtil.getHeight()) / 2.0) + 1), Color.WHITE.getRGB());
            FontRenderers.icon.drawString(context.getMatrices(), "4", (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + (float)((int)(FontRenderers.icon2.getWidth("a") + 2.0f + TextUtil.getWidth(parts[0]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("5") + 2.0f + TextUtil.getWidth(parts[1]) + TextUtil.getWidth(" | "))), 15 + (int)((animatedHeight - (double)FontRenderers.icon.getFontHeight()) / 2.0) + 2, Color.WHITE.getRGB());
            TextUtil.drawString(context, parts[2], ((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0 + 10.0 + (double)((int)(FontRenderers.icon2.getWidth("a") + 2.0f + TextUtil.getWidth(parts[0]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("5") + 2.0f + TextUtil.getWidth(parts[1]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("4") + 2.0f)), (double)(15 + (int)((animatedHeight - (double)TextUtil.getHeight()) / 2.0) + 1), Color.WHITE.getRGB());
            FontRenderers.icon2.drawString(context.getMatrices(), "d", ((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0 + 8.0 + (double)((int)(FontRenderers.icon2.getWidth("a") + 2.0f + TextUtil.getWidth(parts[0]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("5") + 2.0f + TextUtil.getWidth(parts[1]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("4") + 2.0f + TextUtil.getWidth(parts[2]) + TextUtil.getWidth(" | "))), (double)(15 + (int)((animatedHeight - (double)FontRenderers.icon.getFontHeight()) / 2.0) + 2) + 1.5, Color.WHITE.getRGB());
            TextUtil.drawString(context, parts[3], ((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0 + 10.0 + (double)((int)(FontRenderers.icon2.getWidth("a") + 2.0f + TextUtil.getWidth(parts[0]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("5") + 2.0f + TextUtil.getWidth(parts[1]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("4") + 2.0f + TextUtil.getWidth(parts[2]) + TextUtil.getWidth(" | ") + FontRenderers.icon2.getWidth("d") + 2.0f)), (double)(15 + (int)((animatedHeight - (double)TextUtil.getHeight()) / 2.0) + 1), this.getPingColor(ping).getRGB());
            FontRenderers.icon.drawString(context.getMatrices(), "O", (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + (float)((int)(FontRenderers.icon2.getWidth("a") + 2.0f + TextUtil.getWidth(parts[0]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("5") + 2.0f + TextUtil.getWidth(parts[1]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("4") + 2.0f + TextUtil.getWidth(parts[2]) + TextUtil.getWidth(" | ") + FontRenderers.icon2.getWidth("d") + 2.0f + TextUtil.getWidth(parts[3]) + TextUtil.getWidth(" | "))), 15 + (int)((animatedHeight - (double)FontRenderers.icon.getFontHeight()) / 2.0) + 2, Color.WHITE.getRGB());
            TextUtil.drawString(context, parts[4], ((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0 + 10.0 + (double)((int)(FontRenderers.icon2.getWidth("a") + 2.0f + TextUtil.getWidth(parts[0]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("5") + 2.0f + TextUtil.getWidth(parts[1]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("4") + 2.0f + TextUtil.getWidth(parts[2]) + TextUtil.getWidth(" | ") + FontRenderers.icon2.getWidth("d") + 2.0f + TextUtil.getWidth(parts[3]) + TextUtil.getWidth(" | ") + FontRenderers.icon.getWidth("O") + 2.0f)), (double)(15 + (int)((animatedHeight - (double)TextUtil.getHeight()) / 2.0) + 1), Color.WHITE.getRGB());
        }
    }

    private void renderNotifications(DrawContext context) {
        double animatedWidth = this.widthAnimation.get(this.notifications.stream().mapToInt(notification -> 25 + Math.max((int)TextUtil.getWidth("Module Toggled"), (int)TextUtil.getWidth(notification.message + " has been " + (notification.isEnable ? "Enabled" : "Disabled") + "!"))).max().orElse(0) + 40, 125L, Easing.IOS);
        double animatedHeight = this.heightAnimation.get(35 + (this.notifications.size() > 1 ? 37 * (this.notifications.size() - 1) : 0), 125L, Easing.IOS);
        RenderShadersUtil.drawRoundedBlur(context.getMatrices(), (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f - 2.0f, 15.0f, (float)animatedWidth, (float)animatedHeight, 10.0f, new Color(0x35000000, true), 15.0f, 0.55f);
        RenderShadersUtil.drawRect(context.getMatrices(), (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f - 2.0f, 15.0f, (float)animatedWidth, (float)animatedHeight, 10.0f, new Color(0x4F000000, true));
        for (int i = 0; i < this.notifications.size(); ++i) {
            NotificationUtil notification2 = this.notifications.get(i);
            float alpha = DynamicIslandAnimationUtil.getNotificationAlphaProgress(notification2.startTime);
            if (alpha <= 0.0f) continue;
            Color switchColor = DynamicIslandAnimationUtil.getSwitchColor(notification2.startTime, notification2.isEnable);
            RenderShadersUtil.drawRect(context.getMatrices(), (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f, (float)(15 + i * 37) + 17.5f - 6.5f, 25.0f, 15.0f, 7.5f, new Color(switchColor.getRed(), switchColor.getGreen(), switchColor.getBlue(), (int)((float)switchColor.getAlpha() * alpha)));
            RenderShadersUtil.drawRect(context.getMatrices(), DynamicIslandAnimationUtil.getSwitchPosition(notification2.startTime, notification2.isEnable, (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + 2.0f, (float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + 25.0f - 11.0f - 2.0f), (float)(15 + i * 37) + 17.5f - 6.5f + 2.0f, 11.0f, 11.0f, 5.5f, new Color(255, 255, 255, (int)(255.0f * alpha)));
            TextUtil.drawString(context, "Module Toggled", (double)((float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + 30.0f + 5.0f), (double)(15 + i * 37 + 8), new Color(255, 255, 255, (int)(255.0f * alpha)).getRGB());
            TextUtil.drawString(context, notification2.message, (double)((float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + 30.0f + 5.0f), (double)(15 + i * 37 + 20), new Color(255, 255, 255, (int)(255.0f * alpha)).getRGB());
            TextUtil.drawString(context, " has been ", (double)((float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + 30.0f + 5.0f + TextUtil.getWidth(notification2.message)), (double)(15 + i * 37 + 20), new Color(255, 255, 255, (int)(255.0f * alpha)).getRGB());
            Color textColor = notification2.isEnable ? new Color(-16711936) : new Color(-65536);
            TextUtil.drawString(context, notification2.isEnable ? "Enabled" : "Disabled", (double)((float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + 30.0f + 5.0f + TextUtil.getWidth(notification2.message + " has been ")), (double)(15 + i * 37 + 20), new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), (int)((float)textColor.getAlpha() * alpha)).getRGB());
            TextUtil.drawString(context, "!", (double)((float)((double)mc.getWindow().getScaledWidth() - animatedWidth) / 2.0f + 10.0f + 30.0f + 5.0f + TextUtil.getWidth(notification2.message + " has been ") + TextUtil.getWidth(notification2.isEnable ? "Enabled" : "Disabled")), (double)(15 + i * 37 + 20), new Color(255, 255, 255, (int)(255.0f * alpha)).getRGB());
        }
    }

    public void ModuleEnableNotification(String moduleName) {
        this.notifications.add(new NotificationUtil(moduleName, true));
    }

    public void ModuleDisableNotification(String moduleName) {
        this.notifications.add(new NotificationUtil(moduleName, false));
    }

    private String getServerIP() {
        if (DynamicIsland.mc.world == null || mc.getNetworkHandler() == null) {
            return "SinglePlayer";
        }
        ServerInfo serverInfo = mc.getCurrentServerEntry();
        if (serverInfo != null) {
            return serverInfo.address;
        }
        if (mc.isInSingleplayer()) {
            return "SinglePlayer";
        }
        return "Unknown";
    }

    private int getRainbowColor() {
        double rainbowState = Math.ceil((double)(System.currentTimeMillis() * 4L + 7000L) / 20.0);
        Color rainbowColor = Color.getHSBColor((float)(rainbowState % 360.0 / 360.0), 0.50980395f, 1.0f);
        return rainbowColor.getRGB();
    }

    private String getPingText(int ping) {
        Formatting color = Formatting.GREEN;
        if (ping >= 100) {
            color = Formatting.YELLOW;
        }
        if (ping >= 250) {
            color = Formatting.RED;
        }
        return color.toString() + ping + "ms";
    }

    private Color getPingColor(int ping) {
        if (ping >= 250) {
            return Color.RED;
        }
        if (ping >= 100) {
            return Color.YELLOW;
        }
        return Color.GREEN;
    }
}
