package dev.kizuna.mod.gui.clickgui.components.impl;

import dev.kizuna.mod.modules.impl.client.ClickGui;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.core.impl.GuiManager;
import dev.kizuna.api.utils.render.Render2DUtil;
import dev.kizuna.api.utils.render.TextUtil;
import dev.kizuna.mod.gui.clickgui.components.Component;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.tabs.ClickGuiTab;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class BooleanComponent extends Component {

    final BooleanSetting setting;

    public BooleanComponent(ClickGuiTab parent, BooleanSetting setting) {
        super();
        this.parent = parent;
        this.setting = setting;
    }

    @Override
    public boolean isVisible() {
        if (setting.visibility != null) {
            return setting.visibility.getAsBoolean();
        }
        return true;
    }

    boolean hover = false;

    @Override
    
    
    public void update(int offset, double mouseX, double mouseY) {
        int parentX = parent.getX();
        int parentY = parent.getY();
        int parentWidth = parent.getWidth();
        if (GuiManager.currentGrabbed == null && isVisible() && (mouseX >= ((parentX + 1)) && mouseX <= (((parentX)) + parentWidth - 1)) && (mouseY >= (((parentY + offset))) && mouseY <= ((parentY + offset) + defaultHeight - 2))) {
            hover = true;
            if (ClickGuiScreen.clicked) {
                ClickGuiScreen.clicked = false;
                sound();
                setting.toggleValue();
            }
            if (ClickGuiScreen.rightClicked) {
                ClickGuiScreen.rightClicked = false;
                sound();
                setting.popped = !setting.popped;
                setting.popped2 = !setting.popped2;
            }
        } else {
            hover = false;
        }
    }

    public double currentWidth = 0;

    @Override
    
    
    public boolean draw(int offset, DrawContext drawContext, float partialTicks, Color color, boolean back) {
        int x = parent.getX();
        int y = parent.getY() + offset - 2;
        int width = parent.getWidth();
        MatrixStack matrixStack = drawContext.getMatrices();
        Render2DUtil.drawRect(matrixStack, (float) x + 1, (float) y + 1, (float) width - 2, (float) defaultHeight - (ClickGui.INSTANCE.maxFill.getValue() ? 0 : 1), hover ? ClickGui.INSTANCE.settingHover.getValue() : ClickGui.INSTANCE.setting.getValue());

        currentWidth = animation.get(setting.getValue() ? (width - 2D) : 0D);
        switch (ClickGui.INSTANCE.uiType.getValue()) {
            case New, Old -> TextUtil.drawString(drawContext, setting.getName(), x + 5, y + getTextOffsetY(), setting.getValue() ? ClickGui.INSTANCE.enableText.getValue() : ClickGui.INSTANCE.disableText.getValue());
        }

        if (setting.parent) {
            TextUtil.drawString(drawContext, setting.popped ? "-" : "+", x + width - 11,
                    y + getTextOffsetY(), new Color(255, 255, 255).getRGB());
        }
        if (setting.parent2) {
            TextUtil.drawString(drawContext, "...", x + width - 11,
                    y + getTextOffsetY(), new Color(255, 255, 255).getRGB());
        }
        return true;
    }
}
