package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.api.utils.entity.EntityUtil;
import dev.kizuna.api.utils.render.Render2DUtil;
import dev.kizuna.api.utils.render.TextUtil;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ItemTag2 extends Module {
    public static ItemTag2 INSTANCE;

    private final SliderSetting scale = add(new SliderSetting("Scale", 0.68f, 0.1f, 2f, 0.01));
    private final SliderSetting minScale = add(new SliderSetting("MinScale", 0.2f, 0.1f, 1f, 0.01));
    private final SliderSetting scaled = add(new SliderSetting("Scaled", 1, 0, 2, 0.01));
    private final SliderSetting offset = add(new SliderSetting("Offset", 0.315f, 0.001f, 1f, 0.001));
    private final SliderSetting height = add(new SliderSetting("Height", 0, -3, 3, 0.01));
    
    private final BooleanSetting merge = add(new BooleanSetting("Merge", true));
    private final SliderSetting mergeRange = add(new SliderSetting("MergeRange", 2.0f, 0.1f, 10.0f, 0.1));
    private final SliderSetting maxLines = add(new SliderSetting("MaxLines", 5, 1, 20, 1));

    private final ColorSetting outline = add(new ColorSetting("Outline", new Color(0x99FFFFFF, true)).injectBoolean(true));
    private final ColorSetting rect = add(new ColorSetting("Rect", new Color(0x99000001, true)).injectBoolean(true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(0xFFFFFFFF, true)));
    private final ColorSetting lineColor = add(new ColorSetting("LineColor", new Color(0xFF00FFFF, true)).injectBoolean(true));

    public final EnumSetting<NameTags.Font> font = add(new EnumSetting<>("FontMode", NameTags.Font.Fast));

    private final Vector4d positionVec = new Vector4d();

    public ItemTag2() {
        super("ItemTag2", Category.Render);
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (mc == null || mc.world == null) return;

        Set<Entity> processed = new HashSet<>();
        List<Entity> allEntities = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            allEntities.add(e);
        }

        for (Entity ent : allEntities) {
            if (!(ent instanceof ItemEntity itemEntity) || processed.contains(ent)) continue;

            List<ItemEntity> group = new ArrayList<>();
            group.add(itemEntity);
            processed.add(ent);

            if (merge.getValue()) {
                for (Entity other : allEntities) {
                    if (other instanceof ItemEntity otherItem && !processed.contains(other)) {
                        if (ent.squaredDistanceTo(other) <= mergeRange.getValue() * mergeRange.getValue()) {
                            group.add(otherItem);
                            processed.add(other);
                        }
                    }
                }
            }

            // Calculate average position
            double totalX = 0, totalY = 0, totalZ = 0;
            for (ItemEntity e : group) {
                totalX += e.prevX + (e.getX() - e.prevX) * tickDelta;
                totalY += e.prevY + (e.getY() - e.prevY) * tickDelta;
                totalZ += e.prevZ + (e.getZ() - e.prevZ) * tickDelta;
            }
            double x = totalX / group.size();
            double y = totalY / group.size();
            double z = totalZ / group.size();

            // Aggregate items
            Map<String, Integer> itemCounts = new HashMap<>();
            for (ItemEntity e : group) {
                ItemStack stack = e.getStack();
                String name = stack.getName().getString();
                itemCounts.put(name, itemCounts.getOrDefault(name, 0) + stack.getCount());
            }

            // Generate lines
            List<String> lines = new ArrayList<>();
            List<String> sortedNames = itemCounts.keySet().stream().sorted().collect(Collectors.toList());
            
            int limit = (int) maxLines.getValue();
            int displayedCount = 0;
            
            for (String name : sortedNames) {
                if (displayedCount >= limit) break;
                int count = itemCounts.get(name);
                lines.add(name + (count > 1 ? " x" + count : ""));
                displayedCount++;
            }
            
            if (sortedNames.size() > limit) {
                lines.add("(...) +" + (sortedNames.size() - limit));
            }

            Vec3d vector = new Vec3d(x, y + height.getValue() + itemEntity.getBoundingBox().getLengthY() + 0.3, z);
            Vec3d preVec = vector;
            vector = TextUtil.worldSpaceToScreenSpace(vector);
            if (vector.z > 0 && vector.z < 1) {
                // Reuse field Vector4d
                positionVec.set(vector.x, vector.y, vector.z, 0);
                positionVec.x = Math.min(vector.x, positionVec.x);
                positionVec.y = Math.min(vector.y, positionVec.y);
                positionVec.z = Math.max(vector.x, positionVec.z);

                double posX = positionVec.x;
                double posY = positionVec.y;
                double endPosX = positionVec.z;

                NameTags.Font chosenFont = font.getValue();
                
                float maxTextWidth = 0;
                for (String line : lines) {
                    float w;
                    if (chosenFont == NameTags.Font.Fancy) {
                        w = FontRenderers.ui.getWidth(line);
                    } else {
                        w = mc.textRenderer.getWidth(line);
                    }
                    if (w > maxTextWidth) maxTextWidth = w;
                }

                float diff = (float) (endPosX - posX) / 2;
                float tagX = (float) ((posX + diff - maxTextWidth / 2));

                context.getMatrices().push();
                context.getMatrices().translate(tagX - 2 + (maxTextWidth + 4) / 2f, (float) (posY - 13f) + 6.5f, 0);
                float size = (float) Math.max(1 - MathHelper.sqrt((float) mc.cameraEntity.squaredDistanceTo(preVec)) * 0.01 * scaled.getValue(), 0);
                context.getMatrices().scale(Math.max(scale.getValueFloat() * size, minScale.getValueFloat()), Math.max(scale.getValueFloat() * size, minScale.getValueFloat()), 1f);
                context.getMatrices().translate(0, offset.getValueFloat() * MathHelper.sqrt((float) EntityUtil.getEyesPos().squaredDistanceTo(preVec)), 0);
                context.getMatrices().translate(-(tagX - 2 + (maxTextWidth + 4) / 2f), -(float) ((posY - 13f) + 6.5f), 0);

                float lineHeight = (chosenFont == NameTags.Font.Fancy) ? FontRenderers.ui.getFontHeight() + 2 : mc.textRenderer.fontHeight + 2;
                float totalHeight = lines.size() * lineHeight;

                // Draw background rect
                if (rect.booleanValue) {
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 2, (float) (posY - 13f) - (lines.size() - 1) * lineHeight, maxTextWidth + 4, 11 + (lines.size() - 1) * lineHeight, rect.getValue());
                }
                
                // Draw customizable colored line at the bottom
                if (lineColor.booleanValue) {
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 2, (float) (posY - 2f), maxTextWidth + 4, 1.5f, lineColor.getValue());
                }

                // Draw outline
                if (outline.booleanValue) {
                    float rectTop = (float) (posY - 14f) - (lines.size() - 1) * lineHeight;
                    float rectBottom = (float) (posY - 2f);
                    float rectLeft = tagX - 3;
                    float rectRight = tagX + maxTextWidth + 2;
                    float rectHeight = rectBottom - rectTop + 1; // Approx height

                    // Top
                    Render2DUtil.drawRect(context.getMatrices(), rectLeft, rectTop, maxTextWidth + 6, 1, outline.getValue());
                    // Bottom
                    Render2DUtil.drawRect(context.getMatrices(), rectLeft, rectBottom, maxTextWidth + 6, 1, outline.getValue());
                    // Left
                    Render2DUtil.drawRect(context.getMatrices(), rectLeft, rectTop, 1, rectHeight, outline.getValue());
                    // Right
                    Render2DUtil.drawRect(context.getMatrices(), rectRight, rectTop, 1, rectHeight, outline.getValue());
                }
                
                // Draw text lines
                float currentY = (float) posY - 10 - (lines.size() - 1) * lineHeight;
                for (String line : lines) {
                    float lineWidth;
                    if (chosenFont == NameTags.Font.Fancy) {
                        lineWidth = FontRenderers.ui.getWidth(line);
                    } else {
                        lineWidth = mc.textRenderer.getWidth(line);
                    }
                    float centeredX = tagX + (maxTextWidth - lineWidth) / 2f;

                    if (chosenFont == NameTags.Font.Fancy) {
                        FontRenderers.ui.drawString(context.getMatrices(), line, centeredX, currentY, color.getValue().getRGB());
                    } else {
                        context.drawText(mc.textRenderer, line, (int)centeredX, (int)currentY, color.getValue().getRGB(), true);
                    }
                    currentY += lineHeight;
                }

                context.getMatrices().pop();
            }
        }
    }
}
