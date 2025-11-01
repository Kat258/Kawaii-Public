package dev.kizuna.api.utils.render.animation;

import dev.kizuna.api.utils.math.Easing;
import dev.kizuna.api.utils.math.FadeUtils;
import dev.kizuna.mod.modules.Module;

public class NotificationUtil {
    public final FadeUtils enterFade = new FadeUtils(800L);
    public final FadeUtils fillFade = new FadeUtils(1200L);
    public final FadeUtils exitFade = new FadeUtils(300L);
    public final FadeUtils moveFade = new FadeUtils(500L);

    public final String message;
    public final Module.Category moduleCategory;

    private AnimationState state = AnimationState.ENTERING;
    private boolean initialized = false;
    private double currentY = 0;
    private double targetY = 0;

    public NotificationUtil(String message, Module.Category category) {
        this.message = message;
        this.moduleCategory = category;
        resetAllFades();
    }

    private void resetAllFades() {
        this.enterFade.reset();
        this.fillFade.reset();
        this.exitFade.reset();
        this.moveFade.reset();
        this.state = AnimationState.ENTERING;
        this.initialized = true;
    }

    public void update() {
        if (!initialized) return;

        switch (state) {
            case ENTERING:
                if (enterFade.isEnd()) {
                    state = AnimationState.FILLING;
                    fillFade.reset();
                }
                break;
            case FILLING:
                if (fillFade.isEnd()) {
                    state = AnimationState.EXITING;
                    exitFade.reset();
                }
                break;
            case EXITING:
                break;
        }
    }

    public void setTargetPosition(double targetY) {
        if (Math.abs(this.targetY - targetY) > 0.1) {
            this.targetY = targetY;
            if (state != AnimationState.ENTERING && state != AnimationState.EXITING) {
                moveFade.reset();
            }
        }
    }

    public boolean shouldRemove() {
        return state == AnimationState.EXITING && exitFade.isEnd();
    }

    public double getOffsetX(int width, int screenWidth) {
        return switch (state) {
            case ENTERING -> {
                double enterProgress = enterFade.ease(Easing.CircOut);
                yield (screenWidth - width - 25) + width * (1 - enterProgress);
            }
            case EXITING -> {
                double exitProgress = exitFade.ease(Easing.CircIn);
                yield (screenWidth - width - 25) + width * exitProgress;
            }
            default -> screenWidth - width - 25;
        };
    }

    public double getOffsetY(int height, int screenHeight, int currentIndex, int totalActiveNotifications) {
        // 从HUD模块获取间隔设置
        float spacing = dev.kizuna.mod.modules.impl.client.HUD.INSTANCE != null ? 
                       dev.kizuna.mod.modules.impl.client.HUD.INSTANCE.notificationSpacing.getValueInt() : 25;
        double newTargetY = screenHeight - height - (currentIndex * spacing) - 10 - 35;
        setTargetPosition(newTargetY);
        switch (state) {
            case ENTERING:
                double enterProgress = enterFade.ease(Easing.CircOut);
                currentY = screenHeight + (newTargetY - screenHeight) * enterProgress;
                break;
            case EXITING:
                double exitProgress = exitFade.ease(Easing.CircIn);
                currentY = newTargetY + (screenHeight - newTargetY) * exitProgress;
                break;
            case FILLING:
            default:
                if (moveFade.isEnd()) {
                    currentY = targetY;
                } else {
                    double moveProgress = moveFade.ease(Easing.SineInOut);
                    currentY = currentY + (targetY - currentY) * moveProgress;
                }
                break;
        }

        return currentY;
    }

    public float getBackgroundProgress() {
        return switch (state) {
            case FILLING -> (float) fillFade.ease(Easing.SineInOut);
            case EXITING -> 1.0f;
            default -> 0f;
        };
    }

    public float getAlpha() {
        return switch (state) {
            case ENTERING -> (float) enterFade.ease(Easing.SineInOut);
            case FILLING -> 1.0f;
            case EXITING -> (float) (1.0 - exitFade.ease(Easing.CircIn));
        };
    }

    public boolean isActive() {
        return initialized && !shouldRemove();
    }

    public float getCurrentAlpha() {
        return getAlpha();
    }

    private enum AnimationState {
        ENTERING,
        FILLING,
        EXITING
    }
}