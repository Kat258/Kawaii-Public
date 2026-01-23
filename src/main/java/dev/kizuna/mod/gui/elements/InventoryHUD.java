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
import dev.kizuna.mod.modules.impl.hud.InventoryHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class InventoryHUD
extends Tab {
    public InventoryHUD() {
        this.width = 170;
        this.height = 72;
        this.x = (int)Kawaii.CONFIG.getFloat("inventory_hud_x", 593.0f);
        this.y = (int)Kawaii.CONFIG.getFloat("inventory_hud_y", 6.0f);
    }

    @Override
    public void update(double mouseX, double mouseY) {
        if (GuiManager.currentGrabbed == null && InventoryHud.INSTANCE.isOn() && HudEditor.INSTANCE.isOn() && mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height) && ClickGuiScreen.clicked) {
            GuiManager.currentGrabbed = this;
        }
    }

    @Override
    public void draw(DrawContext drawContext, float partialTicks, Color color) {
        if (ClickGui.INSTANCE.isOn() && !HudEditor.INSTANCE.isOn()) {
            return;
        }
        if (InventoryHud.INSTANCE.isOn() && this.mc.player != null) {
            this.renderInventoryHud(drawContext);
        }
    }

    private void renderInventoryHud(DrawContext drawContext) {
        RenderShadersUtil.drawRoundedBlur(drawContext.getMatrices(), this.x, this.y, this.width, this.height, 9.0f, new Color(0x35000000, true), 15.0f, 0.55f);
        if (HudEditor.INSTANCE.isOff() && ClickGui.INSTANCE.isOff()) {
            RenderShadersUtil.drawBlurredShadow(drawContext.getMatrices(), this.x, this.y, this.width, this.height, 10, new Color(0x54000000, true));
        }
        FontRenderers.icon2.drawString(drawContext.getMatrices(), "e", this.x + 5, this.y + 4, Color.WHITE.getRGB());
        FontRenderers.ui2.drawString(drawContext.getMatrices(), "Inventory: ", this.x + 21, this.y + 5, Color.WHITE.getRGB());
        DefaultedList mainInventory = this.mc.player.getInventory().main;
        MatrixStack matrices = drawContext.getMatrices();
        int startX = this.x + 5;
        int startY = this.y + 15;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slotIndex = row * 9 + col + 9;
                if (slotIndex >= mainInventory.size()) continue;
                ItemStack stack = (ItemStack)mainInventory.get(slotIndex);
                int xPos = startX + col * 18;
                int yPos = startY + row * 18;
                if (stack.isEmpty()) continue;
                matrices.push();
                matrices.translate(0.0f, 0.0f, 200.0f);
                drawContext.drawItem(stack, xPos, yPos);
                drawContext.drawItemInSlot(this.mc.textRenderer, stack, xPos, yPos);
                matrices.pop();
            }
        }
    }
}
