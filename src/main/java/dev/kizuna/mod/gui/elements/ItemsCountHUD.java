package dev.kizuna.mod.gui.elements;

import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.entity.InventoryUtil;
import dev.kizuna.api.utils.render.Render2DUtil;
import dev.kizuna.core.impl.GuiManager;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.tabs.Tab;
import dev.kizuna.mod.modules.impl.client.HUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.awt.*;

public class ItemsCountHUD extends Tab {

    public ItemsCountHUD() {
        this.width = 200;
        this.height = 20;
        this.x = (int) Kawaii.CONFIG.getFloat("items_count_x", 574);
        this.y = (int) Kawaii.CONFIG.getFloat("items_count_y", 518);
    }

    @Override
    public void update(double mouseX, double mouseY) {
        if (GuiManager.currentGrabbed == null && HUD.INSTANCE.itemsCountHud.getValue()) {
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
        if (HUD.INSTANCE.itemsCountHud.getValue()) {
            if (Kawaii.GUI.isClickGuiOpen()) {
                Render2DUtil.drawRect(matrixStack, x, y, width, height, new Color(0, 0, 0, 70));
            }
            int xOffset = 0;
            int yOffset = (int) (height / 4.5f);
            if (shouldDrawItem(Items.END_CRYSTAL)) {
                drawItemWithCount(drawContext, Items.END_CRYSTAL, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.EXPERIENCE_BOTTLE)) {
                drawItemWithCount(drawContext, Items.EXPERIENCE_BOTTLE, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.ENDER_PEARL)) {
                drawItemWithCount(drawContext, Items.ENDER_PEARL, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.OBSIDIAN)) {
                drawItemWithCount(drawContext, Items.OBSIDIAN, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.ENCHANTED_GOLDEN_APPLE)) {
                drawItemWithCount(drawContext, Items.ENCHANTED_GOLDEN_APPLE, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.TOTEM_OF_UNDYING)) {
                drawItemWithCount(drawContext, Items.TOTEM_OF_UNDYING, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.COBWEB)) {
                drawItemWithCount(drawContext, Items.COBWEB, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.RESPAWN_ANCHOR)) {
                drawItemWithCount(drawContext, Items.RESPAWN_ANCHOR, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.GLOWSTONE)) {
                drawItemWithCount(drawContext, Items.GLOWSTONE, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.PISTON)) {
                drawItemWithCount(drawContext, Items.PISTON, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.REDSTONE_BLOCK)) {
                drawItemWithCount(drawContext, Items.REDSTONE_BLOCK, xOffset, yOffset);
                xOffset += 20;
            }

            if (shouldDrawItem(Items.ENDER_CHEST)) {
                drawItemWithCount(drawContext, Items.ENDER_CHEST, xOffset, yOffset);
            }
        }
    }

    private boolean shouldDrawItem(Item item) {
        return InventoryUtil.getItemCount(item) > 0;
    }

    private void drawItemWithCount(DrawContext drawContext, Item item, int xOffset, int yOffset) {
        int count = InventoryUtil.getItemCount(item);
        if (count == 0 && HUD.INSTANCE.itemsCountHud.getValue()) {
            return;
        }

        ItemStack stack = new ItemStack(item);
        stack.setCount(Math.max(count, 1));

        int drawX = x + xOffset;
        int drawY = y + yOffset;

        drawContext.drawItem(stack, drawX, drawY);
        drawContext.drawItemInSlot(mc.textRenderer, stack, drawX, drawY);
    }
}