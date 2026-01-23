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
import dev.kizuna.mod.modules.impl.hud.KeyDisplayHud;
import net.minecraft.client.gui.DrawContext;

public class KeyDisplayHUD
extends Tab {
    public KeyDisplayHUD() {
        this.width = 120;
        this.height = 100;
        this.x = (int)Kawaii.CONFIG.getFloat("key_display_hud_x", 277.0f);
        this.y = (int)Kawaii.CONFIG.getFloat("key_display_hud_y", 396.0f);
    }

    @Override
    public void update(double mouseX, double mouseY) {
        if (GuiManager.currentGrabbed == null && KeyDisplayHud.INSTANCE.isOn() && HudEditor.INSTANCE.isOn() && mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height) && ClickGuiScreen.clicked) {
            GuiManager.currentGrabbed = this;
        }
    }

    @Override
    public void draw(DrawContext drawContext, float partialTicks, Color color) {
        if (ClickGui.INSTANCE.isOn() && !HudEditor.INSTANCE.isOn()) {
            return;
        }
        if (KeyDisplayHud.INSTANCE.isOn()) {
            Color wColor = this.mc.options.forwardKey.isPressed() ? new Color(0x60FFFFFF, true) : new Color(0x35000000, true);
            RenderShadersUtil.drawRoundedBlur(drawContext.getMatrices(), (float)(this.x + 15 + 30) + 1.5f, this.y + 15, 30.0f, 30.0f, 10.0f, wColor, 15.0f, 0.55f);
            FontRenderers.ui2.drawCenteredString(drawContext.getMatrices(), "W", (float)(this.x + 15 + 30) + 1.5f + 15.0f, (float)(this.y + 15 + 15) - FontRenderers.ui2.getMarginHeight() / 2.0f, Color.WHITE.getRGB());
            Color aColor = this.mc.options.leftKey.isPressed() ? new Color(0x60FFFFFF, true) : new Color(0x35000000, true);
            RenderShadersUtil.drawRoundedBlur(drawContext.getMatrices(), this.x + 15, (float)(this.y + 15 + 30) + 1.5f, 30.0f, 30.0f, 10.0f, aColor, 15.0f, 0.55f);
            FontRenderers.ui2.drawCenteredString(drawContext.getMatrices(), "A", this.x + 15 + 15, (float)(this.y + 15 + 30) + 1.5f + 15.0f - FontRenderers.ui2.getMarginHeight() / 2.0f, Color.WHITE.getRGB());
            Color sColor = this.mc.options.backKey.isPressed() ? new Color(0x60FFFFFF, true) : new Color(0x35000000, true);
            RenderShadersUtil.drawRoundedBlur(drawContext.getMatrices(), (float)(this.x + 15 + 30) + 1.5f, (float)(this.y + 15 + 30) + 1.5f, 30.0f, 30.0f, 10.0f, sColor, 15.0f, 0.55f);
            FontRenderers.ui2.drawCenteredString(drawContext.getMatrices(), "S", (float)(this.x + 15 + 30) + 1.5f + 15.0f, (float)(this.y + 15 + 30) + 1.5f + 15.0f - FontRenderers.ui2.getMarginHeight() / 2.0f, Color.WHITE.getRGB());
            Color dColor = this.mc.options.rightKey.isPressed() ? new Color(0x60FFFFFF, true) : new Color(0x35000000, true);
            RenderShadersUtil.drawRoundedBlur(drawContext.getMatrices(), (float)(this.x + 15) + 63.0f, (float)(this.y + 15 + 30) + 1.5f, 30.0f, 30.0f, 10.0f, dColor, 15.0f, 0.55f);
            FontRenderers.ui2.drawCenteredString(drawContext.getMatrices(), "D", (float)(this.x + 15) + 63.0f + 15.0f, (float)(this.y + 15 + 30) + 1.5f + 15.0f - FontRenderers.ui2.getMarginHeight() / 2.0f, Color.WHITE.getRGB());
            Color spaceColor = this.mc.options.jumpKey.isPressed() ? new Color(0x60FFFFFF, true) : new Color(0x35000000, true);
            RenderShadersUtil.drawRoundedBlur(drawContext.getMatrices(), this.x + 15, (float)(this.y + 15) + 63.0f, 93.0f, 20.0f, 10.0f, spaceColor, 15.0f, 0.55f);
            FontRenderers.ui2.drawCenteredString(drawContext.getMatrices(), "SPACE", (double)(this.x + 15) + 46.5, (float)(this.y + 15) + 63.0f + 10.0f - FontRenderers.ui2.getMarginHeight() / 2.0f, Color.WHITE.getRGB());
        }
    }
}
