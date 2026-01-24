package dev.kizuna.mod.modules.impl.render;

import dev.kizuna.Kawaii;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import dev.kizuna.api.utils.entity.EntityUtil;
import dev.kizuna.api.utils.render.Render2DUtil;
import dev.kizuna.api.utils.render.TextUtil;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.impl.player.Freecam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.EntitySpawnEvent;
import dev.kizuna.api.events.impl.PacketEvent;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4d;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NameTags extends Module {
    public static NameTags INSTANCE;

    private final SliderSetting scale = add(new SliderSetting("Scale", 0.68f, 0.1f, 2f, 0.01));
    private final SliderSetting minScale = add(new SliderSetting("MinScale", 0.2f, 0.1f, 1f, 0.01));
    private final SliderSetting scaled = add(new SliderSetting("Scaled", 1, 0, 2, 0.01));
    private final SliderSetting offset = add(new SliderSetting("Offset", 0.315f, 0.001f, 1f, 0.001));
    private final SliderSetting height = add(new SliderSetting("Height", 0, -3, 3, 0.01));
    private final BooleanSetting god = add(new BooleanSetting("God", true));
    private final BooleanSetting gamemode = add(new BooleanSetting("Gamemode", false));
    private final BooleanSetting ping = add(new BooleanSetting("Ping", false));
    private final BooleanSetting health = add(new BooleanSetting("Health", true));
    private final BooleanSetting distance = add(new BooleanSetting("Distance", true));
    private final BooleanSetting pops = add(new BooleanSetting("TotemPops", true));
    private final BooleanSetting enchants = add(new BooleanSetting("Enchants", true));
    private final BooleanSetting showFallTime = add(new BooleanSetting("ShowFallTime", true));
    private final ColorSetting outline = add(new ColorSetting("Outline", new Color(0x99FFFFFF, true)).injectBoolean(true));
    private final ColorSetting rect = add(new ColorSetting("Rect", new Color(0x99000001, true)).injectBoolean(true));
    private final ColorSetting friendColor = add(new ColorSetting("FriendColor", new Color(0xFF1DFF1D, true)));
    private final ColorSetting enemyColor = add(new ColorSetting("EnemyColor", new Color(0xFF1DFF1D, true)));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(0xFFFFFFFF, true)));
    private final BooleanSetting mTag = add(new BooleanSetting("MTag", false));

    public final EnumSetting<Font> font = add(new EnumSetting<>("FontMode", Font.Fast));
    private final SliderSetting armorHeight = add(new SliderSetting("ArmorHeight", 0.3f, -10, 10f));
    private final SliderSetting armorScale = add(new SliderSetting("ArmorScale", 0.9f, 0.1f, 2f, 0.01f));
    private final EnumSetting<Armor> armorMode = add(new EnumSetting<>("ArmorMode", Armor.Full));

    private static final Map<UUID, Long> slowFallExpiry = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<StatusEffect, Long>> potionMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastStuckArrowCount = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastPopCountMap = new ConcurrentHashMap<>();

    private static final Color COLOR_RED = Color.RED;
    private static final Color COLOR_ORANGE = Color.ORANGE;
    private static final Color COLOR_GREEN = Color.GREEN;
    private static final Color COLOR_BAR_GREEN = new Color(0x9900FF00, true);
    private static final Color COLOR_BAR_YELLOW = new Color(0x99EEFF05, true);
    private static final Color COLOR_BAR_RED = new Color(0x99FF0000, true);
    private static final Color COLOR_BAR_BG = new Color(0x80000000, true);
    
    private final Vector4d positionVec = new Vector4d();
    private final HashMap<UUID, CachedTag> tagCache = new HashMap<>();
    private long lastCacheWorldTime = Long.MIN_VALUE;
    private final Map<Integer, PotionTrack> trackedPotions = new ConcurrentHashMap<>();
    private static final long TURTLE_SPLASH_MS = 20_000L;
    private static final double SPLASH_RADIUS = 4.0;

    private static final class PotionTrack {
        Vec3d pos;
        long lastSeenTick;
        PotionTrack(Vec3d pos, long lastSeenTick) {
            this.pos = pos;
            this.lastSeenTick = lastSeenTick;
        }
    }

    public NameTags() {
        super("NameTags", Category.Render);
        INSTANCE = this;
    }

    public static void markPlayerShot(PlayerEntity player) {
        if (player == null) return;
        slowFallExpiry.put(player.getUuid(), System.currentTimeMillis() + 30_000L);
    }


    public static int getLastStuckCount(UUID uuid) {
        return lastStuckArrowCount.getOrDefault(uuid, 0);
    }
    public static void setLastStuckCount(UUID uuid, int count) {
        if (uuid == null) return;
        lastStuckArrowCount.put(uuid, count);
    }

    private boolean isChinese(String str) {
        for (char c : str.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }

    private boolean containsChinese(String text) {
        if (text == null) return false;
        return isChinese(text);
    }

    private CachedTag getOrUpdateCache(PlayerEntity ent, long worldTime, long now) {
        CachedTag cached = tagCache.get(ent.getUuid());
        if (cached != null && cached.worldTime == worldTime) {
            return cached;
        }

        if (cached == null) {
            cached = new CachedTag();
            tagCache.put(ent.getUuid(), cached);
        }

        cached.worldTime = worldTime;
        cached.text = buildNameTagText(ent, now);

        boolean hasCJK = isChinese(cached.text);
        Font chosenFont = font.getValue();
        if (chosenFont == Font.Fancy && hasCJK) chosenFont = Font.Fast;
        cached.font = chosenFont;
        cached.textWidth = chosenFont == Font.Fancy ? FontRenderers.ui.getWidth(cached.text) : mc.textRenderer.getWidth(cached.text);

        if (armorMode.getValue() != Armor.None) {
            cached.fillArmor(ent, enchants.getValue());
        } else {
            cached.clearArmor();
        }

        return cached;
    }

    private String buildNameTagText(PlayerEntity ent, long now) {
        UUID uuid = ent.getUuid();
        Integer currentPopCount = null;
        if (pops.getValue()) {
            currentPopCount = Kawaii.POP.getPop(ent.getName().getString());
            Integer lastPopCount = lastPopCountMap.getOrDefault(uuid, 0);

            if (currentPopCount > lastPopCount) {
                slowFallExpiry.put(uuid, now + 30_000L);
                lastPopCountMap.put(uuid, currentPopCount);
            }
        }

        String fallPrefix = "";
        if (showFallTime.getValue()) {
            Long expiry = slowFallExpiry.get(uuid);
            if (expiry != null) {
                long remain = expiry - now;
                if (remain > 0) {
                    int sec = (int) Math.ceil(remain / 1000.0);
                    int m = sec / 60;
                    int s = sec % 60;
                    fallPrefix = Formatting.AQUA.toString() + String.format("%ds ", s);
                } else {
                    slowFallExpiry.remove(uuid);
                }
            }
            
            if (fallPrefix.isEmpty()) {
                var slowFalling = ent.getStatusEffect(StatusEffects.SLOW_FALLING);
                if (slowFalling != null) {
                    int sec = (int) Math.ceil(slowFalling.getDuration() / 20.0);
                    int m = sec / 60;
                    int s = sec % 60;
                    fallPrefix = Formatting.AQUA.toString() + String.format("%02d:%02d ", m, s);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        if (!fallPrefix.isEmpty()) sb.append(fallPrefix);

        if (god.getValue()) {
            boolean isGod = false;
            long godTime = -1;

            if (potionMap.containsKey(uuid) && potionMap.get(uuid).containsKey(StatusEffects.SLOWNESS.value())) {
                     long expiry = potionMap.get(uuid).get(StatusEffects.SLOWNESS.value());
                     if (expiry == -1L) {
                         isGod = true;
                         godTime = -1;
                     } else if (expiry > now) {
                         isGod = true;
                         godTime = expiry - now;
                     } else {
                         potionMap.get(uuid).remove(StatusEffects.SLOWNESS.value());
                     }
                 }

            if (ent.hasStatusEffect(StatusEffects.SLOWNESS)) {
                StatusEffectInstance effect = ent.getStatusEffect(StatusEffects.SLOWNESS);
                if (effect != null) {
                    long duration = effect.getDuration() * 50L;
                    if (duration > godTime) {
                        isGod = true;
                        godTime = duration;
                    }
                }
            }

            if (isGod) {
                if (godTime > 0) {
                    int sec = (int) Math.ceil(godTime / 1000.0);
                    sb.append("§e").append("r").append(sec).append("s ");
                } else if (godTime == -1) {
                    sb.append("§eRS ? ");
                }
            }
        }
        if (ping.getValue()) {
            sb.append(getEntityPing(ent)).append("ms ");
        }
        if (gamemode.getValue()) {
            sb.append(translateGamemode(getEntityGamemode(ent))).append(" ");
        }
        sb.append(Formatting.RESET).append(ent.getName().getString());
        if (health.getValue()) {
            sb.append(" ").append(getHealthColor(ent)).append(round2(ent.getAbsorptionAmount() + ent.getHealth()));
        }
        if (distance.getValue()) {
            sb.append(" ").append(Formatting.RESET).append(String.format("%.1f", mc.player.distanceTo(ent))).append("m");
        }
        if (pops.getValue()) {
            if (currentPopCount == null) {
                currentPopCount = Kawaii.POP.getPop(ent.getName().getString());
            }
            sb.append(" §bPop ").append(Formatting.LIGHT_PURPLE).append(currentPopCount);
        }

        return sb.toString();
    }

    /**
     * 处理包含 mTag 标记的字符串渲染，支持 RGB 颜色
     * mTag 标记会被替换为 #ff80d0 颜色的 [M]
     */
    private void drawStringWithRGBSupport(DrawContext context, String text, float x, float y, 
                                          int defaultColor, Font fontMode, boolean shadow) {
        String mTagMarker = "\u00a7#mTag";
        int index = text.indexOf(mTagMarker);
        
        if (index == -1) {
            // 无 mTag 标记，正常渲染
            if (fontMode == Font.Fancy) {
                FontRenderers.ui.drawString(context.getMatrices(), text, x, y, defaultColor);
            } else {
                context.drawText(mc.textRenderer, text, (int)x, (int)y, defaultColor, shadow);
            }
            return;
        }

        // 有 mTag 标记，需要分段渲染
        float currentX = x;
        String mTag = "[M]";
        int mTagColor = 0xffff80d0; // RGB 颜色
        
        int lastIndex = 0;
        while (index != -1) {
            // 渲染标记前的部分
            if (index > lastIndex) {
                String part = text.substring(lastIndex, index);
                if (fontMode == Font.Fancy) {
                    FontRenderers.ui.drawString(context.getMatrices(), part, currentX, y, defaultColor);
                    currentX += FontRenderers.ui.getWidth(part);
                } else {
                    context.drawText(mc.textRenderer, part, (int)currentX, (int)y, defaultColor, shadow);
                    currentX += mc.textRenderer.getWidth(part);
                }
            }
            
            // 渲染 mTag
            if (fontMode == Font.Fancy) {
                FontRenderers.ui.drawString(context.getMatrices(), mTag, currentX, y, mTagColor);
                currentX += FontRenderers.ui.getWidth(mTag);
            } else {
                context.drawText(mc.textRenderer, mTag, (int)currentX, (int)y, mTagColor, shadow);
                currentX += mc.textRenderer.getWidth(mTag);
            }
            
            lastIndex = index + mTagMarker.length();
            index = text.indexOf(mTagMarker, lastIndex);
        }
        
        // 渲染剩余部分
        if (lastIndex < text.length()) {
            String part = text.substring(lastIndex);
            if (fontMode == Font.Fancy) {
                FontRenderers.ui.drawString(context.getMatrices(), part, currentX, y, defaultColor);
            } else {
                context.drawText(mc.textRenderer, part, (int)currentX, (int)y, defaultColor, shadow);
            }
        }
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (mc == null || mc.world == null) return;
        long now = System.currentTimeMillis();
        long worldTime = mc.world.getTime();
        boolean shouldPrune = lastCacheWorldTime != worldTime;
        lastCacheWorldTime = worldTime;

        for (PlayerEntity ent : mc.world.getPlayers()) {
            if (ent == mc.player && mc.options.getPerspective().isFirstPerson() && Freecam.INSTANCE.isOff()) continue;

            double x = ent.prevX + (ent.getX() - ent.prevX) * tickDelta;
            double y = ent.prevY + (ent.getY() - ent.prevY) * tickDelta;
            double z = ent.prevZ + (ent.getZ() - ent.prevZ) * tickDelta;
            Vec3d vector = new Vec3d(x, y + height.getValue() + ent.getBoundingBox().getLengthY() + 0.3, z);
            Vec3d preVec = vector;
            vector = TextUtil.worldSpaceToScreenSpace(vector);
            if (vector.z > 0 && vector.z < 1) {
                positionVec.set(vector.x, vector.y, vector.x, 0);

                CachedTag cached = getOrUpdateCache(ent, worldTime, now);
                double posX = positionVec.x;
                double posY = positionVec.y;
                float textWidth = cached.textWidth;
                float tagX = (float) (posX - textWidth / 2f);

                context.getMatrices().push();
                context.getMatrices().translate(tagX - 2 + (textWidth + 4) / 2f, (float) (posY - 13f) + 6.5f, 0);
                float size = (float) Math.max(1 - MathHelper.sqrt((float) mc.cameraEntity.squaredDistanceTo(preVec)) * 0.01 * scaled.getValue(), 0);
                context.getMatrices().scale(Math.max(scale.getValueFloat() * size, minScale.getValueFloat()), Math.max(scale.getValueFloat() * size, minScale.getValueFloat()), 1f);
                context.getMatrices().translate(0, offset.getValueFloat() * MathHelper.sqrt((float) EntityUtil.getEyesPos().squaredDistanceTo(preVec)), 0);
                context.getMatrices().translate(-(tagX - 2 + (textWidth + 4) / 2f), -(float) ((posY - 13f) + 6.5f), 0);

                float item_offset = 0;
                if (armorMode.getValue() != Armor.None) {
                    DiffuseLighting.disableGuiDepthLighting();
                    for (int i = 0; i < 6; i++) {
                        ItemStack armorComponent = cached.armorStacks[i];
                        
                        if (!armorComponent.isEmpty()) {
                            context.getMatrices().push();
                            context.getMatrices().translate(tagX - 2 + (textWidth + 4) / 2f, (float) (posY - 13f) + 6.5f, 0);
                            context.getMatrices().scale(armorScale.getValueFloat(), armorScale.getValueFloat(), 1f);
                            context.getMatrices().translate(-(tagX - 2 + (textWidth + 4) / 2f), -(float) ((posY - 13f) + 6.5f), 0);
                            context.getMatrices().translate(posX - 52.5 + item_offset, (float) (posY - 29f) + armorHeight.getValueFloat(), 0);
                            int percent = cached.armorPercents[i];
                            int percentColor = cached.armorPercentColors[i];
                            switch (armorMode.getValue()) {
                                case OnlyArmor -> {
                                    if (i > 0 && i < 5) {
                                        context.drawItem(armorComponent, 0, 0);
                                        context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    }
                                }
                                case Item -> {
                                    context.drawItem(armorComponent, 0, 0);
                                    context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                }
                                case Full -> {
                                    context.drawItem(armorComponent, 0, 0);
                                    context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    if (armorComponent.getMaxDamage() > 0) {
                                        String percentString = cached.armorPercentStrings[i];
                                        if (cached.font == Font.Fancy) {
                                            FontRenderers.ui.drawString(context.getMatrices(), percentString, 9 - FontRenderers.ui.getWidth(percentString) / 2, -FontRenderers.ui.getFontHeight() + 3, percentColor);
                                        } else {
                                            context.drawText(mc.textRenderer, percentString, 9 - mc.textRenderer.getWidth(percentString) / 2, -mc.textRenderer.fontHeight + 1, percentColor, true);
                                        }
                                    }
                                }
                                case Durability -> {
                                    context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    if (armorComponent.getMaxDamage() > 0) {
                                        if (!armorComponent.isItemBarVisible()) {
                                            int step = armorComponent.getItemBarStep();
                                            int j = armorComponent.getItemBarColor();
                                            int k = 2;
                                            int l = 13;
                                            context.fill(RenderLayer.getGuiOverlay(), k, l, k + 13, l + 2, -16777216);
                                            context.fill(RenderLayer.getGuiOverlay(), k, l, k + step, l + 1, j | -16777216);
                                        }
                                        String percentString = cached.armorPercentStrings[i];
                                        if (cached.font == Font.Fancy) {
                                            FontRenderers.ui.drawString(context.getMatrices(), percentString, 9 - FontRenderers.ui.getWidth(percentString) / 2, 7, percentColor);
                                        } else {
                                            context.drawText(mc.textRenderer, percentString, 9 - mc.textRenderer.getWidth(percentString) / 2, 5, percentColor, true);
                                        }
                                    }
                                }
                            }
                            context.getMatrices().pop();

                            if (this.enchants.getValue()) {
                                float enchantmentY = 0;
                                String[] enchantLines = cached.armorEnchantLines[i];
                                for (int index = 0; index < enchantLines.length; index++) {
                                    String encName = enchantLines[index];
                                    context.getMatrices().push();
                                    context.getMatrices().translate((posX - 50f + item_offset), (posY - 45f + enchantmentY), 0);
                                    context.drawText(mc.textRenderer, encName, 0, 0, -1, true);
                                    context.getMatrices().pop();
                                    enchantmentY -= 8f;
                                }
                            }
                        }
                        item_offset += 18f;
                    }
                }
                if (rect.booleanValue) {
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 2, (float) (posY - 13f), textWidth + 4, 11, rect.getValue());
                }
                Render2DUtil.drawRect(context.getMatrices(), tagX - 2, (float) (posY - 2f), textWidth + 4, 1.5f, COLOR_BAR_BG);
                float healthP = Math.max(0, Math.min(1, (ent.getHealth() + ent.getAbsorptionAmount()) / (ent.getMaxHealth() + ent.getAbsorptionAmount())));
                Render2DUtil.drawRect(context.getMatrices(), tagX - 2, (float) (posY - 2f), (textWidth + 4) * healthP, 1.5f,
                        healthP > 0.6f ? COLOR_BAR_GREEN :
                                healthP > 0.3f ? COLOR_BAR_YELLOW :
                                        COLOR_BAR_RED);
                int textColor = Kawaii.FRIEND.isFriend(ent) ? friendColor.getValue().getRGB() : this.color.getValue().getRGB();
                if (outline.booleanValue) {
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 3, (float) (posY - 14f), textWidth + 6, 1, outline.getValue());
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 3, (float) (posY - 2f), textWidth + 6, 1, outline.getValue());
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 3, (float) (posY - 14f), 1, 12, outline.getValue());
                    Render2DUtil.drawRect(context.getMatrices(), tagX + textWidth + 2, (float) (posY - 14f), 1, 12, outline.getValue());
                }
                Color renderColor = Kawaii.FRIEND.isFriend(ent) ? friendColor.getValue() : this.color.getValue();
                if (Kawaii.ENEMY.isEnemy(ent)) renderColor = enemyColor.getValue();
                
                drawStringWithRGBSupport(context, cached.text, tagX, (float) posY - 10, renderColor.getRGB(), cached.font, cached.font != Font.Fancy);

                context.getMatrices().pop();
            }
        }

        if (shouldPrune) {
            tagCache.entrySet().removeIf(e -> e.getValue().worldTime != worldTime);
        }
    }

    public static String getEntityPing(PlayerEntity entity) {
        if (mc.getNetworkHandler() == null) return "-1";
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        if (playerListEntry == null) return "-1";
        int ping = playerListEntry.getLatency();
        Formatting color = Formatting.GREEN;
        if (ping >= 100) color = Formatting.YELLOW;
        if (ping >= 250) color = Formatting.RED;
        return color.toString() + ping;
    }

    public static GameMode getEntityGamemode(PlayerEntity entity) {
        if (entity == null) return null;
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        return playerListEntry == null ? null : playerListEntry.getGameMode();
    }

    private String translateGamemode(GameMode gamemode) {
        if (gamemode == null) return "§7[BOT]";
        return switch (gamemode) {
            case SURVIVAL -> { if (mTag.getValue()) yield "\u00a7#mTag"; else yield "§b[S]"; }
            case CREATIVE -> "§c[C]";
            case SPECTATOR -> "§7[SP]";
            case ADVENTURE -> "§e[A]";
        };
    }

    private Formatting getHealthColor(@NotNull PlayerEntity entity) {
        int health = (int) ((int) entity.getHealth() + entity.getAbsorptionAmount());
        if (health >= 18) return Formatting.GREEN;
        if (health >= 12) return Formatting.YELLOW;
        if (health >= 6) return Formatting.RED;
        return Formatting.DARK_RED;
    }

    @Override
    public void onLogout() {
        slowFallExpiry.clear();
        potionMap.clear();
        lastStuckArrowCount.clear();
        lastPopCountMap.clear();
        tagCache.clear();
        trackedPotions.clear();
    }

    @Override
    public void onUpdate() {
        if (nullCheck()) return;
        long tick = mc.world.getTime();
        double radius = (mc.options.getClampedViewDistance() + 2) * 16.0;
        Box scanBox = mc.player.getBoundingBox().expand(radius);
        for (PotionEntity potion : mc.world.getEntitiesByClass(PotionEntity.class, scanBox, Entity::isAlive)) {
            trackedPotions.compute(potion.getId(), (id, track) -> {
                Vec3d pos = potion.getPos();
                if (track == null) {
                    return new PotionTrack(pos, tick);
                }
                track.pos = pos;
                track.lastSeenTick = tick;
                return track;
            });
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, PotionTrack>> iterator = trackedPotions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, PotionTrack> entry = iterator.next();
            PotionTrack track = entry.getValue();
            if (track.lastSeenTick != tick) {
                applySplashAt(track.pos, now);
                iterator.remove();
            }
        }
    }

    private void applySplashAt(Vec3d pos, long now) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            double dist = player.getPos().distanceTo(pos);
            if (dist > SPLASH_RADIUS) continue;
            double scale = 1.0 - (dist / SPLASH_RADIUS);
            if (scale <= 0) continue;
            int durationTicks = (int) Math.ceil((TURTLE_SPLASH_MS / 50.0) * scale);
            if (durationTicks < 20) continue;
            long expiry = now + durationTicks * 50L;
            potionMap.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
                    .merge(StatusEffects.SLOWNESS.value(), expiry, (oldVal, newVal) -> {
                        if (oldVal == -1L) return newVal;
                        if (oldVal < newVal) return newVal;
                        return oldVal;
                    });
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (nullCheck()) return;
        if (event.getEntity() instanceof PotionEntity potion) {
            trackedPotions.put(potion.getId(), new PotionTrack(potion.getPos(), mc.world.getTime()));
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.world == null) return;
        if (event.getPacket() instanceof EntityStatusEffectS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.getEntityId());
            if (entity instanceof PlayerEntity) {
                potionMap.computeIfAbsent(entity.getUuid(), k -> new ConcurrentHashMap<>())
                        .put(packet.getEffectId().value(), System.currentTimeMillis() + packet.getDuration() * 50L);
            }
        }
        if (event.getPacket() instanceof RemoveEntityStatusEffectS2CPacket packet) {
            Entity entity = packet.getEntity(mc.world);
            if (entity instanceof PlayerEntity) {
                Map<StatusEffect, Long> effects = potionMap.get(entity.getUuid());
                if (effects != null) {
                    effects.remove(packet.effect().value());
                }
            }
        }
        if (event.getPacket() instanceof EntitiesDestroyS2CPacket packet) {
            for (int id : packet.getEntityIds()) {
                PotionTrack track = trackedPotions.remove(id);
                if (track != null) {
                    applySplashAt(track.pos, System.currentTimeMillis());
                }
            }
        }
        if (event.getPacket() instanceof EntityAttributesS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.getEntityId());
            if (entity instanceof PlayerEntity) {
                for (EntityAttributesS2CPacket.Entry entry : packet.getEntries()) {
                    if (entry.attribute().matchesKey(EntityAttributes.GENERIC_MOVEMENT_SPEED.getKey().get())) {
                        boolean hasSlowness = false;
                        for (EntityAttributeModifier modifier : entry.modifiers()) {
                            if (modifier.id().toString().contains("slowness")) {
                                hasSlowness = true;
                                break;
                            }
                        }
                        if (hasSlowness) {
                            potionMap.computeIfAbsent(entity.getUuid(), k -> new ConcurrentHashMap<>())
                                    .merge(StatusEffects.SLOWNESS.value(), -1L, (oldVal, newVal) -> {
                                        if (oldVal > System.currentTimeMillis()) return oldVal;
                                        return newVal;
                                    });
                        } else {
                            Map<StatusEffect, Long> effects = potionMap.get(entity.getUuid());
                            if (effects != null) {
                                effects.remove(StatusEffects.SLOWNESS.value());
                            }
                        }
                    }
                }
            }
        }
    }

    public static float round2(double value) {
        return (float) (Math.round(value * 10.0) / 10.0);
    }

    public enum Font { Fancy, Fast }

    public enum Armor { None, Full, Durability, Item, OnlyArmor }

    private static final class CachedTag {
        private static final String[] EMPTY_STRINGS = new String[0];
        long worldTime;
        String text = "";
        Font font = Font.Fast;
        float textWidth = 0f;

        final ItemStack[] armorStacks = new ItemStack[6];
        final int[] armorPercents = new int[6];
        final int[] armorPercentColors = new int[6];
        final String[] armorPercentStrings = new String[6];
        final String[][] armorEnchantLines = new String[6][];

        CachedTag() {
            clearArmor();
        }

        void clearArmor() {
            for (int i = 0; i < 6; i++) {
                armorStacks[i] = ItemStack.EMPTY;
                armorPercents[i] = 0;
                armorPercentColors[i] = 0;
                armorPercentStrings[i] = "0";
                armorEnchantLines[i] = EMPTY_STRINGS;
            }
        }

        void fillArmor(PlayerEntity ent, boolean enableEnchants) {
            for (int i = 0; i < 6; i++) {
                ItemStack stack;
                if (i == 0) stack = ent.getMainHandStack();
                else if (i == 5) stack = ent.getOffHandStack();
                else stack = ent.getInventory().armor.get(4 - i);

                armorStacks[i] = stack;
                if (stack.isEmpty() || stack.getMaxDamage() <= 0) {
                    armorPercents[i] = 0;
                    armorPercentColors[i] = 0;
                    armorPercentStrings[i] = "0";
                    armorEnchantLines[i] = EMPTY_STRINGS;
                    continue;
                }

                float durability = stack.getMaxDamage() - stack.getDamage();
                int percent = (int) ((durability / (float) stack.getMaxDamage()) * 100F);
                armorPercents[i] = percent;
                armorPercentStrings[i] = String.valueOf(percent);
                if (percent <= 33) armorPercentColors[i] = COLOR_RED.getRGB();
                else if (percent <= 66) armorPercentColors[i] = COLOR_ORANGE.getRGB();
                else armorPercentColors[i] = COLOR_GREEN.getRGB();

                if (!enableEnchants) {
                    armorEnchantLines[i] = EMPTY_STRINGS;
                    continue;
                }

                ItemEnchantmentsComponent enchants = stack.getEnchantments();
                if (enchants == null || enchants.isEmpty()) {
                    armorEnchantLines[i] = EMPTY_STRINGS;
                    continue;
                }

            }
        }
    }
}
