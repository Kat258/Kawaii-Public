package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.mod.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

public class ItemTag extends Module {
    private static final Style NAME_STYLE = Style.EMPTY.withColor(Formatting.WHITE);

    public ItemTag() {
        super("ItemTag", Category.Render);
    }

    @Override
    public void onUpdate() {
        if (mc.world == null || mc.player == null) return;
        double radius = (mc.options.getClampedViewDistance() + 1) * 16.0;
        Box scanBox = mc.player.getBoundingBox().expand(radius);
        for (ItemEntity itemEntity : mc.world.getEntitiesByClass(ItemEntity.class, scanBox, Entity::isAlive)) {
            String name = itemEntity.getStack().getItem().getName().getString();
            Text current = itemEntity.getCustomName();
            if (itemEntity.isCustomNameVisible() && current != null && name.equals(current.getString())) continue;
            MutableText itemNameText = Text.literal(name);
            itemNameText.setStyle(NAME_STYLE);
            itemEntity.setCustomName(itemNameText);
            itemEntity.setCustomNameVisible(true);
        }
    }

    @Override
    public void onDisable() {
        if (mc.world == null || mc.player == null) return;
        double radius = (mc.options.getClampedViewDistance() + 1) * 16.0;
        Box scanBox = mc.player.getBoundingBox().expand(radius);
        for (ItemEntity itemEntity : mc.world.getEntitiesByClass(ItemEntity.class, scanBox, Entity::isAlive)) {
            if (itemEntity.isCustomNameVisible()) {
                itemEntity.setCustomNameVisible(false);
            }
        }
    }
}
