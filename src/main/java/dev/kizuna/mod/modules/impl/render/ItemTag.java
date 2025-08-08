package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.mod.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ItemTag extends Module {

    public ItemTag() {
        super("ItemTag", Category.Render);
    }

    @Override
    public void onUpdate() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
                MutableText itemNameText = (MutableText) Text.of(itemEntity.getStack().getItem().getName().getString());
                Style style = Style.EMPTY.withColor(Formatting.WHITE).withParent(Style.EMPTY);
                itemNameText.setStyle(style);
                itemEntity.setCustomName(itemNameText);
                itemEntity.setCustomNameVisible(true);
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.world == null) return;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.setCustomNameVisible(false);
            }
        }
    }
}
