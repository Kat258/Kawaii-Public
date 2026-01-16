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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;

import java.awt.*;
import java.util.*;
import java.util.List;

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
    private final ArrayList<CachedGroup> cachedGroups = new ArrayList<>();
    private long lastCacheWorldTime = Long.MIN_VALUE;

    public ItemTag2() {
        super("ItemTag2", Category.Render);
        INSTANCE = this;
    }

    private void updateCacheIfNeeded() {
        if (mc == null || mc.world == null || mc.player == null) return;
        long worldTime = mc.world.getTime();
        if (worldTime == lastCacheWorldTime) return;
        lastCacheWorldTime = worldTime;

        cachedGroups.clear();

        double radius = (mc.options.getClampedViewDistance() + 1) * 16.0;
        Box scanBox = mc.player.getBoundingBox().expand(radius);
        ArrayList<ItemEntity> items = new ArrayList<>(mc.world.getEntitiesByClass(ItemEntity.class, scanBox, Entity::isAlive));
        if (items.isEmpty()) return;

        int limit = (int) maxLines.getValue();

        if (!merge.getValue()) {
            for (ItemEntity item : items) {
                CachedGroup group = new CachedGroup();
                group.x = item.getX();
                group.y = item.getY();
                group.z = item.getZ();
                group.boxY = item.getBoundingBox().getLengthY();

                ItemStack stack = item.getStack();
                String itemName = stack.getName().getString();
                int count = stack.getCount();

                group.lines = new ArrayList<>(1);
                group.lines.add(itemName + (count > 1 ? " x" + count : ""));
                cachedGroups.add(group);
            }
            return;
        }

        double range = mergeRange.getValue();
        double rangeSq = range * range;
        double cellSize = Math.max(1.0, range);

        HashMap<Long, ArrayList<ItemEntity>> buckets = new HashMap<>();
        for (ItemEntity item : items) {
            int cellX = (int) Math.floor(item.getX() / cellSize);
            int cellZ = (int) Math.floor(item.getZ() / cellSize);
            long key = (((long) cellX) << 32) ^ (cellZ & 0xffffffffL);
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        Set<ItemEntity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<ItemEntity> queue = new ArrayDeque<>();

        for (ItemEntity start : items) {
            if (visited.contains(start)) continue;

            queue.clear();
            queue.add(start);
            visited.add(start);

            ArrayList<ItemEntity> groupItems = new ArrayList<>();

            while (!queue.isEmpty()) {
                ItemEntity cur = queue.poll();
                groupItems.add(cur);

                int curCellX = (int) Math.floor(cur.getX() / cellSize);
                int curCellZ = (int) Math.floor(cur.getZ() / cellSize);

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int nx = curCellX + dx;
                        int nz = curCellZ + dz;
                        long key = (((long) nx) << 32) ^ (nz & 0xffffffffL);
                        ArrayList<ItemEntity> candidates = buckets.get(key);
                        if (candidates == null) continue;

                        for (ItemEntity cand : candidates) {
                            if (visited.contains(cand)) continue;
                            if (cur.squaredDistanceTo(cand) <= rangeSq) {
                                visited.add(cand);
                                queue.add(cand);
                            }
                        }
                    }
                }
            }

            double totalX = 0, totalY = 0, totalZ = 0;
            double maxBoxY = 0;
            HashMap<String, Integer> itemCounts = new HashMap<>();

            for (ItemEntity item : groupItems) {
                totalX += item.getX();
                totalY += item.getY();
                totalZ += item.getZ();
                maxBoxY = Math.max(maxBoxY, item.getBoundingBox().getLengthY());

                ItemStack stack = item.getStack();
                String name = stack.getName().getString();
                int count = stack.getCount();
                Integer prev = itemCounts.get(name);
                itemCounts.put(name, prev == null ? count : prev + count);
            }

            CachedGroup group = new CachedGroup();
            group.x = totalX / groupItems.size();
            group.y = totalY / groupItems.size();
            group.z = totalZ / groupItems.size();
            group.boxY = maxBoxY;

            ArrayList<String> sortedNames = new ArrayList<>(itemCounts.keySet());
            Collections.sort(sortedNames);

            ArrayList<String> lines = new ArrayList<>();
            int displayed = 0;
            for (String name : sortedNames) {
                if (displayed >= limit) break;
                int count = itemCounts.get(name);
                lines.add(name + (count > 1 ? " x" + count : ""));
                displayed++;
            }
            if (sortedNames.size() > limit) {
                lines.add("(...) +" + (sortedNames.size() - limit));
            }

            group.lines = lines;
            cachedGroups.add(group);
        }
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (mc == null || mc.world == null) return;
        updateCacheIfNeeded();

        for (CachedGroup group : cachedGroups) {
            Vec3d vector = new Vec3d(group.x, group.y + height.getValue() + group.boxY + 0.3, group.z);
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
                for (String line : group.lines) {
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
                int lineCount = group.lines.size();
                if (rect.booleanValue) {
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 2, (float) (posY - 13f) - (lineCount - 1) * lineHeight, maxTextWidth + 4, 11 + (lineCount - 1) * lineHeight, rect.getValue());
                }
                
                if (lineColor.booleanValue) {
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 2, (float) (posY - 2f), maxTextWidth + 4, 1.5f, lineColor.getValue());
                }

                if (outline.booleanValue) {
                    float rectTop = (float) (posY - 14f) - (lineCount - 1) * lineHeight;
                    float rectBottom = (float) (posY - 2f);
                    float rectLeft = tagX - 3;
                    float rectRight = tagX + maxTextWidth + 2;
                    float rectHeight = rectBottom - rectTop + 1; // Approx height

                    Render2DUtil.drawRect(context.getMatrices(), rectLeft, rectTop, maxTextWidth + 6, 1, outline.getValue());
                    Render2DUtil.drawRect(context.getMatrices(), rectLeft, rectBottom, maxTextWidth + 6, 1, outline.getValue());
                    Render2DUtil.drawRect(context.getMatrices(), rectLeft, rectTop, 1, rectHeight, outline.getValue());
                    Render2DUtil.drawRect(context.getMatrices(), rectRight, rectTop, 1, rectHeight, outline.getValue());
                }
                
                float currentY = (float) posY - 10 - (lineCount - 1) * lineHeight;
                for (String line : group.lines) {
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

    private static class CachedGroup {
        double x;
        double y;
        double z;
        double boxY;
        ArrayList<String> lines;
    }
}
