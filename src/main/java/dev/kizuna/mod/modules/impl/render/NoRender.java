package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.PacketEvent;
import dev.kizuna.api.events.impl.ParticleEvent;
import dev.kizuna.mod.modules.Module;
import net.minecraft.client.particle.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.util.math.Box;

public class NoRender extends Module {
	public static NoRender INSTANCE;
	public final BooleanSetting potionsIcon = add(new BooleanSetting("PotionsIcon", false));
	public final BooleanSetting weather = add(new BooleanSetting("Weather", true));
	public final BooleanSetting invisible = add(new BooleanSetting("Invisible", false));
	public final BooleanSetting potions = add(new BooleanSetting("Potions", true));
	public final BooleanSetting xp = add(new BooleanSetting("XP", true));
	public final BooleanSetting arrows = add(new BooleanSetting("Arrows", false));
	public final BooleanSetting eggs = add(new BooleanSetting("Eggs", false));
	public final BooleanSetting armor = add(new BooleanSetting("Armor", false));
	public final BooleanSetting hurtCam = add(new BooleanSetting("HurtCam", true));
	public final BooleanSetting fireOverlay = add(new BooleanSetting("FireOverlay", true));
	public final BooleanSetting waterOverlay = add(new BooleanSetting("WaterOverlay", true));
	public final BooleanSetting blockOverlay = add(new BooleanSetting("BlockOverlay", true));
	public final BooleanSetting portal = add(new BooleanSetting("Portal", true));
	public final BooleanSetting totem = add(new BooleanSetting("Totem", true));
	public final BooleanSetting nausea = add(new BooleanSetting("Nausea", true));
	public final BooleanSetting blindness = add(new BooleanSetting("Blindness", true));
	public final BooleanSetting fog = add(new BooleanSetting("Fog", false));
	public final BooleanSetting darkness = add(new BooleanSetting("Darkness", true));
	public final BooleanSetting fireEntity = add(new BooleanSetting("EntityFire", true));
	public final BooleanSetting antiTitle = add(new BooleanSetting("Title", false));
	public final BooleanSetting antiPlayerCollision = add(new BooleanSetting("PlayerCollision", true));
	public final BooleanSetting effect = add(new BooleanSetting("Effect", true));
	public final BooleanSetting elderGuardian = add(new BooleanSetting("Guardian", false));
	public final BooleanSetting explosions = add(new BooleanSetting("Explosions", true));
	public final BooleanSetting campFire = add(new BooleanSetting("CampFire", false));
	public final BooleanSetting fireworks = add(new BooleanSetting("Fireworks", false));
	public NoRender() {
		super("NoRender", Category.Render);
		setChinese("禁用渲染");
		this.setDescription("Disables all overlays and potion effects.");
		INSTANCE = this;
	}

	@EventHandler
	public void onPacketReceive(PacketEvent.Receive event){
		if(event.getPacket() instanceof TitleS2CPacket && antiTitle.getValue()){
			event.setCancelled(true);
		}
	}

	@Override
	public void onUpdate() {
		if (mc.world == null || mc.player == null) return;

		boolean removePotions = potions.getValue();
		boolean removeXp = xp.getValue();
		boolean removeArrows = arrows.getValue();
		boolean removeEggs = eggs.getValue();
		if (!removePotions && !removeXp && !removeArrows && !removeEggs) return;

		double radius = (mc.options.getClampedViewDistance() + 2) * 16.0;
		Box scanBox = mc.player.getBoundingBox().expand(radius);

		if (removePotions) {
			for (PotionEntity ent : mc.world.getEntitiesByClass(PotionEntity.class, scanBox, Entity::isAlive)) {
				mc.world.removeEntity(ent.getId(), Entity.RemovalReason.KILLED);
			}
		}
		if (removeXp) {
			for (ExperienceBottleEntity ent : mc.world.getEntitiesByClass(ExperienceBottleEntity.class, scanBox, Entity::isAlive)) {
				mc.world.removeEntity(ent.getId(), Entity.RemovalReason.KILLED);
			}
		}
		if (removeArrows) {
			for (ArrowEntity ent : mc.world.getEntitiesByClass(ArrowEntity.class, scanBox, Entity::isAlive)) {
				mc.world.removeEntity(ent.getId(), Entity.RemovalReason.KILLED);
			}
		}
		if (removeEggs) {
			for (EggEntity ent : mc.world.getEntitiesByClass(EggEntity.class, scanBox, Entity::isAlive)) {
				mc.world.removeEntity(ent.getId(), Entity.RemovalReason.KILLED);
			}
		}
	}

	@EventHandler
	public void onParticle(ParticleEvent.AddParticle event) {
		if (elderGuardian.getValue() && event.particle instanceof ElderGuardianAppearanceParticle) {
			event.setCancelled(true);
		} else if (explosions.getValue() && event.particle instanceof ExplosionLargeParticle) {
			event.setCancelled(true);
		} else if (campFire.getValue() && event.particle instanceof CampfireSmokeParticle) {
			event.setCancelled(true);
		} else if (fireworks.getValue() && (event.particle instanceof FireworksSparkParticle.FireworkParticle || event.particle instanceof FireworksSparkParticle.Flash)) {
			event.setCancelled(true);
		} else if (effect.getValue() && event.particle instanceof SpellParticle) {
			event.cancel();
		}
	}
}
