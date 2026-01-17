package dev.kizuna.api.utils.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;

public final class EnchantmentUtil {
    private EnchantmentUtil() {
    }

    public static int getLevel(RegistryKey<Enchantment> enchantment, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) return 0;
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesKey(enchantment)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    public static boolean hasEnchantment(RegistryKey<Enchantment> enchantment, ItemStack stack) {
        return getLevel(enchantment, stack) > 0;
    }

    public static boolean hasAquaAffinity(LivingEntity entity) {
        if (entity == null) return false;
        return hasEnchantment(Enchantments.AQUA_AFFINITY, entity.getEquippedStack(EquipmentSlot.HEAD));
    }
}

