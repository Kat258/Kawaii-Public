package dev.kizuna.mod.modules.settings.impl;

import dev.kizuna.Kawaii;
import dev.kizuna.core.impl.ModuleManager;
import dev.kizuna.mod.modules.settings.Setting;

import java.util.function.BooleanSupplier;

public class BooleanSetting extends Setting {
	public boolean parent = false;
	public boolean parent2 = false;
	public boolean popped = false;
	public boolean popped2 = false;
	public Runnable task = null;
	public boolean injectTask = false;
	private boolean value;
	public final boolean defaultValue;

	public BooleanSetting(String name, boolean defaultValue) {
		super(name, ModuleManager.lastLoadMod.getName() + "_" + name);
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public BooleanSetting(String name, boolean defaultValue, BooleanSupplier visibilityIn) {
		super(name, ModuleManager.lastLoadMod.getName() + "_" + name, visibilityIn);
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public final boolean getValue() {
		return this.value;
	}
	
	public final void setValue(boolean value) {
		if (injectTask && value != this.value) {
			task.run();
		}
		this.value = value;
	}
	
	public final void toggleValue() {
		setValue(!value);
	}
	public final boolean isOpen() {
		if (parent) {
			return popped;
		} else {
			return true;
		}
	}
	public final boolean isOpen2() {
		if (parent2) {
			return popped2;
		} else {
			return true;
		}
	}
	@Override
	public void loadSetting() {
		this.value = Kawaii.CONFIG.getBoolean(this.getLine(), defaultValue);
	}

	public BooleanSetting setParent() {
		parent = true;
		return this;
	}
	public BooleanSetting setParent2() {
		parent2 = true;
		return this;
	}
	public BooleanSetting injectTask(Runnable task) {
		this.task = task;
		injectTask = true;
		return this;
	}
}
