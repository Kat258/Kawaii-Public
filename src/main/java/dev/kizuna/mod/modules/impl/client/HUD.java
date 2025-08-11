package dev.kizuna.mod.modules.impl.client;

import dev.kizuna.Kawaii;
import dev.kizuna.api.utils.entity.InventoryUtil;
import dev.kizuna.api.utils.math.MathUtil;
import dev.kizuna.api.utils.render.TextUtil;
import dev.kizuna.mod.gui.font.FontRenderers;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.impl.exploit.Blink;
import dev.kizuna.mod.modules.impl.player.PacketMine;
import dev.kizuna.mod.modules.impl.player.TimerModule;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static dev.kizuna.mod.modules.settings.impl.ColorSetting.timer;

public class HUD extends Module {
    public static HUD INSTANCE;

    private final EnumSetting<HUD.Pages> page = add(new EnumSetting<>("Page", Pages.Module));
    //other
    public final BooleanSetting lowerCase = add(new BooleanSetting("LowerCase", false, () -> page.getValue() == Pages.Other));
    public final BooleanSetting up = add(new BooleanSetting("Up", false, () -> page.getValue() == Pages.Other));
    public final BooleanSetting customFont = add(new BooleanSetting("CustomFont", true, () -> page.getValue() == Pages.Other));
    //color
    private final SliderSetting pulseSpeed = add(new SliderSetting("Speed", 1, 0, 5, 0.1, () -> page.getValue() == Pages.Color));
    private final SliderSetting pulseCounter = add(new SliderSetting("Counter", 10, 1, 50, () -> page.getValue() == Pages.Color));
    public final BooleanSetting sync = add(new BooleanSetting("InfoColorSync", true, () -> page.getValue() == Pages.Color));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(208, 0, 0), () -> page.getValue() == Pages.Color));
    public final ColorSetting pulse = add(new ColorSetting("Pulse", new Color(79, 0, 0), () -> page.getValue() == Pages.Color));
    //module
    public final BooleanSetting fps = add(new BooleanSetting("FPS", true, () -> page.getValue() == Pages.Module));
    public final BooleanSetting ping = add(new BooleanSetting("Ping", true, () -> page.getValue() == Pages.Module));
    public final BooleanSetting tps = add(new BooleanSetting("TPS", true, () -> page.getValue() == Pages.Module));
    public final BooleanSetting ip = add(new BooleanSetting("IP", false, () -> page.getValue() == Pages.Module));
    public final BooleanSetting time = add(new BooleanSetting("Time", false, () -> page.getValue() == Pages.Module));
    public final BooleanSetting speed = add(new BooleanSetting("Speed", true, () -> page.getValue() == Pages.Module));
    public final BooleanSetting brand = add(new BooleanSetting("Brand", false, () -> page.getValue() == Pages.Module));
    public final BooleanSetting potions = add(new BooleanSetting("Potions", true, () -> page.getValue() == Pages.Module));
    public final BooleanSetting coords = add(new BooleanSetting("Coords", true, () -> page.getValue() == Pages.Module));

    public final BooleanSetting waterMark = add(new BooleanSetting("WaterMark", true, () -> page.getValue() == Pages.Module).setParent());
    //public final StringSetting waterMarkString = add(new StringSetting("Title", "%hackname% §f%version%-nightly §8 %time%", () -> page.getValue() == Pages.Module && waterMark.isOpen()));
    public final SliderSetting waterMarkoffset = add(new SliderSetting("WaterMarkOffset", 1, 0, 100, -1, () -> page.getValue() == Pages.Module && waterMark.isOpen()));

    private final BooleanSetting textRadar = add(new BooleanSetting("TextRadar", false, () -> page.getValue() == Pages.Module).setParent());
    private final SliderSetting updatedelay = add(new SliderSetting("UpdateDelay", 5, 0, 1000, () -> page.getValue() == Pages.Module && textRadar.isOpen()));
    private final BooleanSetting TextRadarhealth = add(new BooleanSetting("Health", false, () -> page.getValue() == Pages.Module && textRadar.isOpen()));

    public final BooleanSetting armor = add(new BooleanSetting("Armor", true, () -> page.getValue() == Pages.Module));

    private final BooleanSetting pvphud = add(new BooleanSetting("PVPHud", false, () -> page.getValue() == Pages.Module).setParent());
    private final BooleanSetting totemtext = add(new BooleanSetting("TotemText", false, () -> page.getValue() == Pages.Module && pvphud.isOpen()));
    private final BooleanSetting potiontext = add(new BooleanSetting("PotionText", false, () -> page.getValue() == Pages.Module && pvphud.isOpen()));
    private final SliderSetting pvpHudOffset = add(new SliderSetting("PVPOffset", 1, -45, 450, () -> page.getValue() == Pages.Module && pvphud.isOpen()));

    private final BooleanSetting mineprogress = add(new BooleanSetting("MineProgress", false, () -> page.getValue() == Pages.Module).setParent());
    private final SliderSetting yOffset = add(new SliderSetting("MineProgressY", 1, -45, 450, () -> page.getValue() == Pages.Module && mineprogress.isOpen()));

    private final BooleanSetting timerprogress = add(new BooleanSetting("TimerProgress", false, () -> page.getValue() == Pages.Module).setParent());
    private final SliderSetting timery = add(new SliderSetting("TimerOffset",1 ,-45 ,450, () -> page.getValue() == Pages.Module && timerprogress.isOpen()));

    private final BooleanSetting blinkhud = add (new BooleanSetting("BlinkHud", false, () -> page.getValue() == Pages.Module).setParent());
    private final SliderSetting blinky = add (new SliderSetting("BlinkOffset",1 ,-45 ,450, () -> page.getValue() == Pages.Module && blinkhud.isOpen()));

    private Map<String, Integer> players = new HashMap<>();
    int pulseProgress = 0;

    public HUD() {
        super("HUD", Category.Client);
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        if (timer.passed(updatedelay.getValue())) {
            players = getTextRadarMap();
            timer.reset();
        }
        pulseProgress -= pulseSpeed.getValueInt();
    }

    private String getTime() {
        return (new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.CHINESE)).format(new Date());
    }

    private final DecimalFormat decimal = new DecimalFormat("0.0");
    int x, y;
    DrawContext drawContext;

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        this.drawContext = drawContext;
        x = waterMarkoffset.getValueInt();
        y = this.yOffset.getValueInt();
        if (armor.getValue()) {
            Kawaii.GUI.armorHud.draw(drawContext, tickDelta, null);
        }
        if (waterMark.getValue()) {
            if (Kawaii.beta) {
                drawText(drawContext, Kawaii.NAME + "§f " + Kawaii.VERSION + "-nightly" + "§8 %time%".replaceAll("%time%",getTime()),
                        waterMarkoffset.getValueInt(),
                        waterMarkoffset.getValueInt());
            } else {
                drawText(drawContext, Kawaii.NAME + "§f " + Kawaii.VERSION + "§8 %time%".replaceAll("%time%",getTime()),
                        waterMarkoffset.getValueInt(),
                        waterMarkoffset.getValueInt());
            }
        }
        int fontHeight = getHeight();
        int height;
        int y;
        if (up.getValue()) {
            y = 1;
            height = -fontHeight;
        } else {
            y = mc.getWindow().getScaledHeight() - fontHeight;
            if (mc.currentScreen instanceof ChatScreen) {
                y -= 15;
            }
            height = fontHeight;
        }
        int windowWidth = mc.getWindow().getScaledWidth() - 1;
        if (potions.getValue()) {
            List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getStatusEffects());
            for (StatusEffectInstance potionEffect : effects) {
                StatusEffect potion = potionEffect.getEffectType();
                String power = "";
                switch (potionEffect.getAmplifier()) {
                    case 0 -> power = "I";
                    case 1 -> power = "II";
                    case 2 -> power = "III";
                    case 3 -> power = "IV";
                    case 4 -> power = "V";
                }
                String s = potion.getName().getString() + " " + power;
                String s2 = getDuration(potionEffect);
                String text = s + " " + s2;
                int x = getWidth(text);
                TextUtil.drawString(drawContext, text, windowWidth - x, y, potionEffect.getEffectType().getColor(), customFont.getValue());
                y -= height;
            }
        }
        if (brand.getValue()) {
            String brand = (mc.isInSingleplayer() ? "Vanilla" : mc.getNetworkHandler().getBrand().replaceAll("\\(.*?\\)", ""));
            int x = getWidth("ServerBrand " + brand);
            drawText(drawContext, "ServerBrand §f" + brand, windowWidth - x, y);
            y -= height;
        }
        if (time.getValue()) {
            String text = "Time §f" + getTime();
            int width = getWidth(text);
            drawText(drawContext, text, windowWidth - width, y);
            y -= height;
        }
        if (ip.getValue()) {
            int x = getWidth("Server " + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address));
            drawText(drawContext, "Server §f" + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getCurrentServerEntry().address), windowWidth - x, y);
            y -= height;
        }
        if (tps.getValue()) {
            int x = getWidth("TPS " + Kawaii.SERVER.getTPS() + " [" + Kawaii.SERVER.getCurrentTPS() + "]");
            drawText(drawContext, "TPS §f" + Kawaii.SERVER.getTPS() + " §7[§f" + Kawaii.SERVER.getCurrentTPS() + "§7]", windowWidth - x, y);
            y -= height;
        }
        if (speed.getValue()) {
            double x = mc.player.getX() - mc.player.prevX;
            // double y = mc.player.getY() - mc.player.prevY;
            double z = mc.player.getZ() - mc.player.prevZ;
            double dist = Math.sqrt(x * x + z * z) / 1000.0;
            double div = 0.05 / 3600.0;
            float timer = Kawaii.TIMER.get();
            final double speed = dist / div * timer;
            String text = String.format("Speed §f%skm/h",
                    decimal.format(speed));
            int width = getWidth(text);
            drawText(drawContext, text, windowWidth - width, y);
            y -= height;
        }
        if (fps.getValue()) {
            int x = getWidth("FPS " + Kawaii.FPS.getFps());
            drawText(drawContext, "FPS §f" + Kawaii.FPS.getFps(), windowWidth - x, y);
            y -= height;
        }
        if (ping.getValue()) {
            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            String ping;
            if (playerListEntry == null) {
                ping = "Unknown";
            } else {
                ping = String.valueOf(playerListEntry.getLatency());
            }
            int x = getWidth("Ping " + ping);
            drawText(drawContext, "Ping §f" + ping, windowWidth - x, y);
            y -= height;
        }

        if (coords.getValue()) {
            boolean inNether = mc.world.getRegistryKey().equals(World.NETHER);

            int posX = mc.player.getBlockX();
            int posY = mc.player.getBlockY();
            int posZ = mc.player.getBlockZ();

            float factor = !inNether ? 0.125F : 8.0F;

            int anotherWorldX = (int) (mc.player.getX() * factor);
            int anotherWorldZ = (int) (mc.player.getZ() * factor);

            String coordsString = "XYZ §f" + (inNether ? (posX + ", " + posY + ", " + posZ + " §7[§f" + anotherWorldX + ", " + anotherWorldZ + "§7]§f") : (posX + ", " + posY + ", " + posZ + "§7 [§f" + anotherWorldX + ", " + anotherWorldZ + "§7]"));

            drawText(drawContext, coordsString, (int) 2.0F, mc.getWindow().getScaledHeight() - fontHeight - (mc.currentScreen instanceof ChatScreen ? 15 : 0));
        }
        if (mineprogress.getValue()) {
            String string = PacketMine.INSTANCE.getInfo();
            if (!string.equals("Done")) {
                string = "[" + PacketMine.INSTANCE.getInfo() + "]";
            }
            drawText(drawContext, string, mc.getWindow().getWidth() / 4 - getWidth(string) / 2, mc.getWindow().getHeight() / 4 + yOffset.getValueInt());
        }
        if (textRadar.getValue()) {
            drawTextRadar(drawContext, waterMark.getValue() ? (int) (waterMarkoffset.getValue() + 2) : 2);
        }
        if (pvphud.getValue()) {
            int pvpHudX = mc.getWindow().getWidth() / 4;
            int pvpHudY = mc.getWindow().getHeight() / 4 + pvpHudOffset.getValueInt();
            int textHeight = getHeight() + 1;
            String t1 = "Totem §f" + InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
            String t2 = "Potion §f" + InventoryUtil.getPotionCount(StatusEffects.RESISTANCE);
            if (totemtext.getValue()) {
                drawText(drawContext, t1, (pvpHudX - getWidth(t1) / 2), pvpHudY);
                pvpHudY += textHeight;
            }
            if (potiontext.getValue()) {
                if (mc.player.hasStatusEffect(StatusEffects.RESISTANCE) && mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() >= 2) {
                    t2 += " §e" + (mc.player.getStatusEffect(StatusEffects.RESISTANCE).getDuration() / 20 + 1);
                }
                drawText(drawContext, t2, (pvpHudX - getWidth(t2) / 2), pvpHudY);
                pvpHudY += textHeight;
            }
        }
        if (timerprogress.getValue()){
            String string = "Timing " + TimerModule.INSTANCE.getInfo();
            drawText(drawContext, string, mc.getWindow().getWidth() / 4 - getWidth(string) / 2, mc.getWindow().getHeight() / 4 + timery.getValueInt());
        }
        if ((blinkhud.getValue()) && Blink.INSTANCE.isOn()){
            String string = "BlinkPacket" + Blink.INSTANCE.getInfo();
            drawText(drawContext, string, mc.getWindow().getWidth() / 4 - getWidth(string) / 2, mc.getWindow().getHeight() / 4 + blinky.getValueInt());
        }
    }

    private int getWidth(String s) {
        if (customFont.getValue()) {
            return (int) FontRenderers.ui.getWidth(s);
        }
        return mc.textRenderer.getWidth(s);
    }

    private int getHeight() {
        if (customFont.getValue()) {
            return (int) FontRenderers.ui.getFontHeight();
        }
        return mc.textRenderer.fontHeight;
    }

    private void drawTextRadar(DrawContext drawContext, int yOffset) {
        if (!players.isEmpty()) {
            int y = mc.textRenderer.fontHeight + 7 + yOffset;
            for (Map.Entry<String, Integer> player : players.entrySet()) {
                String text = player.getKey() + " ";
                drawText(drawContext, text, (int) 2.0F, y); // 使用 drawText 方法
                y += mc.textRenderer.fontHeight + 1;
            }
        }
    }

    private Map<String, Integer> getTextRadarMap() {
        Map<String, Integer> retval = new HashMap<>();

        DecimalFormat dfDistance = new DecimalFormat("#.#");
        dfDistance.setRoundingMode(RoundingMode.CEILING);
        StringBuilder distanceSB = new StringBuilder();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isInvisible() || player.getName().equals(mc.player.getName())) continue;

            int distanceInt = (int) mc.player.distanceTo(player);
            String distance = dfDistance.format(distanceInt);

            if (distanceInt >= 25) {
                distanceSB.append(Formatting.GREEN);

            } else if (distanceInt > 10) {
                distanceSB.append(Formatting.YELLOW);

            } else {
                distanceSB.append(Formatting.RED);
            }
            distanceSB.append(distance);

            retval.put((TextRadarhealth.getValue() ? (getHealthColor(player) + String.valueOf(round2(player.getAbsorptionAmount() + player.getHealth())) + " ") : "") + (Kawaii.FRIEND.isFriend(player) ? Formatting.AQUA : Formatting.RESET) + player.getName().getString() + " " + Formatting.WHITE + "[" + Formatting.RESET + distanceSB + "m" + Formatting.WHITE + "] " + Formatting.GREEN, (int) mc.player.distanceTo(player));

            distanceSB.setLength(0);
        }

        if (!retval.isEmpty()) {
            retval = MathUtil.sortByValue(retval, false);
        }

        return retval;
    }
    private Formatting getHealthColor(@NotNull PlayerEntity entity) {
        int health = (int) ((int) entity.getHealth() + entity.getAbsorptionAmount());
        if (health <= 15 && health > 7) return Formatting.YELLOW;
        if (health > 15) return Formatting.GREEN;
        return Formatting.RED;
    }
    public static float round2(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }
    private void drawText(DrawContext drawContext, String s, int x, int y) {
        if (sync.getValue()) {
            Arraylist.INSTANCE.counter--;
            if (lowerCase.getValue()) {
                s = s.toLowerCase();
            }
            TextUtil.drawString(drawContext, s, x, y, Arraylist.INSTANCE.getColor(Arraylist.INSTANCE.counter), shouldUseCustomFont(s));
            return;
        }
        if (pulse.booleanValue) {
            TextUtil.drawStringPulse(drawContext, s, x, y, color.getValue(), pulse.getValue(), pulseSpeed.getValue(), pulseCounter.getValueInt(), shouldUseCustomFont(s));
        } else {
            TextUtil.drawString(drawContext, s, x, y, color.getValue().getRGB(), shouldUseCustomFont(s));
        }
    }

    private boolean shouldUseCustomFont(String s) {
        if (s.matches(".*[\\u4e00-\\u9fa5].*")) {
            return false;
        }
        return customFont.getValue();
    }

    public static String getDuration(StatusEffectInstance pe) {
        if (pe.isInfinite()) {
            return "*:*";
        } else {
            int var1 = pe.getDuration();
            int mins = var1 / 1200;
            int sec = (var1 % 1200) / 20;

            return mins + ":" + sec;
        }

    }
    private enum Pages {
        Module,
        Offset,
        Color,
        Other,
        Font
    }
}
