package dev.kizuna.api.utils.render.animation;

import dev.kizuna.api.utils.math.FadeUtils;

public class DynamicIslandAnimation {
    public final FadeUtils enterFade = new FadeUtils(650L);
    public final FadeUtils exitFade = new FadeUtils(500L);
    public final FadeUtils expandFade = new FadeUtils(750L);

    private boolean hasTarget = false;
    private boolean isAnimating = false;
    private boolean showingTarget = false;
    private boolean isExpanding = false;

    public void setTarget(boolean hasTarget) {
        if (this.hasTarget != hasTarget) {
            this.hasTarget = hasTarget;
            this.isAnimating = true;
            if (hasTarget) {
                enterFade.reset();
                expandFade.reset();
                showingTarget = true;
                isExpanding = true;
            } else {
                exitFade.reset();
            }
        }
    }

    public void update() {
        if (isAnimating) {
            if (hasTarget) {
                if (enterFade.isEnd() && expandFade.isEnd()) {
                    isAnimating = false;
                    isExpanding = false;
                }
            } else {
                if (exitFade.isEnd()) {
                    showingTarget = false;
                    isAnimating = false;
                }
            }
        }
    }

    public float getAnimationProgress() {
        if (!isAnimating) {
            return hasTarget ? 1.0f : 0.0f;
        }

        if (hasTarget) {
            return (float) enterFade.getFadeOne();
        } else {
            return (float) exitFade.getFadeOne();
        }
    }

    public float getHealthBarProgress() {
        float progress = getAnimationProgress();
        if (hasTarget) {
            return progress < 0.3f ? 0.0f : (progress - 0.3f) / 0.7f;
        } else {
            return 1.0f - progress;
        }
    }

    public double getWidthAnimation(int defaultWidth, int targetWidth) {
        if (!isAnimating) {
            return showingTarget ? targetWidth : defaultWidth;
        }

        if (hasTarget) {
            double progress = enterFade.ease(FadeUtils.Ease.CUBIC_OUT);
            double width = defaultWidth + (targetWidth - defaultWidth) * progress;
            if (progress > 0.9) {
                double overshoot = (progress - 0.9) / 0.1;
                width += Math.sin(overshoot * Math.PI) * 2;
            }
            return width;
        } else {
            double progress = exitFade.ease(FadeUtils.Ease.CUBIC_IN);
            return targetWidth + (defaultWidth - targetWidth) * progress;
        }
    }

    public double getHeightAnimation(int defaultHeight, int targetHeight) {
        if (!isAnimating) {
            return showingTarget ? targetHeight : defaultHeight;
        }

        if (hasTarget) {
            double progress = enterFade.ease(FadeUtils.Ease.CUBIC_OUT);
            double height = defaultHeight + (targetHeight - defaultHeight) * progress;
            if (progress > 0.8) {
                double overshoot = (progress - 0.8) / 0.2;
                height += Math.sin(overshoot * Math.PI) * 1.5;
            }
            return height;
        } else {
            double progress = exitFade.ease(FadeUtils.Ease.CUBIC_IN);
            return targetHeight + (defaultHeight - targetHeight) * progress;
        }
    }

    public int getXAnimation(int screenWidth, int currentWidth) {
        return (screenWidth - currentWidth) / 2;
    }

    public float getAlpha() {
        if (!isAnimating) {
            return 1.0f;
        }

        if (hasTarget) {
            return (float) enterFade.ease(FadeUtils.Ease.OutQuint);
        } else {
            return (float) (1.0 - exitFade.ease(FadeUtils.Ease.OutQuint));
        }
    }

    public float getExpandProgress() {
        if (!isExpanding) return 1.0f;
        return (float) expandFade.ease(FadeUtils.Ease.CUBIC_OUT);
    }

    public boolean shouldShow() {
        return showingTarget || (isAnimating && hasTarget);
    }

    public float easeOutCubic(float x) {
        return (float) (1 - Math.pow(1 - x, 3));
    }

    public void reset() {
        hasTarget = false;
        isAnimating = false;
        showingTarget = false;
        isExpanding = false;
        enterFade.reset();
        exitFade.reset();
        expandFade.reset();
    }
}