package dev.kizuna.mod.modules.impl.player;

import dev.kizuna.api.utils.entity.MovementUtil;
import dev.kizuna.mod.modules.settings.impl.BindSetting;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.PacketEvent;
import dev.kizuna.api.utils.math.*;
import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;

import java.text.DecimalFormat;

public class TimerModule extends Module {
	public final SliderSetting multiplier = add(new SliderSetting("Speed", 1, 0.1, 5, 0.01));
	public final BindSetting boostKey = add(new BindSetting("BoostKey", -1));
	public final SliderSetting boost = add(new SliderSetting("Boost", 1, 0.1, 10, 0.01));
	private final BooleanSetting tickShift = add(new BooleanSetting("TickShift", true).setParent());
	private final SliderSetting shiftTimer = add(new SliderSetting("ShiftTimer", 2, 1, 10, 0.1, tickShift::isOpen));
	private final SliderSetting accumulate = add(new SliderSetting("Charge", 2000f, 1f, 10000f, 50f, tickShift::isOpen).setSuffix("ms"));
	private final SliderSetting minAccumulate = add(new SliderSetting("MinCharge", 500f, 1f, 10000f, 50f, () -> tickShift.isOpen()).setSuffix("ms"));
	private final BooleanSetting smooth = add(new BooleanSetting("Smooth", true, tickShift::isOpen).setParent());
	private final EnumSetting<Easing> ease = add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> smooth.isOpen() && tickShift.isOpen()));
	private final BooleanSetting reset = add(new BooleanSetting("Reset", true, tickShift::isOpen));
	private final BindSetting tickshiftkey = add(new BindSetting("TickShiftKey", -1, tickShift::isOpen));
	public static TimerModule INSTANCE;
	public TimerModule() {
		super("Timer", Category.Player);
		INSTANCE = this;
	}

	@Override
	public void onDisable() {
		Kawaii.TIMER.reset();
	}

	@Override
	public void onUpdate() {
		Kawaii.TIMER.tryReset();
	}

	@Override
	public void onEnable() {
		Kawaii.TIMER.reset();
	}

	private final Timer timer = new Timer();
	private final Timer timer2 = new Timer();
	DecimalFormat df = new DecimalFormat("0.0");
	private final FadeUtils end = new FadeUtils(500);

	long lastMs = 0;
	boolean moving = false;
	@Override
	public void onRender2D(DrawContext drawContext, float tickDelta) {
		if (!tickShift.getValue()) return;
		timer.setMs(Math.min(Math.max(0, timer.getPassedTimeMs()), accumulate.getValueInt()));
		if (MovementUtil.isMoving() && !Kawaii.PLAYER.insideBlock && tickshiftkey.isPressed()) {

			if (!moving) {
				if (timer.passedMs(minAccumulate.getValue())) {
					timer2.reset();
					lastMs = timer.getPassedTimeMs();
				} else {
					lastMs = 0;
				}
				moving = true;
			}

			timer.reset();

			if (timer2.passed(lastMs)) {
				Kawaii.TIMER.reset();
			} else {
				if (smooth.getValue()) {
					double timer = Kawaii.TIMER.getDefault() + (1 - end.ease(ease.getValue())) * (shiftTimer.getValueFloat() - 1) * (lastMs / accumulate.getValue());
					Kawaii.TIMER.set((float) Math.max(Kawaii.TIMER.getDefault(), timer));
				} else {
					Kawaii.TIMER.set(shiftTimer.getValueFloat());
				}
			}
		} else {
			if (moving) {
				Kawaii.TIMER.reset();
				if (reset.getValue()) {
					timer.reset();
				} else {
					timer.setMs(Math.max(lastMs - timer2.getPassedTimeMs(), 0));
				}
				moving = false;
			}
			end.setLength(timer.getPassedTimeMs());
			end.reset();
		}
	}

	@Override
	public String getInfo() {
		if (!tickShift.getValue()) return null;
		double current = (moving ? (Math.max(lastMs - timer2.getPassedTimeMs(), 0)) : timer.getPassedTimeMs());
		double max = accumulate.getValue();
		double value = Math.min(current / max * 100, 100);
		return df.format(value) + "%";
	}

	@EventHandler
	public void onReceivePacket(PacketEvent.Receive event) {
		if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
			lastMs = 0;
		}
	}
}