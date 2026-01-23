package dev.kizuna.core.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;
import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.Wrapper;
import dev.kizuna.api.utils.math.FadeUtils;
import dev.kizuna.api.utils.render.Snow;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.components.impl.ModuleComponent;
import dev.kizuna.mod.gui.clickgui.tabs.ClickGuiTab;
import dev.kizuna.mod.gui.clickgui.tabs.Tab;
import dev.kizuna.mod.gui.elements.ArmorHUD;
import dev.kizuna.mod.gui.elements.InventoryHUD;
import dev.kizuna.mod.gui.elements.ItemsCountHUD;
import dev.kizuna.mod.gui.elements.NotificationHUD;
import dev.kizuna.mod.gui.elements.KeyDisplayHUD;
import dev.kizuna.mod.gui.elements.PlayerRadarHUD;
import dev.kizuna.mod.gui.elements.PotionHUD;
import dev.kizuna.mod.gui.elements.SelfHUD;
import dev.kizuna.mod.gui.elements.TargetHUD;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.impl.client.ClickGui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class GuiManager
        implements Wrapper {
    public final ArrayList<ClickGuiTab> tabs = new ArrayList();
    public static final ClickGuiScreen clickGui = new ClickGuiScreen();
    public final ArmorHUD armorHud = new ArmorHUD();
    public final InventoryHUD inventoryHud = new InventoryHUD();
    public final ItemsCountHUD itemsCountHud = new ItemsCountHUD();
    public final KeyDisplayHUD keyDisplayHud = new KeyDisplayHUD();
    public final PlayerRadarHUD playerRadarHud = new PlayerRadarHUD();
    public final PotionHUD potionHud;
    public final NotificationHUD notificationHud;
    public final TargetHUD targetHud = new TargetHUD();
    public final SelfHUD selfHud = new SelfHUD();
    public static Tab currentGrabbed = null;
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    private int mouseX;
    private int mouseY;
    public static final ArrayList<Snow> snows = new ArrayList<Snow>(){
        {
            Random random = new Random();
            for (int i = 0; i < 100; ++i) {
                for (int y = 0; y < 3; ++y) {
                    this.add(new Snow(25 * i, y * -50, random.nextInt(3) + 1, random.nextInt(2) + 1));
                }
            }
        }
    };

    public GuiManager() {
        notificationHud = new NotificationHUD();
        this.potionHud = new PotionHUD();
        int xOffset = 30;
        for (Module.Category category : Module.Category.values()) {
            ClickGuiTab tab = new ClickGuiTab(category, xOffset, 50);
            for (Module module : Kawaii.MODULE.modules) {
                if (module.getCategory() != category) continue;
                ModuleComponent button = new ModuleComponent(tab, module);
                tab.addChild(button);
            }
            this.tabs.add(tab);
            xOffset += tab.getWidth() + 5;
        }
    }

    public Color getColor() {
        return ClickGui.INSTANCE.color.getValue();
    }

    public void onUpdate() {
        if (this.isClickGuiOpen()) {
            for (ClickGuiTab tab : this.tabs) {
                tab.update(this.mouseX, this.mouseY);
            }
            this.armorHud.update(this.mouseX, this.mouseY);
            this.inventoryHud.update(this.mouseX, this.mouseY);
            this.itemsCountHud.update(this.mouseX, this.mouseY);
            this.keyDisplayHud.update(this.mouseX, this.mouseY);
            this.playerRadarHud.update(this.mouseX, this.mouseY);
            this.targetHud.update(this.mouseX, this.mouseY);
            this.selfHud.update(this.mouseX, this.mouseY);
            this.potionHud.update(this.mouseX, this.mouseY);
        }
    }

    public void draw(int x, int y, DrawContext drawContext, float tickDelta) {
        MatrixStack matrixStack = drawContext.getMatrices();
        boolean mouseClicked = ClickGuiScreen.clicked;
        this.mouseX = x;
        this.mouseY = y;
        if (!mouseClicked) {
            currentGrabbed = null;
        }
        if (currentGrabbed != null) {
            currentGrabbed.moveWindow(this.lastMouseX - this.mouseX, this.lastMouseY - this.mouseY);
        }
        this.lastMouseX = this.mouseX;
        this.lastMouseY = this.mouseY;
        RenderSystem.enableCull();
        matrixStack.push();
        this.armorHud.draw(drawContext, tickDelta, this.getColor());
        this.inventoryHud.draw(drawContext, tickDelta, this.getColor());
        this.itemsCountHud.draw(drawContext, tickDelta, this.getColor());
        this.keyDisplayHud.draw(drawContext, tickDelta, this.getColor());
        this.playerRadarHud.draw(drawContext, tickDelta, this.getColor());
        this.targetHud.draw(drawContext, tickDelta, this.getColor());
        this.selfHud.draw(drawContext, tickDelta, this.getColor());
        this.potionHud.draw(drawContext, tickDelta, this.getColor());
        double quad = ClickGui.fade.ease(FadeUtils.Ease.In2);
        if (quad < 1.0) {
            switch (ClickGui.INSTANCE.mode.getValue()) {
                case Pull: {
                    quad = 1.0 - quad;
                    matrixStack.translate(0.0, -100.0 * quad, 0.0);
                    break;
                }
                case Scale: {
                    matrixStack.scale((float)quad, (float)quad, 1.0f);
                }
            }
        }
        for (ClickGuiTab tab : this.tabs) {
            tab.draw(drawContext, tickDelta, this.getColor());
        }
        matrixStack.pop();
    }

    public boolean isClickGuiOpen() {
        return GuiManager.mc.currentScreen instanceof ClickGuiScreen;
    }
}
