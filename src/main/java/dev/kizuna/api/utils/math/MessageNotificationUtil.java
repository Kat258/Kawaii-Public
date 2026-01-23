package dev.kizuna.api.utils.math;

import dev.kizuna.api.utils.math.Easing;
import dev.kizuna.api.utils.math.FadeUtils;

public class MessageNotificationUtil {
    public final FadeUtils enterFade = new FadeUtils(800L);
    public final FadeUtils waitFade = new FadeUtils(1200L);
    public final FadeUtils exitFade = new FadeUtils(300L);
    public final FadeUtils moveFade = new FadeUtils(500L);
    public final String message;
    private AnimationState state = AnimationState.ENTERING;
    private boolean initialized = false;
    private double currentY = 0.0;
    private double targetY = 0.0;

    public MessageNotificationUtil(String message) {
        this.message = message;
        this.resetAllFades();
    }

    private void resetAllFades() {
        this.enterFade.reset();
        this.waitFade.reset();
        this.exitFade.reset();
        this.moveFade.reset();
        this.state = AnimationState.ENTERING;
        this.initialized = true;
    }

    public void update() {
        if (!this.initialized) {
            return;
        }
        switch (this.state.ordinal()) {
            case 0: {
                if (!this.enterFade.isEnd()) break;
                this.state = AnimationState.WAIT;
                this.waitFade.reset();
                break;
            }
            case 1: {
                if (!this.waitFade.isEnd()) break;
                this.state = AnimationState.EXITING;
                this.exitFade.reset();
                break;
            }
        }
    }

    public void setTargetPosition(double targetY) {
        if (Math.abs(this.targetY - targetY) > 0.1) {
            this.targetY = targetY;
            if (this.state != AnimationState.ENTERING && this.state != AnimationState.EXITING) {
                this.moveFade.reset();
            }
        }
    }

    public boolean shouldRemove() {
        return this.state == AnimationState.EXITING && this.exitFade.isEnd();
    }

    public double getOffsetX(int width, int screenWidth) {
        return switch (this.state.ordinal()) {
            case 0 -> {
                double enterProgress = this.enterFade.ease(Easing.CircOut);
                yield (double)(screenWidth - width - 25) + (double)width * (1.0 - enterProgress);
            }
            case 2 -> {
                double exitProgress = this.exitFade.ease(Easing.CircIn);
                yield (double)(screenWidth - width - 25) + (double)width * exitProgress;
            }
            default -> screenWidth - width - 25;
        };
    }

    public double getOffsetY(int height, int screenHeight, int currentIndex) {
        float spacing = 25.0f;
        double newTargetY = (float)(screenHeight - height) - (float)currentIndex * spacing - 10.0f - 35.0f;
        this.setTargetPosition(newTargetY);
        switch (this.state.ordinal()) {
            case 0: {
                double enterProgress = this.enterFade.ease(Easing.CircOut);
                this.currentY = (double)screenHeight + (newTargetY - (double)screenHeight) * enterProgress;
                break;
            }
            case 2: {
                double exitProgress = this.exitFade.ease(Easing.CircIn);
                this.currentY = newTargetY + ((double)screenHeight - newTargetY) * exitProgress;
                break;
            }
            default: {
                if (this.moveFade.isEnd()) {
                    this.currentY = this.targetY;
                    break;
                }
                double moveProgress = this.moveFade.ease(Easing.SineInOut);
                this.currentY += (this.targetY - this.currentY) * moveProgress;
            }
        }
        return this.currentY;
    }

    public float getAlpha() {
        return switch (this.state.ordinal()) {
            default -> throw new IncompatibleClassChangeError();
            case 0 -> (float)this.enterFade.ease(Easing.SineInOut);
            case 1 -> 1.0f;
            case 2 -> (float)(1.0 - this.exitFade.ease(Easing.CircIn));
        };
    }

    public boolean isActive() {
        return this.initialized && !this.shouldRemove();
    }

    private static enum AnimationState {
        ENTERING,
        WAIT,
        EXITING;

    }
}
