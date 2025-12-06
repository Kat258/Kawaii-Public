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
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
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

    private static final Map<UUID, Integer> lastStuckArrowCount = new ConcurrentHashMap<>();

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
        return text != null && text.matches(".*[\\u4e00-\\u9fa5].*");
    }

    /**
     * 处理包含 mTag 标记的字符串渲染，支持 RGB 颜色
     * mTag 标记会被替换为 #ff80d0 颜色的 [M]
     */
    private void drawStringWithRGBSupport(DrawContext context, String text, float x, float y, 
                                          int defaultColor, Font fontMode, boolean shadow) {
        if (!text.contains("\u00a7#mTag")) {
            // 无 mTag 标记，正常渲染
            if (fontMode == Font.Fancy) {
                FontRenderers.ui.drawString(context.getMatrices(), text, x, y, defaultColor);
            } else {
                context.drawText(mc.textRenderer, text, (int)x, (int)y, defaultColor, shadow);
            }
            return;
        }

        // 有 mTag 标记，需要分段渲染
        String[] parts = text.split("\u00a7#mTag");
        float currentX = x;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            if (i > 0) {
                // 渲染 mTag 部分（[M]），使用 RGB 颜色 #ff80d0
                String mTag = "[M]";
                int mTagColor = 0xffff80d0; // RGB 颜色
                
                if (fontMode == Font.Fancy) {
                    FontRenderers.ui.drawString(context.getMatrices(), mTag, currentX, y, mTagColor);
                    currentX += FontRenderers.ui.getWidth(mTag);
                } else {
                    context.drawText(mc.textRenderer, mTag, (int)currentX, (int)y, mTagColor, shadow);
                    currentX += mc.textRenderer.getWidth(mTag);
                }
            }

            // 渲染普通部分
            if (!part.isEmpty()) {
                if (fontMode == Font.Fancy) {
                    FontRenderers.ui.drawString(context.getMatrices(), part, currentX, y, defaultColor);
                    currentX += FontRenderers.ui.getWidth(part);
                } else {
                    context.drawText(mc.textRenderer, part, (int)currentX, (int)y, defaultColor, shadow);
                    currentX += mc.textRenderer.getWidth(part);
                }
            }
        }
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        if (mc == null || mc.world == null) return;
        long now = System.currentTimeMillis();

        for (PlayerEntity ent : mc.world.getPlayers()) {
            if (ent == mc.player && mc.options.getPerspective().isFirstPerson() && Freecam.INSTANCE.isOff()) continue;

            double x = ent.prevX + (ent.getX() - ent.prevX) * mc.getTickDelta();
            double y = ent.prevY + (ent.getY() - ent.prevY) * mc.getTickDelta();
            double z = ent.prevZ + (ent.getZ() - ent.prevZ) * mc.getTickDelta();
            Vec3d vector = new Vec3d(x, y + height.getValue() + ent.getBoundingBox().getLengthY() + 0.3, z);
            Vec3d preVec = vector;
            vector = TextUtil.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
            if (vector.z > 0 && vector.z < 1) {
                Vector4d position = new Vector4d(vector.x, vector.y, vector.z, 0);
                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);

                String fallPrefix = "";
                if (showFallTime.getValue()) {
                    Long expiry = slowFallExpiry.get(ent.getUuid());
                    if (expiry != null) {
                        long remain = expiry - now;
                        if (remain > 0) {
                            int sec = (int) Math.ceil(remain / 1000.0);
                            int m = sec / 60;
                            int s = sec % 60;
                            fallPrefix = Formatting.AQUA.toString() + String.format("%02d:%02d ", m, s);
                        } else {
                            slowFallExpiry.remove(ent.getUuid());
                        }
                    }
                }

                String final_string = fallPrefix;

                if (god.getValue() && ent.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    final_string += "§4GOD ";
                }
                if (ping.getValue()) {
                    final_string += getEntityPing(ent) + "ms ";
                }
                if (gamemode.getValue()) {
                    final_string += translateGamemode(getEntityGamemode(ent)) + " ";
                }
                final_string += Formatting.RESET + ent.getName().getString();
                if (health.getValue()) {
                    final_string += " " + getHealthColor(ent) + round2(ent.getAbsorptionAmount() + ent.getHealth());
                }
                if (distance.getValue()) {
                    final_string += " " + Formatting.RESET + String.format("%.1f", mc.player.distanceTo(ent)) + "m";
                }
                if (pops.getValue()) {
                    Integer currentPopCount = Kawaii.POP.getPop(ent.getName().getString());
                    Integer lastPopCount = lastStuckArrowCount.getOrDefault(ent.getUuid(), 0);

                    if (currentPopCount > lastPopCount) {
                        slowFallExpiry.remove(ent.getUuid());
                        lastStuckArrowCount.put(ent.getUuid(), currentPopCount);
                    }

                    final_string += " §bPop" + " " + Formatting.LIGHT_PURPLE + currentPopCount.toString();
                }


                double posX = position.x;
                double posY = position.y;
                double endPosX = position.z;

                float diff = (float) (endPosX - posX) / 2;
                float textWidth;

                boolean hasCJK = isChinese(final_string);
                Font chosenFont = font.getValue();
                if (chosenFont == Font.Fancy && hasCJK) chosenFont = Font.Fast;

                if (chosenFont == Font.Fancy) {
                    textWidth = (FontRenderers.ui.getWidth(final_string));
                } else {
                    textWidth = mc.textRenderer.getWidth(final_string);
                }

                float tagX = (float) ((posX + diff - textWidth / 2));

                ArrayList<ItemStack> stacks = new ArrayList<>();
                stacks.add(ent.getMainHandStack());
                stacks.add(ent.getInventory().armor.get(3));
                stacks.add(ent.getInventory().armor.get(2));
                stacks.add(ent.getInventory().armor.get(1));
                stacks.add(ent.getInventory().armor.get(0));
                stacks.add(ent.getOffHandStack());

                context.getMatrices().push();
                context.getMatrices().translate(tagX - 2 + (textWidth + 4) / 2f, (float) (posY - 13f) + 6.5f, 0);
                float size = (float) Math.max(1 - MathHelper.sqrt((float) mc.cameraEntity.squaredDistanceTo(preVec)) * 0.01 * scaled.getValue(), 0);
                context.getMatrices().scale(Math.max(scale.getValueFloat() * size, minScale.getValueFloat()), Math.max(scale.getValueFloat() * size, minScale.getValueFloat()), 1f);
                context.getMatrices().translate(0, offset.getValueFloat() * MathHelper.sqrt((float) EntityUtil.getEyesPos().squaredDistanceTo(preVec)), 0);
                context.getMatrices().translate(-(tagX - 2 + (textWidth + 4) / 2f), -(float) ((posY - 13f) + 6.5f), 0);

                float item_offset = 0;
                if (armorMode.getValue() != Armor.None) {
                    int count = 0;
                    for (ItemStack armorComponent : stacks) {
                        count++;
                        if (!armorComponent.isEmpty()) {
                            context.getMatrices().push();
                            context.getMatrices().translate(tagX - 2 + (textWidth + 4) / 2f, (float) (posY - 13f) + 6.5f, 0);
                            context.getMatrices().scale(armorScale.getValueFloat(), armorScale.getValueFloat(), 1f);
                            context.getMatrices().translate(-(tagX - 2 + (textWidth + 4) / 2f), -(float) ((posY - 13f) + 6.5f), 0);
                            context.getMatrices().translate(posX - 52.5 + item_offset, (float) (posY - 29f) + armorHeight.getValueFloat(), 0);
                            float durability = armorComponent.getMaxDamage() - armorComponent.getDamage();
                            int percent = (int) ((durability / (float) armorComponent.getMaxDamage()) * 100F);
                            Color color;
                            if (percent <= 33) color = Color.RED; else if (percent <= 66) color = Color.ORANGE; else color = Color.GREEN;
                            switch (armorMode.getValue()) {
                                case OnlyArmor -> {
                                    if (count > 1 && count < 6) {
                                        DiffuseLighting.disableGuiDepthLighting();
                                        context.drawItem(armorComponent, 0, 0);
                                        context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    }
                                }
                                case Item -> {
                                    DiffuseLighting.disableGuiDepthLighting();
                                    context.drawItem(armorComponent, 0, 0);
                                    context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                }
                                case Full -> {
                                    DiffuseLighting.disableGuiDepthLighting();
                                    context.drawItem(armorComponent, 0, 0);
                                    context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    if (armorComponent.getMaxDamage() > 0) {
                                        if (chosenFont == Font.Fancy) {
                                            FontRenderers.ui.drawString(context.getMatrices(), String.valueOf(percent), 9 - FontRenderers.ui.getWidth(String.valueOf(percent)) / 2, -FontRenderers.ui.getFontHeight() + 3, color.getRGB());
                                        } else {
                                            context.drawText(mc.textRenderer, String.valueOf(percent), 9 - mc.textRenderer.getWidth(String.valueOf(percent)) / 2, -mc.textRenderer.fontHeight + 1, color.getRGB(), true);
                                        }
                                    }
                                }
                                case Durability -> {
                                    context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    if (armorComponent.getMaxDamage() > 0) {
                                        if (!armorComponent.isItemBarVisible()) {
                                            int i = armorComponent.getItemBarStep();
                                            int j = armorComponent.getItemBarColor();
                                            int k = 2;
                                            int l = 13;
                                            context.fill(RenderLayer.getGuiOverlay(), k, l, k + 13, l + 2, -16777216);
                                            context.fill(RenderLayer.getGuiOverlay(), k, l, k + i, l + 1, j | -16777216);
                                        }
                                        if (chosenFont == Font.Fancy) {
                                            FontRenderers.ui.drawString(context.getMatrices(), String.valueOf(percent), 9 - FontRenderers.ui.getWidth(String.valueOf(percent)) / 2, 7, color.getRGB());
                                        } else {
                                            context.drawText(mc.textRenderer, String.valueOf(percent), 9 - mc.textRenderer.getWidth(String.valueOf(percent)) / 2, 5, color.getRGB(), true);
                                        }
                                    }
                                }
                            }
                            context.getMatrices().pop();

                            if (this.enchants.getValue()) {
                                float enchantmentY = 0;
                                NbtList enchants = armorComponent.getEnchantments();
                                for (int index = 0; index < enchants.size(); ++index) {
                                    String id = enchants.getCompound(index).getString("id");
                                    short level = enchants.getCompound(index).getShort("lvl");
                                    String encName;
                                    switch (id) {
                                        case "minecraft:blast_protection" -> encName = "B" + level;
                                        case "minecraft:protection" -> encName = "P" + level;
                                        case "minecraft:thorns" -> encName = "T" + level;
                                        case "minecraft:sharpness" -> encName = "S" + level;
                                        case "minecraft:efficiency" -> encName = "E" + level;
                                        case "minecraft:unbreaking" -> encName = "U" + level;
                                        case "minecraft:power" -> encName = "PO" + level;
                                        default -> { continue; }
                                    }

                                    if (isChinese(encName)) {
                                        OrderedText orderedText = OrderedText.styledForwardsVisitedString(encName, Style.EMPTY);
                                        context.drawText(mc.textRenderer, orderedText, (int) (posX - 50 + item_offset), (int) (posY - 45 + enchantmentY), -1, false);
                                    } else {
                                        context.getMatrices().push();
                                        context.getMatrices().translate((posX - 50f + item_offset), (posY - 45f + enchantmentY), 0);
                                        context.drawText(mc.textRenderer, encName, 0, 0, -1, true);
                                        context.getMatrices().pop();
                                    }
                                    enchantmentY -= 8;
                                }
                            }
                        }
                        item_offset += 18f;
                    }
                }
                if (rect.booleanValue) {
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 2, (float) (posY - 13f), textWidth + 4, 11, rect.getValue());
                }
                Render2DUtil.drawRect(context.getMatrices(), tagX - 2, (float) (posY - 2f), textWidth + 4, 1.5f, new Color(0x80000000, true));
                Render2DUtil.drawRect(context.getMatrices(), tagX - 2, (float) (posY - 2f), (textWidth + 4) * Math.max(0, Math.min(1, (ent.getHealth() + ent.getAbsorptionAmount()) / (ent.getMaxHealth() + ent.getAbsorptionAmount()))), 1.5f,
                        Math.max(0, Math.min(1, (ent.getHealth() + ent.getAbsorptionAmount()) / (ent.getMaxHealth() + ent.getAbsorptionAmount()))) > 0.6f ? new Color(0x9900FF00, true) :
                                Math.max(0, Math.min(1, (ent.getHealth() + ent.getAbsorptionAmount()) / (ent.getMaxHealth() + ent.getAbsorptionAmount()))) > 0.3f ? new Color(0x99EEFF05, true) :
                                        new Color(0x99FF0000, true));
                int textColor = Kawaii.FRIEND.isFriend(ent) ? friendColor.getValue().getRGB() : this.color.getValue().getRGB();
                if (outline.booleanValue) {
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 3, (float) (posY - 14f), textWidth + 6, 1, outline.getValue());
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 3, (float) (posY - 2f), textWidth + 6, 1, outline.getValue());
                    Render2DUtil.drawRect(context.getMatrices(), tagX - 3, (float) (posY - 14f), 1, 12, outline.getValue());
                    Render2DUtil.drawRect(context.getMatrices(), tagX + textWidth + 2, (float) (posY - 14f), 1, 12, outline.getValue());
                }
                Color renderColor = Kawaii.FRIEND.isFriend(ent) ? friendColor.getValue() : this.color.getValue();
                if (Kawaii.ENEMY.isEnemy(ent)) renderColor = enemyColor.getValue();
                
                drawStringWithRGBSupport(context, final_string, tagX, (float) posY - 10, renderColor.getRGB(), chosenFont, chosenFont != Font.Fancy);

                context.getMatrices().pop();
            }
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

    public static float round2(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public enum Font { Fancy, Fast }

    public enum Armor { None, Full, Durability, Item, OnlyArmor }
}
