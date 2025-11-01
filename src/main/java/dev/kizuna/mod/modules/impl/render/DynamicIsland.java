package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.render.animation.EaseOutCirc;
import dev.kizuna.api.utils.render.RenderShaderUtil;
import dev.kizuna.api.utils.render.TextUtil;
import dev.kizuna.api.utils.render.animation.DynamicIslandAnimation;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.impl.combat.AutoAnchor;
import dev.kizuna.mod.modules.impl.combat.KawaiiAura;
import dev.kizuna.mod.modules.impl.combat.KillAura;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;

import java.awt.*;

// TODO By ZeroSeven
public class DynamicIsland extends Module {
    public static DynamicIsland INSTANCE;
    private final DynamicIslandAnimation animation = new DynamicIslandAnimation();
    private final EaseOutCirc healthAnimation = new EaseOutCirc();
    private AbstractClientPlayerEntity currentTarget = null;
    private double lastHealth = 0.0;
    
    public final SliderSetting posX = add(new SliderSetting("PositionX", 5, 0, 1000, -1));
    public final SliderSetting posY = add(new SliderSetting("PositionY", 5, 0, 1000, -1));

    public DynamicIsland() {
        super("DynamicIsland", Category.Client);
        setChinese("灵动岛");
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        healthAnimation.update();
        if (currentTarget != null) {
            double currentHealth = currentTarget.getHealth() + currentTarget.getAbsorptionAmount();
            if (currentHealth != lastHealth) {
                healthAnimation.setValue(currentHealth);
                lastHealth = currentHealth;
            }
        }
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) return;
        updateTarget();
        animation.update();
        if (animation.shouldShow() && currentTarget != null) {
            renderTargetHUD(context, currentTarget);
        } else{
            renderDefaultHUD(context);
        }
    }

    private void updateTarget() {
        AbstractClientPlayerEntity newTarget = null;
        if (KawaiiAura.INSTANCE.isOn() && isTargetAlive(KawaiiAura.INSTANCE.displayTarget)) {
            newTarget = (AbstractClientPlayerEntity) KawaiiAura.INSTANCE.displayTarget;
        } else if (KillAura.INSTANCE.isOn() && isTargetAlive(KillAura.target)) {
            newTarget = (AbstractClientPlayerEntity) KillAura.target;
        } else if (AutoAnchor.INSTANCE.isOn() && isTargetAlive(AutoAnchor.INSTANCE.displayTarget)) {
            newTarget = (AbstractClientPlayerEntity) AutoAnchor.INSTANCE.displayTarget;
        }

        if (newTarget != null && mc.player != null) {
            double distance = mc.player.distanceTo(newTarget);
            if (distance > 12.0) {
                newTarget = null;
            }
        }

        if (newTarget != currentTarget) {
            currentTarget = newTarget;
            animation.setTarget(currentTarget != null);

            if (currentTarget != null) {
                double initialHealth = currentTarget.getHealth() + currentTarget.getAbsorptionAmount();
                healthAnimation.setValue(initialHealth);
                lastHealth = initialHealth;
            } else {
                lastHealth = 0.0;
            }
        }
    }

    private void renderDefaultHUD(DrawContext context) {
        if (mc.player == null) return;
        MatrixStack matrices = context.getMatrices();
        int ping = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() : 0;
        String displayText = String.format("%s | %s | %s | %s | %dFPS", Kawaii.NAME + ".", mc.getSession().getUsername(), getServerIP(), getPingText(ping), mc.getCurrentFps());
        int iconTotalWidth = (int) (FontRenderers.icon.getWidth("5") +
                FontRenderers.icon.getWidth("4") + FontRenderers.icon.getWidth("L") +
                FontRenderers.icon.getWidth("O") + (2 * 5));
        int defaultWidth = (int) (TextUtil.getWidth(displayText) + iconTotalWidth + 20f);
        double width = animation.getWidthAnimation(defaultWidth, 100);
        double height = animation.getHeightAnimation(20, 40);
        int x = posX.getValueInt();
        int y = posY.getValueInt();
        float alpha = animation.getAlpha();
        float expandProgress = animation.getExpandProgress();
        RenderShaderUtil.drawRoundedBlur(matrices, x - 1, y, (float) width, (float) height, 10f, new Color(0xFF000000, true), 10.0f, 0.45f);

        int textY = y + (int) ((height - TextUtil.getHeight()) / 2f) + 1;
        int iconY = y + (int) ((height - FontRenderers.icon.getFontHeight()) / 2f) + 2;
        int currentX = x + 10;
        String[] parts = displayText.split(" \\| ");
        int maxX = x + (int) width - 10;
        // ClientName - 第一个显示
        float clientNameProgress = Math.min(1.0f, expandProgress / 0.15f);
        float clientNameAlpha = animation.easeOutCubic(clientNameProgress);
        if (currentX + TextUtil.getWidth(parts[0]) <= maxX && clientNameAlpha > 0.01f) {
            int rainbowColor = getRainbowColorWithAlpha(alpha * clientNameAlpha);
//         Color titleColor = ColorUtil.getRainbow(2L, 0.7f, 1.0f, 255, (long) (width/2*5L));
            TextUtil.drawString(context, parts[0], currentX, textY, rainbowColor);
        }
        currentX += (int) (TextUtil.getWidth(parts[0]) + TextUtil.getWidth(" | "));

        // PlayerName - 第二个显示
        float playerNameProgress = Math.max(0, Math.min(1.0f, (expandProgress - 0.15f) / 0.2f));
        float playerNameAlpha = animation.easeOutCubic(playerNameProgress);
        if (currentX + FontRenderers.icon.getWidth("5") <= maxX && playerNameAlpha > 0.01f) {
            int iconColor = new Color(255, 255, 255, (int) (255 * alpha * playerNameAlpha)).getRGB();
            FontRenderers.icon.drawString(matrices, "5", currentX, iconY, iconColor);
        }
        currentX += (int) (FontRenderers.icon.getWidth("5") + 2);
        if (currentX + TextUtil.getWidth(parts[1]) <= maxX && playerNameAlpha > 0.01f) {
            int textColor = new Color(255, 255, 255, (int) (255 * alpha * playerNameAlpha)).getRGB();
            TextUtil.drawString(context, parts[1], currentX, textY, textColor);
        }
        currentX += (int) (TextUtil.getWidth(parts[1]) + TextUtil.getWidth(" | "));

        // ServerIP - 第三个显示
        float serverIpProgress = Math.max(0, Math.min(1.0f, (expandProgress - 0.15f * 2) / 0.2f));
        float serverIpAlpha = animation.easeOutCubic(serverIpProgress);
        if (currentX + FontRenderers.icon.getWidth("4") <= maxX && serverIpAlpha > 0.01f) {
            int iconColor = new Color(255, 255, 255, (int) (255 * alpha * serverIpAlpha)).getRGB();
            FontRenderers.icon.drawString(matrices, "4", currentX, iconY, iconColor);
        }
        currentX += (int) (FontRenderers.icon.getWidth("4") + 2);
        if (currentX + TextUtil.getWidth(parts[2]) <= maxX && serverIpAlpha > 0.01f) {
            int textColor = new Color(255, 255, 255, (int) (255 * alpha * serverIpAlpha)).getRGB();
            TextUtil.drawString(context, parts[2], currentX, textY, textColor);
        }
        currentX += (int) (TextUtil.getWidth(parts[2]) + TextUtil.getWidth(" | "));

        // Ping - 第四个显示
        float pingProgress = Math.max(0, Math.min(1.0f, (expandProgress - 0.15f * 3) / 0.2f));
        float pingAlpha = animation.easeOutCubic(pingProgress);
        if (currentX + FontRenderers.icon.getWidth("L") <= maxX && pingAlpha > 0.01f) {
            int iconColor = new Color(255, 255, 255, (int) (255 * alpha * pingAlpha)).getRGB();
            FontRenderers.icon.drawString(matrices, "L", currentX, iconY + 1.2f, iconColor);
        }
        currentX += (int) (FontRenderers.icon.getWidth("L") + 2);
        if (currentX + TextUtil.getWidth(parts[3]) <= maxX && pingAlpha > 0.01f) {
            Color pingColor = getPingColor(ping);
            int textColor = new Color(pingColor.getRed(), pingColor.getGreen(), pingColor.getBlue(), (int) (255 * alpha * pingAlpha)).getRGB();
            TextUtil.drawString(context, parts[3], currentX, textY, textColor);
        }
        currentX += (int) (TextUtil.getWidth(parts[3]) + TextUtil.getWidth(" | "));

        // FPS - 第五个显示
        float fpsProgress = Math.max(0, Math.min(1.0f, (expandProgress - 0.15f * 4) / 0.2f));
        float fpsAlpha = animation.easeOutCubic(fpsProgress);
        if (currentX + FontRenderers.icon.getWidth("O") <= maxX && fpsAlpha > 0.01f) {
            int iconColor = new Color(255, 255, 255, (int) (255 * alpha * fpsAlpha)).getRGB();
            FontRenderers.icon.drawString(matrices, "O", currentX, iconY, iconColor);
        }
        currentX += (int) (FontRenderers.icon.getWidth("O") + 2);
        if (currentX + TextUtil.getWidth(parts[4]) <= maxX && fpsAlpha > 0.01f) {
            int textColor = new Color(255, 255, 255, (int) (255 * alpha * fpsAlpha)).getRGB();
            TextUtil.drawString(context, parts[4], currentX, textY, textColor);
        }
    }

    private void renderTargetHUD(DrawContext context, AbstractClientPlayerEntity target) {
        if (target == null || !isTargetAlive(target)) {
            currentTarget = null;
            return;
        }
        MatrixStack matrices = context.getMatrices();
        double width = animation.getWidthAnimation(200, 120);
        double height = animation.getHeightAnimation(20, 40);
        // 敌人信息居中显示
        int x = (mc.getWindow().getScaledWidth() - (int)width) / 2;
        int y = posY.getValueInt(); // 垂直位置仍然使用滑块设置
        RenderShaderUtil.drawRoundedBlur(matrices, x, y, (float)width, (float)height, 10f, new Color(0xFF000000, true), 10.0f, 0.45f);
        int avatarX = x + 5, avatarY = y + 5;
        if (target.getSkinTextures() != null) {
            float avatarAlpha = animation.getAlpha() * animation.getHealthBarProgress();
            context.getMatrices().push();
            float enterScale;
            if (animation.getExpandProgress() < 0.7f) {
                enterScale = 0.2f + 0.8f * (animation.getExpandProgress() / 0.7f);
            } else {
                float overshoot = (animation.getExpandProgress() - 0.7f) / 0.3f;
                enterScale = 1.0f + (float) Math.sin(overshoot * Math.PI) * 0.1f;
            }
            float avatarCenterX = avatarX + 15;
            float avatarCenterY = avatarY + 15;
            context.getMatrices().translate(avatarCenterX, avatarCenterY, 0);
            context.getMatrices().scale(enterScale, enterScale, 1f);
            context.getMatrices().translate(-avatarCenterX, -avatarCenterY, 0);
            context.setShaderColor(1f, 1f, 1f, avatarAlpha);
            context.drawTexture(target.getSkinTextures().texture(), avatarX, avatarY, 30, 30, 8, 8, 8, 8, 64, 64);
            context.setShaderColor(1f, 1f, 1f, 1f);
            context.getMatrices().pop();
        }
        float healthPercent = (float) Math.min(Math.max(healthAnimation.getAnimationD() / target.getMaxHealth(), 0f), 1f);
        int textX = avatarX + 30 + 5, textY = avatarY + 5;
        float textAlpha = Math.max(0, (animation.getExpandProgress() - 0.1f) / 0.9f) * animation.getAlpha();
        FontRenderers.ui.drawString(matrices, target.getName().getString(), textX, textY, getRainbowColorWithAlpha(textAlpha));
        if (animation.getHealthBarProgress() > 0.1f) {
            String healthText = String.format("%.1fHP", healthAnimation.getAnimationD());
            float healthTextAlpha = Math.max(0, (animation.getExpandProgress() - 0.3f) / 0.7f) * animation.getAlpha();
            FontRenderers.ui.drawString(matrices, healthText, textX, textY + 10, new Color(255, 255, 255, (int)(255 * healthTextAlpha)).getRGB());
            String pops = "0";
            if (Kawaii.POP.popContainer.containsKey(target.getName().getString())) {
                pops = String.valueOf(Kawaii.POP.popContainer.get(target.getName().getString()));
            }
            String popText = pops + "Pops";
            float popTextX = textX + FontRenderers.ui.getWidth(healthText) + 8;
            FontRenderers.ui.drawString(matrices, popText, popTextX, textY + 10, new Color(255, 255, 255, (int)(255 * healthTextAlpha)).getRGB());
            int healthBarWidth = (int)width - (textX - x) - 10;
            int healthBarY = textY + 20;
            int animatedHealthBarWidth = (int)(healthBarWidth * animation.getExpandProgress());
            RenderShaderUtil.drawRect(matrices, textX, healthBarY, animatedHealthBarWidth, 6f,3f,new Color(0x35000000,true));
            if (healthPercent > 0) {
                int filledWidth = (int) (animatedHealthBarWidth * healthPercent);
                RenderShaderUtil.drawRect(matrices, textX, healthBarY, filledWidth, 6.5f, 3f, new Color(0xB5830CFF, true));
            }
        }
    }

    private int getRainbowColorWithAlpha(float alpha) {
        double rainbowState = Math.ceil((System.currentTimeMillis() * 4 + 20 * 350) / 20.0);
        Color rainbowColor = Color.getHSBColor((float) (rainbowState % 360.0 / 360), 130.0f / 255.0f, 1.0f);
        return new Color(rainbowColor.getRed(), rainbowColor.getGreen(), rainbowColor.getBlue(), (int)(255 * alpha)).getRGB();
    }

    private String getServerIP() {
        if (mc.world == null || mc.getNetworkHandler() == null) {
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
        } else if (ping >= 100) {
            return Color.YELLOW;
        } else {
            return Color.GREEN;
        }
    }

    private boolean isTargetAlive(Object target) {
        if (!(target instanceof AbstractClientPlayerEntity player)) {
            return false;
        }
        if (player.getWorld() == null || player.isRemoved()) {
            return false;
        }
        return !player.isDead();
    }
}