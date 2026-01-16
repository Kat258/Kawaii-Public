package dev.kizuna.mod.modules.impl.combat;

import com.mojang.authlib.GameProfile;
import dev.kizuna.Kawaii;
import dev.kizuna.api.events.eventbus.EventListener;
import dev.kizuna.api.events.impl.PacketEvent;
import dev.kizuna.api.events.impl.UpdateWalkingPlayerEvent;
import dev.kizuna.api.utils.math.*;
import dev.kizuna.api.utils.render.CaptureMark;
import dev.kizuna.api.utils.render.JelloUtil;
import dev.kizuna.api.utils.world.BlockPosX;
import dev.kizuna.asm.accessors.IEntity;
import dev.kizuna.mod.modules.impl.client.AntiCheat;
import dev.kizuna.mod.modules.impl.client.ClientSetting;
import dev.kizuna.mod.modules.impl.client.Colors;
import dev.kizuna.mod.modules.impl.player.PacketMine;
import dev.kizuna.mod.modules.settings.impl.BooleanSetting;
import dev.kizuna.mod.modules.settings.impl.ColorSetting;
import dev.kizuna.mod.modules.settings.impl.EnumSetting;
import dev.kizuna.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.LookAtEvent;
import dev.kizuna.api.events.impl.Render3DEvent;
import dev.kizuna.api.utils.combat.CombatUtil;
import dev.kizuna.api.utils.entity.EntityUtil;
import dev.kizuna.api.utils.entity.InventoryUtil;
import dev.kizuna.api.utils.render.ColorUtil;
import dev.kizuna.api.utils.render.Render3DUtil;
import dev.kizuna.api.utils.world.BlockUtil;
import dev.kizuna.mod.modules.Module;
import dev.kizuna.mod.modules.impl.exploit.Blink;
import dev.kizuna.mod.modules.settings.SwingSide;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.collection.DefaultedList;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;

import static dev.kizuna.api.utils.world.BlockUtil.*;


public class KawaiiAura extends Module {
    public static KawaiiAura INSTANCE;
    public static BlockPos crystalPos;
    public static BlockPos basePos;
    public final Timer lastBreakTimer = new Timer();
    private final Timer placeTimer = new Timer(), noPosTimer = new Timer(), switchTimer = new Timer(), calcDelay = new Timer();
    private final Timer placeBaseTimer = new Timer();
    private final Timer baseDelayTimer = new Timer();

    //General
    private final BooleanSetting general = add (new BooleanSetting("General",true).setParent());
    private final BooleanSetting preferAnchor = add(new BooleanSetting("PreferAnchor", true, general::isOpen));
    private final BooleanSetting breakOnlyHasCrystal = add(new BooleanSetting("OnlyHold", true, general::isOpen));
    private final EnumSetting<SwingSide> swingMode = add(new EnumSetting<>("Swing", SwingSide.All, general::isOpen));
    private final BooleanSetting eatingPause = add(new BooleanSetting("EatingPause", true, general::isOpen));
    private final SliderSetting switchCooldown = add(new SliderSetting("SwitchPause", 100, 0, 1000, general::isOpen).setSuffix("ms"));
    private final SliderSetting targetRange = add(new SliderSetting("TargetRange", 12.0, 0.0, 20.0, general::isOpen).setSuffix("m"));
    private final SliderSetting updateDelay = add(new SliderSetting("UpdateDelay", 50, 0, 1000, general::isOpen).setSuffix("ms"));
    private final SliderSetting wallRange = add(new SliderSetting("WallRange", 6.0, 0.0, 6.0, general::isOpen).setSuffix("m"));
    //Rotate
    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", true).setParent());
    private final BooleanSetting onBreak = add(new BooleanSetting("OnBreak", false, rotate::isOpen));
    private final SliderSetting yOffset = add(new SliderSetting("YOffset", 0.05, 0, 1, 0.01, () -> rotate.isOpen() && onBreak.getValue()));
    private final BooleanSetting yawStep = add(new BooleanSetting("YawStep", false, rotate::isOpen));
    private final SliderSetting steps = add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> rotate.isOpen() && yawStep.getValue()));
    private final BooleanSetting checkFov = add(new BooleanSetting("OnlyLooking", true, () -> rotate.isOpen() && yawStep.getValue()));
    private final SliderSetting fov = add(new SliderSetting("Fov", 30, 0, 50, () -> rotate.isOpen() && yawStep.getValue() && checkFov.getValue()));
    private final SliderSetting priority = add(new SliderSetting("Priority", 10,0 ,100, () -> rotate.isOpen() && yawStep.getValue()));
    //Place
    private final BooleanSetting place = add(new BooleanSetting("Place", true).setParent());
    private final SliderSetting minDamage = add(new SliderSetting("Min", 5.0, 0.0, 36.0, place::isOpen).setSuffix("dmg"));
    private final SliderSetting maxSelf = add(new SliderSetting("Self", 12.0, 0.0, 36.0, place::isOpen).setSuffix("dmg"));
    private final SliderSetting range = add(new SliderSetting("Range", 5.0, 0.0, 6, place::isOpen).setSuffix("m"));
    private final SliderSetting noSuicide = add(new SliderSetting("NoSuicide", 3.0, 0.0, 10.0, place::isOpen).setSuffix("hp"));
    private final BooleanSetting smart = add(new BooleanSetting("Smart", true, place::isOpen));
    private final SliderSetting placeDelay = add(new SliderSetting("PlaceDelay", 300, 0, 1000, place::isOpen).setSuffix("ms"));
    private final BooleanSetting smartPlace = add(new BooleanSetting("SmartDelay", false, place::isOpen));
    private final SliderSetting placeLHealth = add(new SliderSetting("Health", 10.0, 0.0, 36.0, () -> place.isOpen() && smartPlace.getValue()).setSuffix("hp"));
    private final SliderSetting placeLDelay = add(new SliderSetting("LowDelay", 100, 0, 1000, () -> place.isOpen() && smartPlace.getValue()).setSuffix("ms"));
    private final EnumSetting<Sequential> sequential = add(new EnumSetting<>("Sequential", Sequential.NONE, place::isOpen));
    private final EnumSetting<SwapMode> autoSwap = add(new EnumSetting<>("AutoSwap", SwapMode.Off, place::isOpen));
    private final BooleanSetting afterBreak = add(new BooleanSetting("AfterBreak", true, place::isOpen));
    private final SliderSetting autoMinDamage = add(new SliderSetting("PistonMin", 5.0, 0.0, 36.0, place::isOpen).setSuffix("dmg"));
    //Base
    private final BooleanSetting base = add(new BooleanSetting("Base",false).setParent());
    private final SliderSetting minBaseDamage = add(new SliderSetting("BaseMin", 5.0, 0.0, 36.0, base::isOpen).setSuffix("dmg"));
    private final SliderSetting minBaseRange = add(new SliderSetting("MinBaseRange", 5.0, 0.0, 6, base::isOpen).setSuffix("m"));
    private final SliderSetting maxBaseRange = add(new SliderSetting("MaxBaseRange", 5.0, 0.0, 36.0, base::isOpen).setSuffix("m"));
    private final SliderSetting placeBaseDelay = add(new SliderSetting("BaseDelay", 300, 0, 1000, base::isOpen).setSuffix("ms"));
    private final BooleanSetting detectMining = add(new BooleanSetting("DetectMining", false, base::isOpen));
    //Break
    private final BooleanSetting breakSetting = add(new BooleanSetting("Break", true).setParent());
    private final SliderSetting breakDelay = add(new SliderSetting("BreakDelay", 300, 0, 1000, breakSetting::isOpen).setSuffix("ms"));
    private final BooleanSetting smartBreak = add(new BooleanSetting("SmartDelay", false, breakSetting::isOpen));
    private final SliderSetting breakLHealth = add(new SliderSetting("Health", 10.0, 0.0, 36.0, () -> breakSetting.isOpen() && smartBreak.getValue()).setSuffix("hp"));
    private final SliderSetting breakLDelay = add(new SliderSetting("LowDelay", 100, 0, 1000, () -> breakSetting.isOpen() && smartBreak.getValue()).setSuffix("ms"));
    private final SliderSetting minAge = add(new SliderSetting("MinAge", 0, 0, 20, breakSetting::isOpen).setSuffix("tick"));
    private final BooleanSetting breakRemove = add(new BooleanSetting("Remove", false, breakSetting::isOpen));
    private final BooleanSetting onlyTick = add(new BooleanSetting("OnlyTick", true, breakSetting::isOpen));
    //Misc
    private final BooleanSetting misc = add(new BooleanSetting("Misc", true).setParent());
    private final BooleanSetting ignoreMine = add(new BooleanSetting("IgnoreMine", true, misc::isOpen));
    private final SliderSetting constantProgress = add(new SliderSetting("Progress", 90.0, 0.0, 100.0, () -> misc.isOpen() && ignoreMine.isOpen2()).setSuffix("%"));
    private final BooleanSetting antiSurround = add(new BooleanSetting("AntiSurround", false, misc::isOpen).setParent2());
    private final SliderSetting antiSurroundMax = add(new SliderSetting("WhenLower", 5.0, 0.0, 36.0, () -> misc.isOpen2() && antiSurround.isOpen2()).setSuffix("dmg"));
    private final BooleanSetting slowPlace = add(new BooleanSetting("Timeout", true, misc::isOpen).setParent2());
    private final SliderSetting slowDelay = add(new SliderSetting("TimeoutDelay", 600, 0, 2000, () -> misc.isOpen() && slowPlace.isOpen2()).setSuffix("ms"));
    private final SliderSetting slowMinDamage = add(new SliderSetting("TimeoutMin", 1.5, 0.0, 36.0, () -> misc.isOpen() && slowPlace.isOpen2()).setSuffix("dmg"));
    private final BooleanSetting forcePlace = add(new BooleanSetting("ForcePlace", true, misc::isOpen).setParent2());
    private final SliderSetting forceMaxHealth = add(new SliderSetting("LowerThan", 7, 0, 36, () -> misc.isOpen() && forcePlace.isOpen2()).setSuffix("health"));
    private final SliderSetting forceMin = add(new SliderSetting("ForceMin", 1.5, 0.0, 36.0, () -> misc.isOpen() && forcePlace.isOpen2()).setSuffix("dmg"));
    private final BooleanSetting armorBreaker = add(new BooleanSetting("ArmorBreaker", true, misc::isOpen).setParent2());
    private final SliderSetting maxDurable = add(new SliderSetting("MaxDurable", 8, 0, 100, () -> misc.isOpen() && armorBreaker.isOpen2()).setSuffix("%"));
    private final SliderSetting armorBreakerDamage = add(new SliderSetting("BreakerMin", 3.0, 0.0, 36.0, () -> misc.isOpen() && armorBreaker.isOpen2()).setSuffix("dmg"));
    private final SliderSetting syncTimeout = add(new SliderSetting("WaitTimeOut", 500, 0, 2000, 10, misc::isOpen));
    private final BooleanSetting forceWeb = add(new BooleanSetting("ForceWeb", true, misc::isOpen).setParent2());
    public final BooleanSetting airPlace = add(new BooleanSetting("AirPlace", false, () -> misc.isOpen() && forceWeb.isOpen2()));
    public final BooleanSetting replace = add(new BooleanSetting("Replace", false, () -> misc.isOpen() && forceWeb.isOpen2()));
    private final BooleanSetting websync = add(new BooleanSetting("WebSync", true, misc::isOpen).setParent2());
    private final SliderSetting hurtTime = add(new SliderSetting("HurtTime", 10, 0, 10, 1, websync::isOpen2).setSuffix("Tick"));
    private final SliderSetting waitHurt = add(new SliderSetting("WaitHurt", 10, 0, 10, 1, websync::isOpen2).setSuffix("Tick"));
    //Calc
    private final BooleanSetting calc = add(new BooleanSetting("Calc", true).setParent());
    private final BooleanSetting thread = add(new BooleanSetting("Thread", true, calc::isOpen));
    private final BooleanSetting doCrystal = add(new BooleanSetting("ThreadInteract", false, calc::isOpen));
    private final BooleanSetting lite = add(new BooleanSetting("LessCPU", false, calc::isOpen));
    private final SliderSetting predictTicks = add(new SliderSetting("Predict", 4, 0, 10, calc::isOpen).setSuffix("ticks"));
    private final BooleanSetting terrainIgnore = add(new BooleanSetting("TerrainIgnore", true, calc::isOpen));
    //Render
    private final BooleanSetting render = add(new BooleanSetting("Render", true).setParent());
    private final BooleanSetting sync = add(new BooleanSetting("Sync", true, () -> render.isOpen() && render.getValue()));
    private final BooleanSetting shrink = add(new BooleanSetting("Shrink", true, () -> render.isOpen() && render.getValue()));
    private final ColorSetting box = add(new ColorSetting("Box", new Color(255, 255, 255, 255), () -> render.isOpen() && render.getValue())).injectBoolean(true);
    private final ColorSetting fill = add(new ColorSetting("Fill", new Color(255, 255, 255, 100), () -> render.isOpen() && render.getValue()).injectBoolean(true));
    private final SliderSetting sliderSpeed = add(new SliderSetting("SliderSpeed", 0.2, 0.01, 1, 0.01, () -> render.isOpen() && render.getValue()));
    private final SliderSetting startFadeTime = add(new SliderSetting("StartFade", 0.3d, 0d, 2d, 0.01, () -> render.isOpen() && render.getValue()).setSuffix("s"));
    private final SliderSetting fadeSpeed = add(new SliderSetting("FadeSpeed", 0.2d, 0.01d, 1d, 0.01, () -> render.isOpen() && render.getValue()));
    private final SliderSetting lineWidth = add(new SliderSetting("LineWidth", 1.5d, 0.01d, 3d, 0.01, () -> render.isOpen() && render.getValue()));
    private final BooleanSetting rainbow = add(new BooleanSetting("Rainbow", false,render::isOpen).setParent2());
    private final SliderSetting rainbowSpeed = add(new SliderSetting("RainbowSpeed", 4, 1, 10, 0.1, () -> render.isOpen2() && rainbow.getValue()));
    private final SliderSetting saturation = add(new SliderSetting("Saturation", 130.0f, 1.0f, 255.0f, () -> render.isOpen2() && rainbow.getValue()));
    private final SliderSetting rainbowDelay = add(new SliderSetting("Delay", 350, 0, 1000, () -> render.isOpen2() && rainbow.getValue()));
    private final SliderSetting rbalpha = add(new SliderSetting("Alpha",80, 0,255, () -> render.isOpen2() && rainbow.getValue()));
    private final ColorSetting text = add(new ColorSetting("Text", new Color(-1), render::isOpen).injectBoolean(true));

    private final EnumSetting<TargetESP> mode = add(new EnumSetting<>("TargetESP", TargetESP.Jello, render::isOpen));
    private final ColorSetting color = add(new ColorSetting("TargetColor", new Color(255, 255, 255, 50), render::isOpen));
    private final ColorSetting hitColor = add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), render::isOpen));
    public final SliderSetting animationTime = add(new SliderSetting("AnimationTime", 200, 0, 2000, 1, () -> render.isOpen() && mode.is(TargetESP.Box)));
    public final EnumSetting<Easing> ease = add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> render.isOpen() && mode.is(TargetESP.Box)));

    public PlayerEntity displayTarget;
    private final Animation animation = new Animation();
    public float breakDamage, tempDamage, lastDamage;
    public float breakBaseDamage, tempBaseDamage, lastBaseDamage;
    public Vec3d directionVec = null;
    public Vec3d baseDirectionVec = null;
    double currentFade = 0;
    public BlockPos tempPos, breakPos, syncPos;
    private BlockPos baseTempPos;
    private Vec3d placeVec3d, curVec3d;
    private final java.util.HashMap<UUID, PredictEntity> predictCache = new java.util.HashMap<>();
    private long lastMaxEntityIdWorldTime = Long.MIN_VALUE;
    private int cachedMaxEntityId = -1;

    public enum TargetESP {
        Box,
        Jello,
        NurikZapen,
        None
    }

    public KawaiiAura() {
        super("KawaiiAura", Category.Combat);
        INSTANCE = this;
        Kawaii.EVENT_BUS.subscribe(new CrystalRender());
    }

    public static boolean canSee(Vec3d from, Vec3d to) {
        HitResult result = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result == null || result.getType() == HitResult.Type.MISS;
    }

    private int getMaxEntityId() {
        if (mc.world == null) return -1;
        long worldTime = mc.world.getTime();
        if (lastMaxEntityIdWorldTime == worldTime) return cachedMaxEntityId;

        int id = -1;
        for (Entity e : mc.world.getEntities()) {
            int eid = e.getId();
            if (eid > id) id = eid;
        }

        lastMaxEntityIdWorldTime = worldTime;
        cachedMaxEntityId = id;
        return id;
    }

    private PlayerEntity getPredict(PlayerEntity player) {
        if (predictTicks.getValueInt() <= 0 || mc.world == null) {
            return player;
        }

        UUID uuid = player.getUuid();
        PredictEntity predict = predictCache.get(uuid);
        if (predict == null || predict.getWorld() != mc.world) {
            predict = new PredictEntity((ClientWorld) mc.world);
            predictCache.put(uuid, predict);
        }

        predict.setSource(player);
        predict.setPosition(player.getPos().add(CombatUtil.getMotionVec(player, predictTicks.getValueInt(), true)));
        predict.setYaw(player.getYaw());
        predict.setPitch(player.getPitch());
        predict.setHealth(player.getHealth());
        predict.setAbsorptionAmount(player.getAbsorptionAmount());
        predict.prevX = player.prevX;
        predict.prevY = player.prevY;
        predict.prevZ = player.prevZ;
        predict.setOnGround(player.isOnGround());

        DefaultedList<ItemStack> sourceArmor = player.getInventory().armor;
        DefaultedList<ItemStack> targetArmor = predict.getInventory().armor;
        for (int i = 0; i < sourceArmor.size(); i++) {
            targetArmor.set(i, sourceArmor.get(i));
        }

        predict.clearStatusEffects();
        for (StatusEffectInstance se : player.getStatusEffects()) {
            predict.addStatusEffect(new StatusEffectInstance(se));
        }

        return predict;
    }

    DecimalFormat df = new DecimalFormat("0.0");
    @Override
    public String getInfo() {
        return  crystalPos != null
                ? displayTarget.getName().getString()
                + " , "
                + (Math.floor((double)this.lastBreakTimer.getPassedTimeMs()) == (double)this.lastBreakTimer.getPassedTimeMs() ? (int)this.lastBreakTimer.getPassedTimeMs() : String.format(String.valueOf(this.lastBreakTimer.getPassedTimeMs())))
                + "ms"
                + " , "
                + (Math.floor(this.lastDamage) == (double)this.lastDamage ? (int)this.lastDamage : String.format("%.1f", this.lastDamage))
                : null;
    }

    @Override
    public void onDisable() {
        crystalPos = null;
        tempPos = null;
        if (base.getValue()) {
            basePos = null;
            baseTempPos = null;
        }
    }

    @Override
    public void onEnable() {
        crystalPos = null;
        tempPos = null;
        breakPos = null;
        if (base.getValue()) {
            basePos = null;
            baseTempPos = null;
        }
        displayTarget = null;
        syncTimer.reset();
        lastBreakTimer.reset();
    }

    @Override
    public void onThread() {
        if (thread.getValue()) {
            updateCrystalPos();
        }
    }
    public Color getColor(int counter) {
        if (rainbow.getValue()) {
            return rainbow(counter);
        }
        int alpha = (int) (rbalpha.getValue() * 255.0 / 100.0);
        return new Color(255, 255, 255, alpha);
    }

    public Color rainbow(int delay) {
        double rainbowState = Math.ceil((System.currentTimeMillis() * rainbowSpeed.getValue() + delay * rainbowDelay.getValue()) / 20.0);
        float alpha = (float) (rbalpha.getValue() / 255.0f);
        Color baseColor = Color.getHSBColor((float) (rainbowState % 360.0 / 360), saturation.getValueFloat() / 255.0f, 1.0f);
        int alphaInt = (int) (alpha * 255);
        return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alphaInt);
    }

    @Override
    public void onUpdate() {
        if (!thread.getValue()) {
            updateCrystalPos();
        }
        doInteract();
    }

    @EventHandler
    public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
        if (!thread.getValue()) updateCrystalPos();
        if (!onlyTick.getValue()) doInteract();
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!thread.getValue()) updateCrystalPos();
        if (!onlyTick.getValue()) doInteract();
        if (displayTarget != null && !noPosTimer.passedMs(500)) {
            doRender(matrixStack, mc.getTickDelta(), displayTarget, mode.getValue());
        }
    }

    public void doRender(MatrixStack matrixStack, float partialTicks, Entity entity, TargetESP mode) {
        switch (mode) {
            case Box -> Render3DUtil.draw3DBox(matrixStack, ((IEntity) entity).getDimensions().getBoxAt(new Vec3d(MathUtil.interpolate(entity.lastRenderX, entity.getX(), partialTicks), MathUtil.interpolate(entity.lastRenderY, entity.getY(), partialTicks), MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), partialTicks))).expand(0, 0.1, 0), ColorUtil.fadeColor(color.getValue(), hitColor.getValue(), animation.get(0, animationTime.getValueInt(), ease.getValue())), false, true);
            case NurikZapen -> CaptureMark.render(entity, color.getValue());
            case Jello -> JelloUtil.drawJello(matrixStack, entity, color.getValue());
        }
    }

    private void doInteract() {
        baseDirectionVec = null;
        if (shouldReturn()) {
            return;
        }
        if (breakPos != null) {
            doBreak(breakPos);
            breakPos = null;
        }
        if (crystalPos != null) {
            doCrystal(crystalPos);
        }
        if (base.getValue() && basePos != null) {
            doPlaceBase(basePos);
        }
    }

    @EventHandler()
    public void onRotate(LookAtEvent event) {
        if (rotate.getValue() && yawStep.getValue() && directionVec != null && !noPosTimer.passed(1000)) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    @EventHandler(priority = -199)
    public void onPacketSend(PacketEvent.Send event) {
        if (event.isCancelled()) return;
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer.reset();
        }
    }

    private void updateCrystalPos() {
        getCrystalPos();
        update();
        lastDamage = tempDamage;
        crystalPos = tempPos;
        lastBaseDamage = tempBaseDamage;
        basePos = baseTempPos;
    }

    private boolean shouldReturn() {
        if (eatingPause.getValue() && mc.player.isUsingItem() || Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue() || PacketThrow.INSTANCE.getBind().isPressed() || PacketThrow.INSTANCE.isOn() && PacketThrow.INSTANCE.pauseCombat.getValue()) {
            lastBreakTimer.reset();
            return true;
        }
        if (preferAnchor.getValue() && AutoAnchor.INSTANCE.currentPos != null) {
            lastBreakTimer.reset();
            return true;
        }
            return false;
    }

    private void getCrystalPos() {
        if (nullCheck()) {
            lastBreakTimer.reset();
            tempPos = null;
            return;
        }
        if (!calcDelay.passedMs((long) updateDelay.getValue())) return;
        if (breakOnlyHasCrystal.getValue() && !mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) && !mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL) && !findCrystal()) {
            lastBreakTimer.reset();
            tempPos = null;
            return;
        }
        boolean shouldReturn = shouldReturn();
        calcDelay.reset();
        breakPos = null;
        breakDamage = 0;
        tempPos = null;
        tempDamage = 0f;
        Vec3d eyePos = mc.player.getEyePos();
        double rangeVal = range.getValue();
        double rangeSq = rangeVal * rangeVal;
        double wallRangeVal = wallRange.getValue();
        double wallRangeSq = wallRangeVal * wallRangeVal;
        ArrayList<PlayerAndPredict> list = new ArrayList<>();
        for (PlayerEntity target : CombatUtil.getEnemies(targetRange.getValueFloat())) {
            if (target.hurtTime <= hurtTime.getValueInt()) {
                list.add(new PlayerAndPredict(target));
            }
        }
        PlayerAndPredict self = new PlayerAndPredict(mc.player);
        if (list.isEmpty()) {
            lastBreakTimer.reset();
        } else {
            for (BlockPos pos : BlockUtil.getSphere((float) range.getValue() + 1)) {
                if (behindWall(pos)) continue;
                double cx = pos.getX() + 0.5;
                double cy = pos.getY();
                double cz = pos.getZ() + 0.5;
                double dx = cx - eyePos.x;
                double dy = cy - eyePos.y;
                double dz = cz - eyePos.z;
                if (dx * dx + dy * dy + dz * dz > rangeSq) {
                    continue;
                }
                if (!canTouch(pos.down())) continue;
                if (!canPlaceCrystal(pos, true, false)) continue;
                for (PlayerAndPredict pap : list) {
                    if (lite.getValue() && liteCheck(new Vec3d(cx, cy, cz), pap.predict.getPos())) {
                        continue;
                    }
                    float damage = calculateDamage(pos, pap.player, pap.predict);
                    if (tempPos == null || damage > tempDamage) {
                        float selfDamage = calculateDamage(pos, self.player, self.predict);
                        if (selfDamage > maxSelf.getValue()) continue;
                        if (noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())
                            continue;
                        if (damage < pap.targetHealth) {
                            if (damage < pap.minTargetDamage) continue;
                            if (smart.getValue()) {
                                if (pap.minTargetDamage == forceMin.getValue()) {
                                    if (damage < selfDamage - 2.5) {
                                        continue;
                                    }
                                } else {
                                    if (damage < selfDamage) {
                                        continue;
                                    }
                                }
                            }
                        }
                        displayTarget = pap.player;
                        tempPos = pos;
                        tempDamage = damage;
                    }
                }
            }
            double scanRange = Math.max(rangeVal, wallRangeVal) + 2.0;
            Box scanBox = new Box(
                    eyePos.x - scanRange, eyePos.y - scanRange - 2.0, eyePos.z - scanRange,
                    eyePos.x + scanRange, eyePos.y + scanRange + 2.0, eyePos.z + scanRange
            );
            for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, scanBox, Entity::isAlive)) {
                Vec3d cpos = crystal.getPos();
                double cdx = cpos.x - eyePos.x;
                double cdy = cpos.y - eyePos.y;
                double cdz = cpos.z - eyePos.z;
                double cDistSq = cdx * cdx + cdy * cdy + cdz * cdz;
                if (!mc.player.canSee(crystal) && cDistSq > wallRangeSq) continue;
                if (cDistSq > rangeSq) continue;

                for (PlayerAndPredict pap : list) {
                    float damage = calculateDamage(cpos, pap.player, pap.predict);
                    if (breakPos == null || damage > breakDamage) {
                        float selfDamage = calculateDamage(cpos, self.player, self.predict);
                        if (selfDamage > maxSelf.getValue()) continue;
                        if (noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())
                            continue;
                        if (damage < pap.targetHealth) {
                            if (damage < pap.minTargetDamage) continue;
                            if (smart.getValue()) {
                                if (pap.minTargetDamage == forceMin.getValue()) {
                                    if (damage < selfDamage - 2.5) {
                                        continue;
                                    }
                                } else {
                                    if (damage < selfDamage) {
                                        continue;
                                    }
                                }
                            }
                        }
                        breakPos = new BlockPosX(cpos);
                        if (damage > tempDamage) {
                            displayTarget = pap.player;
                        }
                    }
                }
            }
            if (doCrystal.getValue() && breakPos != null && !shouldReturn) {
                doBreak(breakPos);
                breakPos = null;
            }
            if (antiSurround.getValue() && PacketMine.getBreakPos() != null && PacketMine.progress >= 0.9 && !BlockUtil.hasEntity(PacketMine.getBreakPos(), false)) {
                if (tempDamage <= antiSurroundMax.getValueFloat()) {
                    for (PlayerAndPredict pap : list) {
                        for (Direction i : Direction.values()) {
                            if (i == Direction.DOWN || i == Direction.UP) continue;
                            BlockPos offsetPos = new BlockPosX(pap.player.getPos().add(0, 0.5, 0)).offset(i);
                            if (offsetPos.equals(PacketMine.getBreakPos())) {
                                if (canPlaceCrystal(offsetPos.offset(i), false, false)) {
                                    float selfDamage = calculateDamage(offsetPos.offset(i), self.player, self.predict);
                                    if (selfDamage < maxSelf.getValue() && !(noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())) {
                                        tempPos = offsetPos.offset(i);
                                        if (doCrystal.getValue() && tempPos != null && !shouldReturn) {
                                            doCrystal(tempPos);
                                        }
                                        return;
                                    }
                                }
                                for (Direction ii : Direction.values()) {
                                    if (ii == Direction.DOWN || ii == i) continue;
                                    if (canPlaceCrystal(offsetPos.offset(ii), false, false)) {
                                        float selfDamage = calculateDamage(offsetPos.offset(ii), self.player, self.predict);
                                        if (selfDamage < maxSelf.getValue() && !(noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())) {
                                            tempPos = offsetPos.offset(ii);
                                            if (doCrystal.getValue() && tempPos != null && !shouldReturn) {
                                                doCrystal(tempPos);
                                            }
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (doCrystal.getValue() && tempPos != null && !shouldReturn) {
            doCrystal(tempPos);
        }
    }

    private void update() {
        if (base.getValue()) {
            if (nullCheck()) return;
            if (!baseDelayTimer.passedMs((long) updateDelay.getValue())) return;
            if (eatingPause.getValue() && mc.player.isUsingItem()) {
                baseTempPos = null;
                return;
            }
            if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
                baseTempPos = null;
                return;
            }
            baseDelayTimer.reset();
            breakBaseDamage = 0;
            baseTempPos = null;
            tempBaseDamage = 0f;
            ArrayList<PlayerAndPredict> list = new ArrayList<>();
            for (PlayerEntity target : CombatUtil.getEnemies(maxBaseRange.getValueFloat())) {
                if (target.hurtTime <= hurtTime.getValueInt()) {
                    list.add(new PlayerAndPredict(target));
                }
            }
            PlayerAndPredict self = new PlayerAndPredict(mc.player);
            if (!list.isEmpty()) {
                Vec3d eyePos = mc.player.getEyePos();
                double minBaseRangeVal = minBaseRange.getValue();
                double minBaseRangeSq = minBaseRangeVal * minBaseRangeVal;
                for (BlockPos pos : BlockUtil.getSphere((float) minBaseRangeVal + 1)) {
                    CombatUtil.modifyPos = null;
                    double cx = pos.getX() + 0.5;
                    double cy = pos.getY();
                    double cz = pos.getZ() + 0.5;
                    double dx = cx - eyePos.x;
                    double dy = cy - eyePos.y;
                    double dz = cz - eyePos.z;
                    if (dx * dx + dy * dy + dz * dz > minBaseRangeSq) {
                            continue;
                        }
                        if (!canPlaceBase(pos, true, false)) continue;
                        CombatUtil.modifyPos = pos.down();
                        CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
                        if (behindWall(pos)) continue;
                        if (!canTouch(pos.down())) continue;
                        for (PlayerAndPredict pap : list) {
                            if (pos.down().getY() > pap.player.getBlockY()) continue;
                            if (lite.getValue() && liteCheck(new Vec3d(cx, cy, cz), pap.predict.getPos())) {
                                continue;
                            }
                            float damage = calculateDamage(pos, pap.player, pap.predict);
                            if (baseTempPos == null || damage > tempBaseDamage) {
                                float selfDamage = calculateDamage(pos, self.player, self.predict);
                                if (selfDamage > maxSelf.getValue()) continue;
                                if (noSuicide.getValue() > 0 && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() - noSuicide.getValue())
                                    continue;
                                if (damage < pap.targetHealth) {
                                    if (damage < pap.minBaseDamage) continue;
                                    if (smart.getValue()) {
                                        if (pap.minBaseDamage == forceMin.getValue()) {
                                            if (damage < selfDamage - 2.5) {
                                                continue;
                                            }
                                        } else {
                                            if (damage < selfDamage) {
                                                continue;
                                            }
                                        }
                                    }
                                }
                                displayTarget = pap.player;
                                baseTempPos = pos.down();
                                tempBaseDamage = damage;
                            }
                        }
                    }
                    CombatUtil.modifyPos = null;
                    if (baseTempPos != null) {
                        if (!BlockUtil.canPlace(baseTempPos, minBaseRangeVal)) {
                            baseTempPos = null;
                            tempBaseDamage = 0;
                        }
                    }
                if (doCrystal.getValue() && baseTempPos != null) {
                    doCrystal(baseTempPos);
                }
            }
        }
    }

    public boolean canPlaceCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        BlockPos boost2 = boost.up();

        return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN)
                && BlockUtil.getClickSideStrict(obsPos) != null
                && noEntityBlockCrystal(boost, ignoreCrystal, ignoreItem)
                && noEntityBlockCrystal(boost2, ignoreCrystal, ignoreItem)
                && (mc.world.isAir(boost) || hasCrystal(boost) && getBlock(boost) == Blocks.FIRE)
                && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost2));
    }
    public boolean canPlaceBase(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        BlockPos boost2 = boost.up();

        return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN || BlockUtil.canPlace(obsPos, range.getValue()))
                && BlockUtil.getClickSideStrict(obsPos) != null
                && noEntityBlockCrystal(boost, ignoreCrystal, ignoreItem)
                && noEntityBlockCrystal(boost2, ignoreCrystal, ignoreItem)
                && (mc.world.isAir(boost) || hasCrystal(boost) && getBlock(boost) == Blocks.FIRE)
                && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.world.isAir(boost2));
    }

    private boolean liteCheck(Vec3d from, Vec3d to) {
        return !canSee(from, to) && !canSee(from, to.add(0, 1.8, 0));
    }

    private boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
        Vec3d eyePos = mc.player.getEyePos();
        double wallRangeVal = wallRange.getValue();
        double wallRangeSq = wallRangeVal * wallRangeVal;
        for (Entity entity : BlockUtil.getEntities(new Box(pos))) {
            if (!entity.isAlive() || ignoreItem && entity instanceof ItemEntity || entity instanceof ArmorStandEntity && AntiCheat.INSTANCE.obsMode.getValue())
                continue;
            if (entity instanceof EndCrystalEntity) {
                if (!ignoreCrystal) return false;
                Vec3d epos = entity.getPos();
                double dx = epos.x - eyePos.x;
                double dy = epos.y - eyePos.y;
                double dz = epos.z - eyePos.z;
                if (mc.player.canSee(entity) || dx * dx + dy * dy + dz * dz <= wallRangeSq) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    private boolean behindWall(BlockPos pos) {
        Vec3d testVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 2 * 0.85, pos.getZ() + 0.5);
        HitResult result = mc.world.raycast(new RaycastContext(EntityUtil.getEyesPos(), testVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (result == null || result.getType() == HitResult.Type.MISS) return false;
        Vec3d eyePos = mc.player.getEyePos();
        double wallRangeVal = wallRange.getValue();
        double wallRangeSq = wallRangeVal * wallRangeVal;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY();
        double cz = pos.getZ() + 0.5;
        double dx = cx - eyePos.x;
        double dy = cy - eyePos.y;
        double dz = cz - eyePos.z;
        return dx * dx + dy * dy + dz * dz > wallRangeSq;
    }

    private boolean canTouch(BlockPos pos) {
        Direction side = BlockUtil.getClickSideStrict(pos);
        if (side == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        double rangeVal = range.getValue();
        double rangeSq = rangeVal * rangeVal;
        double tx = pos.getX() + 0.5 + side.getVector().getX() * 0.5;
        double ty = pos.getY() + 0.5 + side.getVector().getY() * 0.5;
        double tz = pos.getZ() + 0.5 + side.getVector().getZ() * 0.5;
        double dx = tx - eyePos.x;
        double dy = ty - eyePos.y;
        double dz = tz - eyePos.z;
        return dx * dx + dy * dy + dz * dz <= rangeSq;
    }

    private void doCrystal(BlockPos pos) {
        if (canPlaceCrystal(pos, false, false)) {
            doPlace(pos);
        } else {
            doBreak(pos);
        }
    }

    public float calculateDamage(BlockPos pos, PlayerEntity player, PlayerEntity predict) {
        return calculateDamage(new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), player, predict);
    }

    public float calculateDamage(Vec3d pos, PlayerEntity player, PlayerEntity predict) {
        if (ignoreMine.getValue() && PacketMine.getBreakPos() != null) {
            if (mc.player.getEyePos().distanceTo(PacketMine.getBreakPos().toCenterPos()) <= PacketMine.INSTANCE.range.getValue()) {
                if (PacketMine.progress >= constantProgress.getValue() / 100) {
                    CombatUtil.modifyPos = PacketMine.getBreakPos();
                    CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                }
            }
        }
        if (terrainIgnore.getValue()) {
            CombatUtil.terrainIgnore = true;
        }
        float damage = ExplosionUtil.calculateDamage(pos.getX(), pos.getY(), pos.getZ(), player, predict, 6);
        CombatUtil.modifyPos = null;
        CombatUtil.terrainIgnore = false;
        return damage;
    }

    private double getDamage(PlayerEntity target) {
        if (!PacketMine.INSTANCE.obsidian.isPressed() && slowPlace.getValue() && lastBreakTimer.passedMs((long) slowDelay.getValue()) && !PistonCrystal.INSTANCE.isOn()) {
            return slowMinDamage.getValue();
        }
        if (forcePlace.getValue() && EntityUtil.getHealth(target) <= forceMaxHealth.getValue() && !PacketMine.INSTANCE.obsidian.isPressed() && !PistonCrystal.INSTANCE.isOn()) {
            return forceMin.getValue();
        }
        if (armorBreaker.getValue()) {
            DefaultedList<ItemStack> armors = target.getInventory().armor;
            for (ItemStack armor : armors) {
                if (armor.isEmpty()) continue;
                if (EntityUtil.getDamagePercent(armor) > maxDurable.getValue()) continue;
                return armorBreakerDamage.getValue();
            }
        }
        if (PistonCrystal.INSTANCE.isOn()) {
            return autoMinDamage.getValueFloat();
        }
        return minDamage.getValue();
    }
    private double getBaseDamage(PlayerEntity target) {
        return minBaseDamage.getValue();
    }

    public boolean findCrystal() {
        if (autoSwap.getValue() == SwapMode.Off) return false;
        return getCrystal() != -1;
    }

    private final Timer syncTimer = new Timer();

    private void doBreak(BlockPos pos) {
        noPosTimer.reset();
        if (!breakSetting.getValue()) return;
        if (displayTarget != null && displayTarget.hurtTime > waitHurt.getValueInt() && !syncTimer.passed(syncTimeout.getValue())) {
            return;
        }
        lastBreakTimer.reset();
        if (!switchTimer.passedMs((long) switchCooldown.getValue())) {
            return;
        }
        syncTimer.reset();
        for (EndCrystalEntity entity : BlockUtil.getEndCrystals(new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1))) {
            if (entity.age < minAge.getValueInt()) continue;
            if (rotate.getValue() && onBreak.getValue()) {
            if (!faceVector(entity.getPos().add(0, yOffset.getValue(), 0))) return;
        }
        long delay = (long) breakDelay.getValue();
        if (smartBreak.getValue() && displayTarget != null && EntityUtil.getHealth(displayTarget) <= breakLHealth.getValue()) {
            delay = (long) breakLDelay.getValue();
        }
        if (!CombatUtil.breakTimer.passedMs(delay)) return;
        animation.to = 1;
            animation.from = 1;
            CombatUtil.breakTimer.reset();
            syncPos = pos;
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            mc.player.resetLastAttackedTicks();
            EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());
            if (breakRemove.getValue()) {
                mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
            }
            if (crystalPos != null && displayTarget != null && lastDamage >= getDamage(displayTarget) && afterBreak.getValue()) {
                if (!yawStep.getValue() || !checkFov.getValue() || Kawaii.ROTATION.inFov(entity.getPos(), fov.getValueFloat())) {
                    doPlace(crystalPos);
                }
            }
            if (forceWeb.getValue() && WebAura.INSTANCE.isOn()) {
                WebAura.force = true;
            }
            if (rotate.getValue() && !yawStep.getValue() && AntiCheat.INSTANCE.snapBack.getValue()) {
                Kawaii.ROTATION.snapBack();
            }
            return;
        }
    }

    private void doPlace(BlockPos pos) {
        noPosTimer.reset();
        if (!place.getValue()) return;
        if (!mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) && !mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL) && !findCrystal()) {
            return;
        }
        if (!canTouch(pos.down())) {
            return;
        }
        BlockPos obsPos = pos.down();
        Direction facing = BlockUtil.getClickSide(obsPos);
        Vec3d vec = obsPos.toCenterPos().add(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5);
        if (facing != Direction.UP && facing != Direction.DOWN) {
            vec = vec.add(0, 0.45, 0);
        }
        if (rotate.getValue()) {
            if (!faceVector(vec)) return;
        }
        long delay = (long) placeDelay.getValue();
        if (smartPlace.getValue() && displayTarget != null && EntityUtil.getHealth(displayTarget) <= placeLHealth.getValue()) {
            delay = (long) placeLDelay.getValue();
        }
        if (!placeTimer.passedMs(delay)) return;

        int limit = 1;
        if (sequential.getValue() == Sequential.STRICT) {
            limit = 2;
        } else if (sequential.getValue() == Sequential.STRONG) {
            limit = 10;
        }

        boolean holding = mc.player.getMainHandStack().getItem().equals(Items.END_CRYSTAL) || mc.player.getOffHandStack().getItem().equals(Items.END_CRYSTAL);

        if (holding) {
            placeTimer.reset();
            syncPos = pos;
            for (int i = 0; i < limit; i++) {
                placeCrystal(pos);
                if (sequential.getValue() == Sequential.STRONG) {
                    predictAttack(pos);
                }
            }
        } else {
            placeTimer.reset();
            syncPos = pos;
            int old = mc.player.getInventory().selectedSlot;
            int crystal = getCrystal();
            if (crystal == -1) return;
            doSwap(crystal);
            for (int i = 0; i < limit; i++) {
                placeCrystal(pos);
                if (sequential.getValue() == Sequential.STRONG) {
                    predictAttack(pos);
                }
            }
            if (autoSwap.getValue() == SwapMode.Silent) {
                doSwap(old);
            } else if (autoSwap.getValue() == SwapMode.Inventory) {
                doSwap(crystal);
                EntityUtil.syncInventory();
            }
        }
    }

    private void predictAttack(BlockPos pos) {
        int id = getMaxEntityId();
        if (id != -1) {
            EndCrystalEntity fake = new EndCrystalEntity(mc.world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            fake.setId(id + 1);
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(fake, mc.player.isSneaking()));
            EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());
        }
    }
    private void doPlaceBase(BlockPos pos) {
        if (!placeBaseTimer.passedMs((long) placeBaseDelay.getValue())) return;
        if (detectMining.getValue() && Kawaii.BREAK.isMining(pos)) return;
        int block = getBaseBlock();
        if (block == -1) return;
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side == null) return;
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
        if (!BlockUtil.canPlace(pos, range.getValue())) return;
        if (rotate.getValue()) {
            if (!faceVector(directionVec)) return;
        }
        int old = mc.player.getInventory().selectedSlot;
        doSwap(block);
        if (BlockUtil.airPlace()) {
            BlockUtil.placedPos.add(pos);
            BlockUtil.clickBlock(pos, Direction.DOWN, false, Hand.MAIN_HAND);
        } else {
            BlockUtil.placedPos.add(pos);
            BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false, Hand.MAIN_HAND);
        }
        if (autoSwap.is(SwapMode.Inventory)) {
            doSwap(block);
            EntityUtil.syncInventory();
        } else {
            doSwap(old);
        }
        if (rotate.getValue() && !yawStep.getValue() && AntiCheat.INSTANCE.snapBack.getValue()) {
            Kawaii.ROTATION.snapBack();
        }
        placeBaseTimer.reset();
    }
    private int getBaseBlock() {
        if (autoSwap.getValue() == SwapMode.Inventory) {
            return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
        } else {
            return InventoryUtil.findBlock(Blocks.OBSIDIAN);
        }
    }

    private void doSwap(int slot) {
        if (autoSwap.getValue() == SwapMode.Silent || autoSwap.getValue() == SwapMode.Normal) {
            InventoryUtil.switchToSlot(slot);
        } else if (autoSwap.getValue() == SwapMode.Inventory) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        }
    }

    private int getCrystal() {
        if (autoSwap.getValue() == SwapMode.Silent || autoSwap.getValue() == SwapMode.Normal) {
            return InventoryUtil.findItem(Items.END_CRYSTAL);
        } else if (autoSwap.getValue() == SwapMode.Inventory) {
            return InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL);
        }
        return -1;
    }

    private void placeCrystal(BlockPos pos) {
        boolean offhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
        BlockPos obsPos = pos.down();
        Direction facing = BlockUtil.getClickSide(obsPos);
        BlockUtil.clickBlock(obsPos, facing, false, offhand ? Hand.OFF_HAND : Hand.MAIN_HAND, swingMode.getValue());
    }

    private boolean faceVector(Vec3d directionVec) {
        if (!yawStep.getValue()) {
            Kawaii.ROTATION.lookAt(directionVec);
            return true;
        } else {
            this.directionVec = directionVec;
            if (Kawaii.ROTATION.inFov(directionVec, fov.getValueFloat())) {
                return true;
            }
        }
        return !checkFov.getValue();
    }


    private enum SwapMode {
        Off, Normal, Silent, SILENT_ALT, Inventory
    }
    public enum Sequential {
        NONE,
        STRICT,
        STRONG
    }

    private static final class PredictEntity extends PlayerEntity {
        private PlayerEntity source;

        private PredictEntity(ClientWorld world) {
            super(world, BlockPos.ORIGIN, 0f, new GameProfile(UUID.randomUUID(), "PredictEntity"));
        }

        private void setSource(PlayerEntity source) {
            this.source = source;
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }

        @Override
        public boolean isOnGround() {
            return source != null && source.isOnGround();
        }
    }

    private class PlayerAndPredict {
        final PlayerEntity player;
        final PlayerEntity predict;
        final float targetHealth;
        final double minTargetDamage;
        final double minBaseDamage;

        private PlayerAndPredict(PlayerEntity player) {
            this.player = player;
            this.predict = getPredict(player);
            this.targetHealth = EntityUtil.getHealth(player);
            this.minTargetDamage = getDamage(player);
            this.minBaseDamage = getBaseDamage(player);
        }
    }

    private class CrystalRender {
        @EventHandler
        public void onRender3D(Render3DEvent event) {
            BlockPos cpos = sync.getValue() && crystalPos != null ? syncPos : crystalPos;
            if (cpos != null) {
                placeVec3d = cpos.down().toCenterPos();
            }
            if (placeVec3d == null) {
                return;
            }
            if (fadeSpeed.getValue() >= 1) {
                currentFade = noPosTimer.passedMs((long) (startFadeTime.getValue() * 1000)) ? 0 : 0.5;
            } else {
                currentFade = AnimateUtil.animate(currentFade, noPosTimer.passedMs((long) (startFadeTime.getValue() * 1000)) ? 0 : 0.5, fadeSpeed.getValue() / 10);
            }
            if (currentFade == 0) {
                curVec3d = null;
                return;
            }
            if (curVec3d == null || sliderSpeed.getValue() >= 1) {
                curVec3d = placeVec3d;
            } else {
                curVec3d = new Vec3d(AnimateUtil.animate(curVec3d.x, placeVec3d.x, sliderSpeed.getValue() / 10), AnimateUtil.animate(curVec3d.y, placeVec3d.y, sliderSpeed.getValue() / 10), AnimateUtil.animate(curVec3d.z, placeVec3d.z, sliderSpeed.getValue() / 10));
            }
            if (render.getValue()) {
                Box cbox = new Box(curVec3d, curVec3d);
                if (shrink.getValue()) {
                    cbox = cbox.expand(currentFade);
                } else {
                    cbox = cbox.expand(0.5);
                }
                MatrixStack matrixStack = event.getMatrixStack();

                if (fill.booleanValue && Colors.INSTANCE.kawaiiAura.getValue()) {
                    Color fillColor = rainbow.getValue() ?
                            getColor(1) :
                            ColorUtil.injectAlpha(Colors.INSTANCE.clientColor.getValue(), (int) (fill.getValue().getAlpha() * currentFade * 2D));
                    Render3DUtil.drawFill(matrixStack, cbox, fillColor);
                } else if (fill.booleanValue){
                    Color fillColor = rainbow.getValue() ?
                            getColor(1) :
                            ColorUtil.injectAlpha(Colors.INSTANCE.clientColor.getValue(), (int) (fill.getValue().getAlpha() * currentFade * 2D));
                    Render3DUtil.drawFill(matrixStack, cbox, fillColor);
                }

                if (box.booleanValue && Colors.INSTANCE.kawaiiAura.getValue()) {
                    Color boxColor = rainbow.getValue() ?
                            getColor(1) :
                            ColorUtil.injectAlpha(Colors.INSTANCE.clientColor.getValue(), (int) (box.getValue().getAlpha() * currentFade * 2D));
                    Render3DUtil.drawBox(matrixStack, cbox, boxColor, lineWidth.getValueFloat());
                } else if (box.booleanValue) {
                    Color boxColor = rainbow.getValue() ?
                            getColor(1) :
                            ColorUtil.injectAlpha(Colors.INSTANCE.clientColor.getValue(), (int) (box.getValue().getAlpha() * currentFade * 2D));
                    Render3DUtil.drawBox(matrixStack, cbox, boxColor, lineWidth.getValueFloat());
                }
            }
            if (text.booleanValue && lastDamage > 0) {
                if (!noPosTimer.passedMs((long) (startFadeTime.getValue() * 1000))) Render3DUtil.drawText3D(df.format(lastDamage), curVec3d, text.getValue());
            }
        }
    }
}
