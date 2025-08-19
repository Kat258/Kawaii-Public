package dev.kizuna.mod.modules.impl.player;

import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.combat.CombatUtil;
import dev.kizuna.api.utils.entity.EntityUtil;
import dev.kizuna.api.utils.entity.InventoryUtil;
import dev.kizuna.api.utils.math.Timer;
import dev.kizuna.api.utils.world.BlockPosX;
import dev.kizuna.mod.gui.clickgui.ClickGuiScreen;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.impl.client.AntiCheat;
import dev.kizuna.mod.modules.impl.combat.KawaiiAura;
import dev.kizuna.mod.modules.settings.impl.BindSetting;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.Hand;

import java.util.List;

public class AutoPot extends Module {

    public static AutoPot INSTANCE;

    private final SliderSetting range = add(new SliderSetting("Range", 6.0, 1, 15.0));
    private final SliderSetting delay = add(new SliderSetting("Delay", 5, 0, 10).setSuffix("s"));
    private final BooleanSetting speed = add(new BooleanSetting("Speed", true));
    private final BooleanSetting resistance = add(new BooleanSetting("Resistance", true));
    private final BooleanSetting slowFalling = add(new BooleanSetting("SlowFalling", true));
    private final BooleanSetting usingPause = add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting onlyGround = add(new BooleanSetting("OnlyGround", true));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting noCheck = add(new BooleanSetting("NoCheck", false));
    private final BooleanSetting pauseModule = add(new BooleanSetting("PauseModule", true));

    private final BooleanSetting single = add(new BooleanSetting("Single", false));
    public final BindSetting boostKey = add(new BindSetting("BoostKey", -1));

    private final BooleanSetting sneak = add(new BooleanSetting("Sneak", true));
    private final SliderSetting duration = add(new SliderSetting("Duration", 100, 0, 10000).setSuffix("ms"));
    private final SliderSetting throwDelay = add(new SliderSetting("ThrowDelay", 50, 0, 2000).setSuffix("ms"));

    private final Timer delayTimer = new Timer();
    private final Timer sneakTimer = new Timer();

    public AutoPot() {
        super("AutoPot", Category.Player);
        INSTANCE = this;
    }

    private boolean throwing = false;
    private boolean sneakingFromAuto = false;
    private boolean boostPrev = false;

    private long throwStart = 0L;
    private StatusEffect pendingEffect = null;

    @Override
    public void onDisable() {
        throwing = false;
        if (sneakingFromAuto && mc.player != null) {
            try {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            } catch (Throwable ignored) {}
            sneakingFromAuto = false;
        }
        pendingEffect = null;
        throwStart = 0L;
        boostPrev = false;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;

        if (sneakingFromAuto) {
            long durMs = (long) duration.getValue();
            if (durMs > 0 && sneakTimer.passedMs(durMs)) {
                try {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                } catch (Throwable ignored) {}
                sneakingFromAuto = false;
            }
        }

        if (single.getValue()) {
            boolean pressed = false;
            try { pressed = boostKey.isPressed(); } catch (Throwable ignored) {}

            if (pressed && !boostPrev) {
                if (delayTimer.passedMs((long) (delay.getValue() * 1000))) {
                    if (speed.getValue() && (noCheck.getValue() || !mc.player.hasStatusEffect(StatusEffects.SPEED))) {
                        throwing = checkThrow(StatusEffects.SPEED);
                        if (isThrow()) {
                            if (throwPotion(StatusEffects.SPEED)) delayTimer.reset();
                        }
                    }
                    if (!throwing && resistance.getValue() && (noCheck.getValue() || (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2))) {
                        throwing = checkThrow(StatusEffects.RESISTANCE);
                        if (isThrow()) {
                            if (throwPotion(StatusEffects.RESISTANCE)) delayTimer.reset();
                        }
                    }
                }
            }
            boostPrev = pressed;
            return;
        }

        if (!onlyGround.getValue() || (mc.player.isOnGround() && !mc.world.isAir(new BlockPosX(mc.player.getPos().add(0, -1, 0))))) {
            if (speed.getValue() && (noCheck.getValue() || !mc.player.hasStatusEffect(StatusEffects.SPEED))) {
                throwing = checkThrow(StatusEffects.SPEED);
                if (isThrow() && delayTimer.passedMs((long) (delay.getValue() * 1000))) {
                    if (throwPotion(StatusEffects.SPEED)) {
                        delayTimer.reset();
                        return;
                    }
                }
            }
            if (resistance.getValue() && (noCheck.getValue() || (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2))) {
                throwing = checkThrow(StatusEffects.RESISTANCE);
                if (isThrow() && delayTimer.passedMs((long) (delay.getValue() * 1000))) {
                    if (throwPotion(StatusEffects.RESISTANCE)) {
                        delayTimer.reset();
                        return;
                    }
                }
            }
        }
    }

    public boolean throwPotion(StatusEffect targetEffect) {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int newSlot;

        if (pauseModule.getValue()) KawaiiAura.INSTANCE.lastBreakTimer.reset();

        if (findPotion(targetEffect) == -1 && (!inventory.getValue() || findPotionInventorySlot(targetEffect) == -1)) {
            if (pendingEffect == targetEffect) {
                pendingEffect = null;
                throwStart = 0L;
            }
            return false;
        }

        long now = System.currentTimeMillis();

        if (sneak.getValue() && (long) throwDelay.getValue() > 0L) {
            if (pendingEffect == null || pendingEffect != targetEffect) {
                try {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                    sneakingFromAuto = true;
                    sneakTimer.reset();
                } catch (Throwable ignored) {}
                pendingEffect = targetEffect;
                throwStart = now;
                return false;
            } else {
                if (now - throwStart < (long) throwDelay.getValue()) return false;
                pendingEffect = null;
                throwStart = 0L;
            }
        } else {
            if (sneak.getValue() && !sneakingFromAuto) {
                try {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                    sneakingFromAuto = true;
                    sneakTimer.reset();
                } catch (Throwable ignored) {}
            }
        }

        for (PlayerEntity player : CombatUtil.getEnemies(range.getValue())) {
            if (inventory.getValue() && (newSlot = findPotionInventorySlot(targetEffect)) != -1) {
                Kawaii.ROTATION.snapAt(Kawaii.ROTATION.rotationYaw, 90);
                InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
                try { sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id)); } catch (Throwable ignored) {}
                InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
                EntityUtil.syncInventory();
                if (AntiCheat.INSTANCE.snapBack.getValue()) Kawaii.ROTATION.snapBack();
                return true;
            } else if ((newSlot = findPotion(targetEffect)) != -1) {
                Kawaii.ROTATION.snapAt(Kawaii.ROTATION.rotationYaw, 90);
                InventoryUtil.switchToSlot(newSlot);
                try { sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id)); } catch (Throwable ignored) {}
                InventoryUtil.switchToSlot(oldSlot);
                if (AntiCheat.INSTANCE.snapBack.getValue()) Kawaii.ROTATION.snapBack();
                return true;
            }
        }

        return false;
    }

    public boolean isThrow() { return throwing; }

    public boolean checkThrow(StatusEffect targetEffect) {
        if (isOff()) return false;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof InventoryScreen) && !(mc.currentScreen instanceof ClickGuiScreen) && !(mc.currentScreen instanceof GameMenuScreen)) return false;
        if (usingPause.getValue() && mc.player.isUsingItem()) return false;
        if (findPotion(targetEffect) == -1 && (!inventory.getValue() || findPotionInventorySlot(targetEffect) == -1)) return false;
        return true;
    }

    public static int findPotionInventorySlot(StatusEffect targetEffect) {
        for (int i = 0; i < 45; ++i) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (Item.getRawId(itemStack.getItem()) != Item.getRawId(Items.SPLASH_POTION)) continue;
            List<StatusEffectInstance> effects = PotionUtil.getPotionEffects(itemStack);
            for (StatusEffectInstance effect : effects) {
                if (effect.getEffectType() == targetEffect) return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    public static int findPotion(StatusEffect targetEffect) {
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = InventoryUtil.getStackInSlot(i);
            if (Item.getRawId(itemStack.getItem()) != Item.getRawId(Items.SPLASH_POTION)) continue;
            List<StatusEffectInstance> effects = PotionUtil.getPotionEffects(itemStack);
            for (StatusEffectInstance effect : effects) {
                if (effect.getEffectType() == targetEffect) return i;
            }
        }
        return -1;
    }
}
