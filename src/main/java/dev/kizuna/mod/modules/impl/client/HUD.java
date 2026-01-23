package dev.kizuna.mod.modules.impl.client;

import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.entity.InventoryUtil;
import dev.kizuna.api.utils.render.RenderShaderUtil;
import dev.kizuna.api.utils.render.TextUtil;
import dev.kizuna.mod.gui.elements.NotificationHUD;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import dev.kizuna.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HUD extends Module {
    public static HUD INSTANCE;
    public final BooleanSetting armor = add(new BooleanSetting("Armor", true));
    public final BooleanSetting notification = add(new BooleanSetting("Notification", true));
    public final SliderSetting notificationX = add(new SliderSetting("NotificationX", 5, 0, 1000, -1, notification::isOpen));
    public final SliderSetting notificationY = add(new SliderSetting("NotificationY", 5, 0, 1000, -1, notification::isOpen));
    public final SliderSetting notificationSpacing = add(new SliderSetting("NotificationSpacing", 25, 5, 100, -1, notification::isOpen));
    public final EnumSetting<FontType> notificationFont = add(new EnumSetting<>("NotificationFont", FontType.UI, notification::isOpen));
    public final BooleanSetting potionHud = add(new BooleanSetting("PotionHUD", true));
    public final SliderSetting potionHudX = add(new SliderSetting("PotionHudX", 5, 0, 1000, -1, potionHud::isOpen));
    public final SliderSetting potionHudY = add(new SliderSetting("PotionHudY", 5, 0, 1000, -1, potionHud::isOpen));
    public final EnumSetting<FontType> potionHudFont = add(new EnumSetting<>("PotionHudFont", FontType.UI, potionHud::isOpen));
    
    public enum FontType {
        UI, DEFAULT, ICON, TROLL, CALIBRI
    }
    public final BooleanSetting itemsCountHud = add(new BooleanSetting("ItemsCountHud", true));
    public final BooleanSetting showPotionCount = add(new BooleanSetting("ShowPotionCount", false));
    public final SliderSetting potionCountX = add(new SliderSetting("PotionCountX", 5, 0, 1000, -1, showPotionCount::isOpen));
    public final SliderSetting potionCountY = add(new SliderSetting("PotionCountY", 5, 0, 1000, -1, showPotionCount::isOpen));
    public final BooleanSetting up = add(new BooleanSetting("Up", false));
    public final BooleanSetting customFont = add(new BooleanSetting("CustomFont", true));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(0xFFACAFFD, true)));
    public final ColorSetting pulse = add(new ColorSetting("Pulse", new Color(0x6EACAFFD, true)).injectBoolean(true));

    public final BooleanSetting welcome = add(new BooleanSetting("Welcome", false).setParent());
    public final BooleanSetting welcomeSync = add(new BooleanSetting("WelcomeSync", true,welcome::isOpen));
    public final StringSetting welcomeText = add(new StringSetting("WelcomeText", "Hello, [username]! :^)",welcome::isOpen));

    public final BooleanSetting waterMark = add(new BooleanSetting("WaterMark", true).setParent());
    public final StringSetting waterMarkString = add(new StringSetting("Title", "%hackname% %version%.",waterMark::isOpen));
    public final SliderSetting offset = add(new SliderSetting("Offset", 5, 0, 100, -1,waterMark::isOpen));

    public final BooleanSetting playerRadar = add(new BooleanSetting("PlayerRadar", true).setParent());
    public final BooleanSetting playerIcons = add(new BooleanSetting("Icons", true, playerRadar::isOpen));
    public final BooleanSetting playerRadarDistance = add(new BooleanSetting("Distance", true, playerRadar::isOpen));
    public final BooleanSetting playerRadarPing = add(new BooleanSetting("Ping", true, playerRadar::isOpen));
    public final BooleanSetting playerRadarHealth = add(new BooleanSetting("Health", true, playerRadar::isOpen));

    public final BooleanSetting sync = add(new BooleanSetting("InfoColorSync", true));
    public final BooleanSetting lowerCase = add(new BooleanSetting("LowerCase", false));
    public final BooleanSetting fps = add(new BooleanSetting("FPS", true));
    public final BooleanSetting ping = add(new BooleanSetting("Ping", true));
    public final BooleanSetting tps = add(new BooleanSetting("TPS", true));
    public final BooleanSetting ip = add(new BooleanSetting("IP", false));
    public final BooleanSetting time = add(new BooleanSetting("Time", false));
    public final BooleanSetting speed = add(new BooleanSetting("Speed", true));
    public final BooleanSetting brand = add(new BooleanSetting("Brand", false));
    public final BooleanSetting coords = add(new BooleanSetting("Coords", true));
    private final SliderSetting pulseSpeed = add(new SliderSetting("Speed", 1, 0, 5, 0.1));
    private final SliderSetting pulseCounter = add(new SliderSetting("Counter", 10, 1, 50));

    public HUD() {
        super("HUD", Category.Client);
        setChinese("界面");
        INSTANCE = this;
    }

    private final DecimalFormat decimal = new DecimalFormat("0.0");

    @Override
    public void onUpdate() {
        if (Kawaii.GUI != null && Kawaii.GUI.armorHud != null) {
            NotificationHUD.updateNotifications();
        }
    }

    @Override
    public void onDisable() {
        NotificationHUD.clearNotifications();
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (armor.getValue()) {
            Kawaii.GUI.armorHud.draw(drawContext, tickDelta, null);
        }
        if (notification.getValue()) {
            Kawaii.GUI.notificationHud.draw(drawContext, tickDelta, null);
        }
        if (potionHud.getValue()) {
            Kawaii.GUI.potionHud.draw(drawContext, tickDelta, null);
        }
        if (itemsCountHud.getValue()) {
            Kawaii.GUI.itemsCountHud.draw(drawContext, tickDelta, null);
        }
        if (showPotionCount.getValue()) {
            int splashPotionCount = InventoryUtil.getItemCount(Items.SPLASH_POTION);
            String text = "Potion: " + splashPotionCount;
            drawText(drawContext, text, (int)potionCountX.getValue(), (int)potionCountY.getValue());
        }
        if (welcome.getValue()) {
            String text = welcomeText.getValue().replace("[username]", (welcomeSync.getValue() ? "" : "§f") + mc.player.getName().getString() + "§r");
            float textWidth = TextUtil.getWidth(text);
            float x = mc.getWindow().getScaledWidth() / 2.0f - textWidth / 2.0f;
            float y = 6;
            drawText(drawContext, text, (int) x, (int) y);
        }
        if (waterMark.getValue()) {
            String waterMarkText = waterMarkString.getValue().replace("%version%", Kawaii.VERSION).replace("%hackname%", Kawaii.NAME);
            int offsetValue = offset.getValueInt();
            int textWidth = getWidth(waterMarkText);
            int textHeight = getHeight();
            float bgX = offsetValue - 2;
            float bgY = offsetValue - 2;
            float bgWidth = textWidth + 4;
            float bgHeight = textHeight + 4;
            RenderShaderUtil.drawBlurredShadow(drawContext.getMatrices(), bgX, bgY, bgWidth, bgHeight,10,new Color(0x80000000, true));
            RenderShaderUtil.drawRoundedBlur(drawContext.getMatrices(), bgX, bgY, bgWidth, bgHeight, 3f);
            drawText(drawContext, waterMarkText, offsetValue, offsetValue);
        }
        if (playerRadar.getValue() && mc.player != null && mc.world != null) {
            renderPlayerRadar(drawContext);
        }
        int fontHeight = getHeight();
        int height;
        int y;
        if (up.getValue()) {
            y = 1;
            height = -fontHeight;
        } else {
            y = mc.getWindow().getScaledHeight() - fontHeight;
            if (mc.currentScreen instanceof ChatScreen) {
                y -= 15;
            }
            height = fontHeight;
        }
        int windowWidth = mc.getWindow().getScaledWidth() - 1;
        if (brand.getValue()) {
            String brand = (mc.isInSingleplayer() ? "Vanilla" : mc.getNetworkHandler().getBrand().replaceAll("\\(.*?\\)", ""));
            int x = getWidth("ServerBrand " + brand);
            drawText(drawContext, "ServerBrand §f" + brand, windowWidth - x, y);
            y -= height;
        }
        if (time.getValue()) {
            String text = "Time §f" + (new SimpleDateFormat("h:mm a", Locale.ENGLISH)).format(new Date());
            int width = getWidth(text);
            drawText(drawContext, text, windowWidth - width, y);
            y -= height;
        }
        if (ip.getValue()) {
            int x = getWidth("Server " + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address));
            drawText(drawContext, "Server §f" + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address), windowWidth - x, y);
            y -= height;
        }
        if (tps.getValue()) {
            int x = getWidth("TPS " + Kawaii.SERVER.getTPS() + " [" + Kawaii.SERVER.getCurrentTPS() + "]");
            drawText(drawContext, "TPS §f" + Kawaii.SERVER.getTPS() + " §7[§f" + Kawaii.SERVER.getCurrentTPS() + "§7]", windowWidth - x, y);
            y -= height;
        }
        if (speed.getValue()) {
            double x = mc.player.getX() - mc.player.prevX;
            double z = mc.player.getZ() - mc.player.prevZ;
            double dist = Math.sqrt(x * x + z * z) / 1000.0;
            double div = 0.05 / 3600.0;
            float timer = Kawaii.TIMER.get();
            final double speed = dist / div * timer;
            String text = String.format("Speed §f%skm/h",
                    decimal.format(speed));
            int width = getWidth(text);
            drawText(drawContext, text, windowWidth - width, y);
            y -= height;
        }
        if (fps.getValue()) {
            int x = getWidth("FPS " + Kawaii.FPS.getFps());
            drawText(drawContext, "FPS §f" + Kawaii.FPS.getFps(), windowWidth - x, y);
            y -= height;
        }
        if (ping.getValue()) {
            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            String ping;
            if (playerListEntry == null) {
                ping = "Unknown";
            } else {
                ping = String.valueOf(playerListEntry.getLatency());
            }
            int x = getWidth("Ping " + ping);
            drawText(drawContext, "Ping §f" + ping, windowWidth - x, y);
            y -= height;
        }

        if (coords.getValue()) {
            boolean inNether = mc.world.getRegistryKey().equals(World.NETHER);

            int posX = mc.player.getBlockX();
            int posY = mc.player.getBlockY();
            int posZ = mc.player.getBlockZ();

            float factor = !inNether ? 0.125F : 8.0F;

            int anotherWorldX = (int) (mc.player.getX() * factor);
            int anotherWorldZ = (int) (mc.player.getZ() * factor);

            String coordsString = "XYZ §f" + (inNether ? (posX + ", " + posY + ", " + posZ + " §7[§f" + anotherWorldX + ", " + anotherWorldZ + "§7]§f") : (posX + ", " + posY + ", " + posZ + "§7 [§f" + anotherWorldX + ", " + anotherWorldZ + "§7]"));

            drawText(drawContext, coordsString, (int) 2.0F, mc.getWindow().getScaledHeight() - fontHeight - (mc.currentScreen instanceof ChatScreen ? 15 : 0));
        }
    }

    private void renderPlayerRadar(DrawContext context) {
        int fontHeight = getHeight();
        int totalWidth = 70;
        if (playerRadarDistance.getValue()) totalWidth += 45;
        if (playerRadarPing.getValue()) totalWidth += 35;
        if (playerRadarHealth.getValue()) totalWidth += 40;
        int totalPlayers = 0;
        List<AbstractClientPlayerEntity> playersToShow = new ArrayList<>();
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (totalPlayers >= 6) break;
            playersToShow.add(player);
            totalPlayers++;
        }
        if (totalPlayers == 0) return;
        float bgWidth = totalWidth + 6;
        float bgHeight = (fontHeight + 1) * (totalPlayers + 1) + 7;
        RenderShaderUtil.drawBlurredShadow(context.getMatrices(), 1 - 2, 156 - 2, bgWidth + 4, bgHeight + 4, 15, new Color(0x67000000, true));
        RenderShaderUtil.drawRoundedBlur(context.getMatrices(), 1, 156, bgWidth, bgHeight, 3f);
        String nameHeader = "Player";
        drawText(context, nameHeader, 7,  160);
        if (playerRadarDistance.getValue()) {
            String distanceHeader = "Distance";
            int distanceHeaderWidth = getWidth(distanceHeader);
            int distanceHeaderX = 75 + (45 - distanceHeaderWidth) / 2;
            drawText(context, distanceHeader, distanceHeaderX,  160);
        }
        if (playerRadarPing.getValue()) {
            String pingHeader = "Ping";
            int pingHeaderWidth = getWidth(pingHeader);
            int pingHeaderX = 120 + (35 - pingHeaderWidth) / 2;
            drawText(context, pingHeader, pingHeaderX,  160);
        }
        if (playerRadarHealth.getValue()) {
            String healthHeader = "Health";
            int healthHeaderWidth = getWidth(healthHeader);
            int healthHeaderX = 155 + (40 - healthHeaderWidth) / 2;
            drawText(context, healthHeader, healthHeaderX,  160);
        }
        float lineY = 160 + fontHeight + 1;
        int offset = 1;
        for (AbstractClientPlayerEntity player : playersToShow) {
            float y = lineY + 2 + ((fontHeight + 1) * (offset - 1));
            float currentX = 5;
            if (playerIcons.getValue()) {
                Identifier headTexture;
                if (mc.getNetworkHandler() != null) {
                    PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getName().getString());
                    if (entry != null) {
                        headTexture = entry.getSkinTextures().texture();
                        PlayerSkinDrawer.draw(context, headTexture, (int) currentX + 2, (int) y, fontHeight);
                        currentX += fontHeight + 4;
                    }
                }
            }
            String playerName = player.getName().getString();
            int playerNameWidth = getWidth(playerName);
            int maxNameWidth = 70 - (playerIcons.getValue() ? (fontHeight + 4) : 0) - 4;
            if (playerNameWidth > maxNameWidth) {
                playerName = truncateTextFont(playerName, maxNameWidth);
            }
            drawText(context, playerName, (int) currentX + 2, (int) y);
            if (playerRadarDistance.getValue()) {
                String distance = "§f" + decimal.format(mc.player.distanceTo(player));
                int distanceWidth = getWidth(distance);
                int distanceX = 75 + (45 - distanceWidth) / 2;
                drawText(context, distance, distanceX, (int) y);
            }
            if (playerRadarPing.getValue()) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getName().getString());
                if (entry != null) {
                    String ping = "§f" + entry.getLatency() + "ms";
                    int pingWidth = getWidth(ping);
                    int pingX = 120 + (35 - pingWidth) / 2;
                    drawText(context, ping, pingX, (int) y);
                } else {
                    String ping = "§f-";
                    int pingWidth = getWidth(ping);
                    int pingX = 120 + (35 - pingWidth) / 2;
                    drawText(context, ping, pingX, (int) y);
                }
            }
            if (playerRadarHealth.getValue()) {
                float health = player.getHealth() + player.getAbsorptionAmount();
                String healthColor = getHealthColor(health);
                String healthStr = healthColor + decimal.format(health);
                int healthWidth = getWidth(healthStr);
                int healthX = 155 + (40 - healthWidth) / 2;
                drawText(context, healthStr, healthX, (int) y);
            }
            offset++;
        }
    }

    private String truncateTextFont(String text, int maxWidth) {
        if (getWidth(text) <= maxWidth) return text;
        StringBuilder builder = new StringBuilder(text);
        while (builder.length() > 1 && getWidth(builder.toString()) > maxWidth) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    private String getHealthColor(float health) {
        if (health <= 5) return "§c";
        if (health <= 10) return "§6";
        if (health <= 15) return "§e";
        return "§a";
    }

    private int getWidth(String s) {
        boolean useCustomFont = customFont.getValue() && !containsChinese(s) && FontRenderers.ui != null;
        if (useCustomFont) {
            return (int) FontRenderers.ui.getWidth(s);
        }
        return mc.textRenderer.getWidth(s);
    }

    private int getHeight() {
        if (customFont.getValue() && FontRenderers.ui != null) {
            return (int) FontRenderers.ui.getFontHeight();
        }
        return mc.textRenderer.fontHeight;
    }

    private void drawText(DrawContext drawContext, String S, int x, int y) {
        boolean useCustomFont = customFont.getValue() && !containsChinese(S);
        if (sync.getValue()) {
            // 使用Arraylist.INSTANCE代替ModuleList.INSTANCE，因为ModuleList模块似乎未被加载
            if (lowerCase.getValue()) {
                S = S.toLowerCase();
            }
            TextUtil.drawString(drawContext, S, x, y, Colors.INSTANCE.getColor(Arraylist.INSTANCE.counter), useCustomFont);
            return;
        }
        if (pulse.booleanValue) {
            TextUtil.drawStringPulse(drawContext, S, x, y, color.getValue(), pulse.getValue(), pulseSpeed.getValue(), pulseCounter.getValueInt(), useCustomFont);
        } else {
            TextUtil.drawString(drawContext, S, x, y, color.getValue().getRGB(), useCustomFont);
        }
    }

    private boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            if (isChinese(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }
}