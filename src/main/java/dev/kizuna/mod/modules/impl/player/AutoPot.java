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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;

import java.util.List;

public class AutoPot extends Module {

    public static AutoPot INSTANCE;

    private final SliderSetting range = add(new SliderSetting("Range", 6.0, 1, 15.0));
    private final SliderSetting delay = add(new SliderSetting("Delay", 5, 0, 10).setSuffix("s"));
    private final BooleanSetting speed = add(new BooleanSetting("Speed", true));
    private final BooleanSetting resistance = add(new BooleanSetting("Resistance", true));
    private final BooleanSetting usingPause = add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting onlyGround = add(new BooleanSetting("OnlyGround", true));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting noCheck = add(new BooleanSetting("NoCheck", false));
    private final BooleanSetting pauseModule = add(new BooleanSetting("PauseModule", true));

    private final BooleanSetting single = add(new BooleanSetting("Single", false));
    public final BindSetting boostKey = add(new BindSetting("BoostKey", -1));
    public final BooleanSetting slowFalling = add(new BooleanSetting("SlowFallingPotion", false));
    public final BindSetting slowFallKey = add(new BindSetting("SlowFallKey", -1));

    private final BooleanSetting sneak = add(new BooleanSetting("Sneak", true));
    private final SliderSetting duration = add(new SliderSetting("Duration", 50, 0, 10000).setSuffix("ms"));
    private final SliderSetting throwDelay = add(new SliderSetting("ThrowDelay", 50, 0, 2000).setSuffix("ms"));
    private final SliderSetting slowFallDelay = add(new SliderSetting("SlowFallDelay", 500, 0, 5000).setSuffix("ms"));

    private final Timer delayTimer = new Timer();
    private final Timer sneakTimer = new Timer();
    private final Timer slowFallTimer = new Timer();

    public AutoPot() {
        super("AutoPot", Category.Player);
        INSTANCE = this;
    }

    private boolean throwing = false;
    private boolean sneakingFromAuto = false;
    private boolean slowFallQueued = false;
    private boolean slowFallPrev = false;
    private boolean boostPrev = false;
    private boolean deferredBoost = false;

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
        slowFallPrev = false;
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

        try {
            boolean pressed = slowFalling.getValue() && slowFallKey.isPressed();
            long interval = (long) slowFallDelay.getValue();
            if (interval <= 0) interval = 50;

            if (pressed) {
                if (!slowFallPrev || slowFallTimer.passedMs(interval)) {
                    try {
                        if (throwSlowFalling()) slowFallTimer.reset();
                    } catch (Throwable ignored) {}
                }
            }
            slowFallPrev = pressed;
        } catch (Throwable ignored) {}

        if (deferredBoost) {
            if (mc.player.isOnGround() && !mc.world.isAir(new BlockPosX(mc.player.getPos().add(0, -1, 0)))) {
                if (pendingEffect != null) {
                    if (throwPotion(pendingEffect)) {
                        delayTimer.reset();
                        deferredBoost = false;
                        pendingEffect = null;
                        boostPrev = false;
                    }
                } else {
                    deferredBoost = false;
                    boostPrev = false;
                }
            }
        }

        if (single.getValue()) {
            boolean pressed = false;
            try { pressed = boostKey.isPressed(); } catch (Throwable ignored) {}

            if (pressed && !boostPrev) {
                if (onlyGround.getValue() && !(mc.player.isOnGround() && !mc.world.isAir(new BlockPosX(mc.player.getPos().add(0, -1, 0))))) {
                    StatusEffect toQueue = null;
                    if (speed.getValue() && (noCheck.getValue() || !mc.player.hasStatusEffect(StatusEffects.SPEED))) {
                        toQueue = (StatusEffect) StatusEffects.SPEED;
                    } else if (resistance.getValue() && (noCheck.getValue() || (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2))) {
                        toQueue = (StatusEffect) StatusEffects.RESISTANCE;
                    }
                    if (toQueue != null) {
                        pendingEffect = toQueue;
                        deferredBoost = true;
                        boostPrev = true;
                    }
                    return;
                }
                if (delayTimer.passedMs((long) (delay.getValue() * 1000))) {
                    if (slowFallQueued && slowFalling.getValue()) {
                        try {
                            if (throwSlowFalling()) {
                                delayTimer.reset();
                                slowFallQueued = false;
                                boostPrev = pressed;
                                return;
                            }
                        } catch (Throwable ignored) {}
                    }
                    if (speed.getValue() && (noCheck.getValue() || !mc.player.hasStatusEffect(StatusEffects.SPEED))) {
                        throwing = checkThrow((StatusEffect) StatusEffects.SPEED);
                        if (isThrow()) {
                            if (throwPotion((StatusEffect) StatusEffects.SPEED)) delayTimer.reset();
                        }
                    }
                    if (!throwing && resistance.getValue() && (noCheck.getValue() || (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2))) {
                        throwing = checkThrow((StatusEffect) StatusEffects.RESISTANCE);
                        if (isThrow()) {
                            if (throwPotion((StatusEffect) StatusEffects.RESISTANCE)) delayTimer.reset();
                        }
                    }
                }
            }
            boostPrev = pressed;
            if (pendingEffect != null) {
                try { throwPotion(pendingEffect); } catch (Throwable ignored) {}
            }
            return;
        }

        if (!onlyGround.getValue() || (mc.player.isOnGround() && !mc.world.isAir(new BlockPosX(mc.player.getPos().add(0, -1, 0))))) {
            if (speed.getValue() && (noCheck.getValue() || !mc.player.hasStatusEffect(StatusEffects.SPEED))) {
                throwing = checkThrow((StatusEffect) StatusEffects.SPEED);
                if (isThrow() && delayTimer.passedMs((long) (delay.getValue() * 1000))) {
                    if (throwPotion((StatusEffect) StatusEffects.SPEED)) {
                        delayTimer.reset();
                        return;
                    }
                }
            }
            if (resistance.getValue() && (noCheck.getValue() || (!mc.player.hasStatusEffect(StatusEffects.RESISTANCE) || mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() < 2))) {
                throwing = checkThrow((StatusEffect) StatusEffects.RESISTANCE);
                if (isThrow() && delayTimer.passedMs((long) (delay.getValue() * 1000))) {
                    if (throwPotion((StatusEffect) StatusEffects.RESISTANCE)) {
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

        List<PlayerEntity> enemies = CombatUtil.getEnemies(range.getValue());
        if (enemies == null || enemies.isEmpty()) {
            if (pendingEffect == targetEffect) {
                pendingEffect = null;
                throwStart = 0L;
                try {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                } catch (Throwable ignored) {}
                sneakingFromAuto = false;
            }
            return false;
        }

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
                try { sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch())); } catch (Throwable ignored) {}
                InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
                EntityUtil.syncInventory();
                if (AntiCheat.INSTANCE.snapBack.getValue()) Kawaii.ROTATION.snapBack();
                return true;
            } else if ((newSlot = findPotion(targetEffect)) != -1) {
                Kawaii.ROTATION.snapAt(Kawaii.ROTATION.rotationYaw, 90);
                InventoryUtil.switchToSlot(newSlot);
                try { sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch())); } catch (Throwable ignored) {}
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

    public boolean throwSlowFalling() {
        StatusEffect target = (StatusEffect) StatusEffects.SLOW_FALLING;
        ItemStack main = mc.player.getMainHandStack();
        if (Item.getRawId(main.getItem()) == Item.getRawId(Items.SPLASH_POTION)) {
        }

        if (inventory.getValue()) {
            int invSlot = findPotionInventorySlot(target);
            if (invSlot != -1) {
                int current = mc.player.getInventory().selectedSlot;
                InventoryUtil.inventorySwap(invSlot, current);
                Kawaii.ROTATION.snapAt(mc.player.getYaw(), mc.player.getPitch());
                sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
                InventoryUtil.inventorySwap(invSlot, current);
                EntityUtil.syncInventory();
                if (AntiCheat.INSTANCE.snapBack.getValue()) Kawaii.ROTATION.snapBack();
                return true;
            }
        }

        int hot = findPotion(target);
        if (hot != -1) {
            int old = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(hot);
            Kawaii.ROTATION.snapAt(mc.player.getYaw(), mc.player.getPitch());
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.switchToSlot(old);
            if (AntiCheat.INSTANCE.snapBack.getValue()) Kawaii.ROTATION.snapBack();
            return true;
        }
        return false;
    }
    public static int findPotionInventorySlot(StatusEffect targetEffect) {
        for (int i = 0; i < 45; ++i) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (Item.getRawId(itemStack.getItem()) != Item.getRawId(Items.SPLASH_POTION)) continue;
            PotionContentsComponent contents = itemStack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) continue;
            for (StatusEffectInstance effect : contents.getEffects()) {
                if (effect.getEffectType().value() == targetEffect) return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    public static int findPotion(StatusEffect targetEffect) {
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = InventoryUtil.getStackInSlot(i);
            if (Item.getRawId(itemStack.getItem()) != Item.getRawId(Items.SPLASH_POTION)) continue;
            PotionContentsComponent contents = itemStack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) continue;
            for (StatusEffectInstance effect : contents.getEffects()) {
                if (effect.getEffectType().value() == targetEffect) return i;
            }
        }
        return -1;
    }
}
