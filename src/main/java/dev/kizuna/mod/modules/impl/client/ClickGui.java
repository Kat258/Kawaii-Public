package dev.kizuna.mod.modules.impl.client;

import dev.kizuna.Kawaii;
import dev.kizuna.core.impl.GuiManager;
import dev.kizuna.api.utils.math.Easing;
import dev.kizuna.api.utils.math.FadeUtils;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.gui.clickgui.components.Component;
import dev.kizuna.mod.gui.clickgui.components.impl.ModuleComponent;
import dev.kizuna.mod.gui.clickgui.tabs.ClickGuiTab;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.*;

import java.awt.*;

public class ClickGui extends Module {
	public static ClickGui INSTANCE;
	public final EnumSetting<Mode> mode = add(new EnumSetting<>("EnableAnim", Mode.Pull));
	public final BooleanSetting customFont = add(new BooleanSetting("CustomFont", false));
	public final SliderSetting height = add(new SliderSetting("Height", 13, 10, 20, 1));
	public final SliderSetting animationTime = add(new SliderSetting("AnimSpeed", 530, 0, 1000, 1));
	//Element
	private final BooleanSetting element = add(new BooleanSetting("Element",true).setParent2());
	public final EnumSetting<Type> uiType = add(new EnumSetting<>("UIType", Type.Old,element::isOpen2));
	public final BooleanSetting activeBox = add(new BooleanSetting("ActiveBox", false,element::isOpen2));
	public final BooleanSetting center = add(new BooleanSetting("Center", false,element::isOpen2));
	//General
	private final BooleanSetting general = add(new BooleanSetting("General",true).setParent2());
	public final BooleanSetting chinese = add(new BooleanSetting("Chinese", false,general::isOpen2));
	public final BooleanSetting maxFill = add(new BooleanSetting("MaxFill", false,general::isOpen2));
	public final BooleanSetting sound = add(new BooleanSetting("Sound", true,general::isOpen2));
	public final EnumSetting<Easing> ease = add(new EnumSetting<>("Ease", Easing.QuadInOut,general::isOpen2));
	//Text
	private final BooleanSetting text = add(new BooleanSetting("Text",true).setParent2());
	public final ColorSetting disableText = add(new ColorSetting("TextColor", 16777215,text::isOpen2));
	public final ColorSetting enableText = add(new ColorSetting("EnableText", -8673025,text::isOpen2));
	//Color
	private final BooleanSetting colors = add(new BooleanSetting("Colors",true).setParent2());
	public final ColorSetting bgEnable = add(new ColorSetting("BgEnable", 2088478975,colors::isOpen2));
	public final ColorSetting color = add(new ColorSetting("Main", 1669048575,colors::isOpen2));
	public final ColorSetting mainHover = add(new ColorSetting("Hover", 930851071,colors::isOpen2));
	public final ColorSetting bar = add(new ColorSetting("Bar", -8673025,colors::isOpen2));
	public final ColorSetting module = add(new ColorSetting("Module", 1669048575,colors::isOpen2));
	public final ColorSetting moduleHover = add(new ColorSetting("ModuleHover", 1669048575,colors::isOpen2));
	public final ColorSetting setting = add(new ColorSetting("Setting", 1669048575,colors::isOpen2));
	public final ColorSetting setting2 = add(new ColorSetting("Setting2", 1669048575,colors::isOpen2));
	public final ColorSetting settingHover = add(new ColorSetting("SettingHover", 1669048575,colors::isOpen2));
	public final ColorSetting background = add(new ColorSetting("Background", 1040187392,colors::isOpen2));
	public final ColorSetting bind = add(new ColorSetting("Bind", new Color(255, 255, 255),colors::isOpen2).injectBoolean(false));
	public final ColorSetting gear = add(new ColorSetting("Gear", new Color(255, 255, 255),colors::isOpen2).injectBoolean(true));
	public ClickGui() {
		super("ClickGui", Category.Client);
		setChinese("菜单");
		INSTANCE = this;
	}

	public static final FadeUtils fade = new FadeUtils(300);

	@Override
	public void onUpdate() {
		if (chinese.getValue()) {
			customFont.setValue(false);
		}
		if (!(mc.currentScreen instanceof ClickGuiScreen)) {
			disable();
		}
	}

	int lastHeight;
	@Override
	public void onEnable() {
		//size = scale.getValue();
		if (lastHeight != height.getValueInt()) {
			for (ClickGuiTab tab : Kawaii.GUI.tabs) {
				for (Component component : tab.getChildren()) {
					if (component instanceof ModuleComponent moduleComponent) {
						for (Component settingComponent : moduleComponent.getSettingsList()) {
							settingComponent.setHeight(height.getValueInt());
							settingComponent.defaultHeight = height.getValueInt();
						}
					}
					component.setHeight(height.getValueInt());
					component.defaultHeight = height.getValueInt();
				}
			}
			lastHeight = height.getValueInt();
		}
		fade.reset();
		if (nullCheck()) {
			disable();
			return;
		}
		mc.setScreen(GuiManager.clickGui);
	}

	@Override
	public void onDisable() {
		if (mc.currentScreen instanceof ClickGuiScreen) {
			mc.setScreen(null);
		}
	}

	public enum Mode {
		Scale, Pull, None
	}

	public enum Type {
		Old,
		New
	}
}