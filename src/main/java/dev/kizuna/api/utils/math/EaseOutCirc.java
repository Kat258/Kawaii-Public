package dev.kizuna.api.utils.math;

import dev.kizuna.api.utils.Wrapper;
import net.minecraft.util.math.MathHelper;

public class EaseOutCirc
implements Wrapper {
    private final int maxTicks;
    private double value;
    private double dstValue;
    private int prevStep;
    private int step;

    public EaseOutCirc(int maxTicks) {
        this.maxTicks = maxTicks;
    }

    public EaseOutCirc() {
        this(5);
    }

    public void update() {
        this.prevStep = this.step;
        this.step = MathHelper.clamp((int)(this.step + 1), (int)0, (int)this.maxTicks);
    }

    public static double createAnimation(double value) {
        return Math.sqrt(1.0 - Math.pow(value - 1.0, 2.0));
    }

    public void setValue(double value) {
        if (value != this.dstValue) {
            this.prevStep = 0;
            this.step = 0;
            this.value = this.dstValue;
            this.dstValue = value;
        }
    }

    public double getAnimationD() {
        double delta = this.dstValue - this.value;
        double animation = EaseOutCirc.createAnimation((double)((float)this.prevStep + (float)(this.step - this.prevStep) * mc.getRenderTickCounter().getTickDelta(true)) / (double)this.maxTicks);
        return this.value + delta * animation;
    }
}
