package dev.kizuna.mod.modules.impl.misc;

import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.PlaySoundEvent;
import dev.kizuna.mod.modules.Module;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

public class NoSoundLag extends Module {
	public static NoSoundLag INSTANCE;
	public NoSoundLag() {
		super("NoSoundLag", Category.Misc);
		setChinese("去除声音");
		INSTANCE = this;
	}
	private final BooleanSetting equip =
			add(new BooleanSetting("ArmorEquip", true));
	private final BooleanSetting explode =
			add(new BooleanSetting("Explode", true));
	private final BooleanSetting attack =
			add(new BooleanSetting("Attack", true));
	static final ArrayList<Identifier> armor = new ArrayList<>();
	@EventHandler
	public void onPlaySound(PlaySoundEvent event){
		if (equip.getValue()) {
			for (Identifier se : armor) {
				if (event.sound.getId().equals(se)) {
					event.cancel();
					return;
				}
			}
		}
		if (explode.getValue()) {
			if (event.sound.getId().equals(Identifier.of("minecraft", "entity.generic.explode"))
					|| event.sound.getId().equals(Identifier.of("minecraft", "entity.dragon_fireball.explode"))) {
				event.cancel();
				return;
			}
		}
		if (attack.getValue()) {
			if (event.sound.getId().equals(Identifier.of("minecraft", "entity.player.attack.weak"))
					|| event.sound.getId().equals(Identifier.of("minecraft", "entity.player.attack.strong"))) {
				event.cancel();
            }
		}
	}

	static {
		armor.add(Identifier.of("minecraft", "item.armor.equip_netherite"));
		armor.add(Identifier.of("minecraft", "item.armor.equip_turtle"));
		armor.add(Identifier.of("minecraft", "item.armor.equip_chain"));
		armor.add(Identifier.of("minecraft", "item.armor.equip_elytra"));
		armor.add(Identifier.of("minecraft", "item.armor.equip_diamond"));
		armor.add(Identifier.of("minecraft", "item.armor.equip_gold"));
		armor.add(Identifier.of("minecraft", "item.armor.equip_iron"));
		armor.add(Identifier.of("minecraft", "item.armor.equip_leather"));
		armor.add(Identifier.of("minecraft", "item.armor.equip_generic"));
	}
}
