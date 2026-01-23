package dev.kizuna.mod.gui.elements;

import java.awt.Color;
import java.util.Objects;
import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.entity.EntityUtil;
import dev.kizuna.api.utils.render.ColorUtil;
import dev.kizuna.api.utils.render.Render2DUtil;
import dev.kizuna.api.utils.render.TextUtil;
import dev.kizuna.core.impl.GuiManager;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.tabs.Tab;
import dev.kizuna.mod.modules.impl.client.ClickGui;
import dev.kizuna.mod.modules.impl.client.HudEditor;
import dev.kizuna.mod.modules.impl.hud.ArmorHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class ArmorHUD
extends Tab {
    public ArmorHUD() {
        this.width = 80;
        this.height = 34;
        this.x = (int)Kawaii.CONFIG.getFloat("armor_x", 0.0f);
        this.y = (int)Kawaii.CONFIG.getFloat("armor_y", 200.0f);
    }

    @Override
    public void update(double mouseX, double mouseY) {
        if (GuiManager.currentGrabbed == null && ArmorHud.INSTANCE.isOn() && HudEditor.INSTANCE.isOn() && mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height) && ClickGuiScreen.clicked) {
            GuiManager.currentGrabbed = this;
        }
    }

    @Override
    public void draw(DrawContext drawContext, float partialTicks, Color color) {
        MatrixStack matrixStack = drawContext.getMatrices();
        if (ClickGui.INSTANCE.isOn() && !HudEditor.INSTANCE.isOn()) {
            return;
        }
        if (ArmorHud.INSTANCE.isOn()) {
            if (HudEditor.INSTANCE.isOn()) {
                Render2DUtil.drawRect(drawContext.getMatrices(), (float)this.x, (float)this.y, (float)this.width, (float)this.height, new Color(0, 0, 0, 70));
            }
            int xOff = 0;
            for (ItemStack armor : this.mc.player.getInventory().armor) {
                xOff += 20;
                if (armor.isEmpty()) continue;
                matrixStack.push();
                int damage = EntityUtil.getDamagePercent(armor);
                int yOffset = this.height / 2;
                drawContext.drawItem(armor, this.x + this.width - xOff, this.y + yOffset);
                drawContext.drawItemInSlot(this.mc.textRenderer, armor, this.x + this.width - xOff, this.y + yOffset);
                String string = damage + "%";
                float f = this.x + this.width + 2 - xOff;
                double d = this.y + yOffset;
                Objects.requireNonNull(this.mc.textRenderer);
                TextUtil.drawStringScale(drawContext, string, f, (float)(d - 9.0 / 4.0), ColorUtil.fadeColor(new Color(196, 0, 0), new Color(0, 227, 0), (float)damage / 100.0f).getRGB(), 0.5f);
                matrixStack.pop();
            }
        }
    }
}
