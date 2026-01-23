package dev.kizuna.mod.gui.elements;

import java.awt.Color;
import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.entity.InventoryUtil;
import dev.kizuna.api.utils.render.Render2DUtil;
import dev.kizuna.core.impl.GuiManager;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.tabs.Tab;
import dev.kizuna.mod.modules.impl.client.ClickGui;
import dev.kizuna.mod.modules.impl.client.HudEditor;
import dev.kizuna.mod.modules.impl.hud.ItemsCountHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ItemsCountHUD
extends Tab {
    public ItemsCountHUD() {
        this.width = 200;
        this.height = 20;
        this.x = (int)Kawaii.CONFIG.getFloat("items_count_x", 574.0f);
        this.y = (int)Kawaii.CONFIG.getFloat("items_count_y", 518.0f);
    }

    @Override
    public void update(double mouseX, double mouseY) {
        if (GuiManager.currentGrabbed == null && ItemsCountHud.INSTANCE.isOn() && HudEditor.INSTANCE.isOn() && mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height) && ClickGuiScreen.clicked) {
            GuiManager.currentGrabbed = this;
        }
    }

    @Override
    public void draw(DrawContext drawContext, float partialTicks, Color color) {
        MatrixStack matrixStack = drawContext.getMatrices();
        if (ClickGui.INSTANCE.isOn() && !HudEditor.INSTANCE.isOn()) {
            return;
        }
        if (ItemsCountHud.INSTANCE.isOn()) {
            if (HudEditor.INSTANCE.isOn()) {
                Render2DUtil.drawRect(matrixStack, (float)this.x, (float)this.y, (float)this.width, (float)this.height, new Color(0, 0, 0, 70));
            }
            int xOffset = 0;
            int yOffset = (int)((float)this.height / 4.5f);
            if (this.shouldDrawItem(Items.END_CRYSTAL)) {
                this.drawItemWithCount(drawContext, Items.END_CRYSTAL, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.EXPERIENCE_BOTTLE)) {
                this.drawItemWithCount(drawContext, Items.EXPERIENCE_BOTTLE, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.ENDER_PEARL)) {
                this.drawItemWithCount(drawContext, Items.ENDER_PEARL, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.OBSIDIAN)) {
                this.drawItemWithCount(drawContext, Items.OBSIDIAN, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.ENCHANTED_GOLDEN_APPLE)) {
                this.drawItemWithCount(drawContext, Items.ENCHANTED_GOLDEN_APPLE, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.TOTEM_OF_UNDYING)) {
                this.drawItemWithCount(drawContext, Items.TOTEM_OF_UNDYING, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.COBWEB)) {
                this.drawItemWithCount(drawContext, Items.COBWEB, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.RESPAWN_ANCHOR)) {
                this.drawItemWithCount(drawContext, Items.RESPAWN_ANCHOR, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.GLOWSTONE)) {
                this.drawItemWithCount(drawContext, Items.GLOWSTONE, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.PISTON)) {
                this.drawItemWithCount(drawContext, Items.PISTON, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.REDSTONE_BLOCK)) {
                this.drawItemWithCount(drawContext, Items.REDSTONE_BLOCK, xOffset, yOffset);
                xOffset += 20;
            }
            if (this.shouldDrawItem(Items.ENDER_CHEST)) {
                this.drawItemWithCount(drawContext, Items.ENDER_CHEST, xOffset, yOffset);
            }
        }
    }

    private boolean shouldDrawItem(Item item) {
        return InventoryUtil.getItemCount(item) > 0;
    }

    private void drawItemWithCount(DrawContext drawContext, Item item, int xOffset, int yOffset) {
        int count = InventoryUtil.getItemCount(item);
        if (count == 0 && ItemsCountHud.INSTANCE.isOn()) {
            return;
        }
        ItemStack stack = new ItemStack((ItemConvertible)item);
        stack.setCount(Math.max(count, 1));
        int drawX = this.x + xOffset;
        int drawY = this.y + yOffset;
        drawContext.drawItem(stack, drawX, drawY);
        drawContext.drawItemInSlot(this.mc.textRenderer, stack, drawX, drawY);
    }
}
