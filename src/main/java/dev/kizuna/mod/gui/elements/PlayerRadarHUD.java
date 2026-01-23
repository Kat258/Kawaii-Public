package dev.kizuna.mod.gui.elements;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.render.RenderShadersUtil;
import dev.kizuna.api.utils.render.TextUtil;
import dev.kizuna.core.impl.GuiManager;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.tabs.Tab;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.impl.client.ClickGui;
import dev.kizuna.mod.modules.impl.client.Colors;
import dev.kizuna.mod.modules.impl.client.HudEditor;
import dev.kizuna.mod.modules.impl.client.ModuleList;
import dev.kizuna.mod.modules.impl.hud.PlayerRadarHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

public class PlayerRadarHUD
extends Tab {
    private final DecimalFormat decimal = new DecimalFormat("0.0");

    public PlayerRadarHUD() {
        this.width = 190;
        this.height = 80;
        this.x = (int)Kawaii.CONFIG.getFloat("playerRadar_x", 1.0f);
        this.y = (int)Kawaii.CONFIG.getFloat("playerRadar_y", 150.0f);
    }

    @Override
    public void update(double mouseX, double mouseY) {
        if (GuiManager.currentGrabbed == null && PlayerRadarHud.INSTANCE.isOn() && HudEditor.INSTANCE.isOn() && mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height) && ClickGuiScreen.clicked) {
            GuiManager.currentGrabbed = this;
        }
    }

    @Override
    public void draw(DrawContext drawContext, float partialTicks, Color color) {
        if (ClickGui.INSTANCE.isOn() && !HudEditor.INSTANCE.isOn()) {
            return;
        }
        if (PlayerRadarHud.INSTANCE.isOn() && this.mc.player != null && this.mc.world != null) {
            ArrayList<AbstractClientPlayerEntity> playersToShow = new ArrayList<AbstractClientPlayerEntity>();
            for (AbstractClientPlayerEntity player : this.mc.world.getPlayers()) {
                if (player == this.mc.player) continue;
                playersToShow.add(player);
            }
            if (!playersToShow.isEmpty()) {
                float bgWidth = 70 + (PlayerRadarHud.INSTANCE.radarDistance.getValue() ? 45 : 0) + (PlayerRadarHud.INSTANCE.radarPing.getValue() ? 35 : 0) + (PlayerRadarHud.INSTANCE.radarHealth.getValue() ? 40 : 0) + 6;
                float bgHeight = ((int)FontRenderers.ui.getFontHeight() + 1) * (playersToShow.size() + 1) + 7;
                if (HudEditor.INSTANCE.isOff() && ClickGui.INSTANCE.isOff()) {
                    RenderShadersUtil.drawBlurredShadow(drawContext.getMatrices(), this.x - 2, this.y - 2, bgWidth + 4.0f, bgHeight + 4.0f, 15, new Color(0x77000000, true));
                }
                RenderShadersUtil.drawRoundedBlur(drawContext.getMatrices(), this.x, this.y, bgWidth, bgHeight, 3.0f, new Color(0x35000000, true), 15.0f, 0.55f);
                this.drawText(drawContext, "Player", this.x + 7, this.y + 4);
                int currentHeaderX = this.x + 75;
                if (PlayerRadarHud.INSTANCE.radarDistance.getValue()) {
                    String distanceHeader = "Distance";
                    this.drawText(drawContext, distanceHeader, currentHeaderX + (45 - (int)FontRenderers.ui.getWidth(distanceHeader)) / 2, this.y + 4);
                    currentHeaderX += 45;
                }
                if (PlayerRadarHud.INSTANCE.radarPing.getValue()) {
                    String pingHeader = "Ping";
                    this.drawText(drawContext, pingHeader, currentHeaderX + (35 - (int)FontRenderers.ui.getWidth(pingHeader)) / 2, this.y + 4);
                    currentHeaderX += 35;
                }
                if (PlayerRadarHud.INSTANCE.radarHealth.getValue()) {
                    String healthHeader = "Health";
                    this.drawText(drawContext, healthHeader, currentHeaderX + (40 - (int)FontRenderers.ui.getWidth(healthHeader)) / 2, this.y + 4);
                }
                for (int i = 0; i < playersToShow.size(); ++i) {
                    PlayerListEntry entry;
                    AbstractClientPlayerEntity player = (AbstractClientPlayerEntity)playersToShow.get(i);
                    float currentY = this.y + (int)FontRenderers.ui.getFontHeight() + 5 + 2 + ((int)FontRenderers.ui.getFontHeight() + 1) * i;
                    float currentX = this.x + 5;
                    if (PlayerRadarHud.INSTANCE.icons.getValue() && this.mc.getNetworkHandler() != null && (entry = this.mc.getNetworkHandler().getPlayerListEntry(player.getName().getString())) != null) {
                        PlayerSkinDrawer.draw((DrawContext)drawContext, (Identifier)entry.getSkinTextures().texture(), (int)((int)currentX + 2), (int)((int)currentY), (int)((int)FontRenderers.ui.getFontHeight()));
                        currentX += (float)((int)FontRenderers.ui.getFontHeight() + 4);
                    }
                    String playerName = player.getName().getString();
                    int maxNameWidth = 70 - (PlayerRadarHud.INSTANCE.icons.getValue() ? (int)FontRenderers.ui.getFontHeight() + 4 : 0) - 4;
                    String truncatedName = playerName;
                    if ((int)FontRenderers.ui.getWidth(playerName) > maxNameWidth) {
                        StringBuilder builder = new StringBuilder(playerName);
                        while (builder.length() > 1 && (int)FontRenderers.ui.getWidth(builder.toString()) > maxNameWidth) {
                            builder.setLength(builder.length() - 1);
                        }
                        truncatedName = builder.toString();
                    }
                    this.drawText(drawContext, truncatedName, (int)currentX + 2, (int)currentY);
                    currentX = this.x + 75;
                    if (PlayerRadarHud.INSTANCE.radarDistance.getValue()) {
                        String distance = "\u00a7f" + this.decimal.format(this.mc.player.distanceTo((Entity)player));
                        this.drawText(drawContext, distance, (int)(currentX + (float)(45 - (int)FontRenderers.ui.getWidth(distance)) / 2.0f), (int)currentY);
                        currentX += 45.0f;
                    }
                    if (PlayerRadarHud.INSTANCE.radarPing.getValue()) {
                        PlayerListEntry entry2 = this.mc.getNetworkHandler().getPlayerListEntry(player.getName().getString());
                        String pingText = "\u00a7f" + (String)(entry2 != null ? entry2.getLatency() + "ms" : "-");
                        this.drawText(drawContext, pingText, (int)(currentX + (float)(35 - (int)FontRenderers.ui.getWidth(pingText)) / 2.0f), (int)currentY);
                        currentX += 35.0f;
                    }
                    if (!PlayerRadarHud.INSTANCE.radarHealth.getValue()) continue;
                    float health = player.getHealth() + player.getAbsorptionAmount();
                    String healthStr = this.getHealthColor(health) + this.decimal.format(health);
                    this.drawText(drawContext, healthStr, (int)(currentX + (float)(40 - (int)FontRenderers.ui.getWidth(healthStr)) / 2.0f), (int)currentY);
                }
            }
        }
    }

    private String getHealthColor(float health) {
        if (health <= 5.0f) {
            return "\u00a7c";
        }
        if (health <= 10.0f) {
            return "\u00a76";
        }
        if (health <= 15.0f) {
            return "\u00a7e";
        }
        return "\u00a7a";
    }

    private void drawText(DrawContext drawContext, String s, int x, int y) {
        boolean useCustomFont = !this.containsChinese(s);
        --ModuleList.INSTANCE.counter;
        TextUtil.drawString(drawContext, s, x, y, Colors.INSTANCE.getColor(ModuleList.INSTANCE.counter), useCustomFont);
    }

    private boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            if (!this.isChinese(c)) continue;
            return true;
        }
        return false;
    }

    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }
}
