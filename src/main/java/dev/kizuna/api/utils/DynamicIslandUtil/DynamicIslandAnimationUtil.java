package dev.kizuna.api.utils.DynamicIslandUtil;

import java.awt.Color;
import dev.kizuna.api.utils.math.Easing;
import dev.kizuna.mod.modules.impl.client.ClickGui;

public class DynamicIslandAnimationUtil {
    private boolean setup = false;
    public double from = 0.0;
    public double to = 0.0;
    private long lastDeltaTime = System.currentTimeMillis();

    private double smooth(double current, double target, double speed) {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - this.lastDeltaTime;
        this.lastDeltaTime = currentTime;
        speed = Math.abs(target - current) * speed;
        if (deltaTime < 1L) {
            deltaTime = 1L;
        }
        double difference = current - target;
        double smoothing = Math.max(speed * ((double)deltaTime / 16.0), 0.15);
        current = difference > speed ? Math.max(current - smoothing, target) : (difference < -speed ? Math.min(current + smoothing, target) : target);
        return current;
    }

    public static double getNotificationProgress(long startTime) {
        return Math.min((float)(System.currentTimeMillis() - startTime) / 500.0f, 1.0f);
    }

    public static double getEasedNotificationProgress(long startTime) {
        return Easing.IOS.ease(DynamicIslandAnimationUtil.getNotificationProgress(startTime));
    }

    public static Color getSwitchColor(long startTime, boolean isEnable) {
        double easedProgress = Easing.IOS.ease(Math.min((float)(System.currentTimeMillis() - startTime) / 300.0f, 1.0f));
        if (isEnable) {
            return new Color((int)(0.0 + 68.0 * easedProgress), (int)(0.0 + 248.0 * easedProgress), (int)(0.0 + 68.0 * easedProgress), (int)(100.0 + 102.0 * easedProgress));
        }
        return new Color((int)(68.0 + -68.0 * easedProgress), (int)(248.0 + -248.0 * easedProgress), (int)(68.0 + -68.0 * easedProgress), (int)(202.0 + -102.0 * easedProgress));
    }

    public static float getSwitchPosition(long startTime, boolean isEnable, float startX, float endX) {
        double easedProgress = DynamicIslandAnimationUtil.getEasedNotificationProgress(startTime);
        if (isEnable) {
            return startX + (float)((double)(endX - startX) * easedProgress);
        }
        return endX - (float)((double)(endX - startX) * easedProgress);
    }

    public double get(double target) {
        long length = ClickGui.INSTANCE.animationTime.getValueInt();
        if (length == 0L) {
            return target;
        }
        double speedFactor = 300.0 / (double)Math.max(length, 1L);
        double animationSpeed = 0.1 * speedFactor;
        if (!this.setup) {
            this.setup = true;
            this.from = target;
            this.to = target;
            return target;
        }
        if (target != this.to) {
            this.to = target;
        }
        this.from = this.smooth(this.from, this.to, animationSpeed);
        return this.from;
    }

    public double get(double target, long length, Easing ease) {
        if (length == 0L) {
            return target;
        }
        double speedFactor = 300.0 / (double)Math.max(length, 1L);
        double animationSpeed = 0.1 * speedFactor;
        if (!this.setup) {
            this.setup = true;
            this.from = target;
            this.to = target;
            return target;
        }
        if (target != this.to) {
            this.to = target;
        }
        this.from = this.smooth(this.from, this.to, animationSpeed);
        return this.from;
    }

    public void reset() {
        this.setup = false;
        this.from = 0.0;
        this.to = 0.0;
        this.lastDeltaTime = System.currentTimeMillis();
    }

    public void setCurrent(double value) {
        this.from = value;
        this.to = value;
        this.setup = true;
        this.lastDeltaTime = System.currentTimeMillis();
    }

    public static float getNotificationAlphaProgress(long notificationStartTime) {
        long elapsed = System.currentTimeMillis() - notificationStartTime;
        if (elapsed < 300L) {
            return (float)Easing.CubicOut.ease((float)elapsed / 300.0f);
        }
        if (elapsed > 1700L) {
            return (float)(1.0 - Easing.CubicIn.ease((float)(elapsed - 1700L) / 300.0f));
        }
        return 1.0f;
    }
}
