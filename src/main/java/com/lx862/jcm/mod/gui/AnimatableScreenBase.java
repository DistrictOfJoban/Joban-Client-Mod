package com.lx862.jcm.mod.gui;

import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ScreenExtension;

public abstract class AnimatableScreenBase extends ScreenExtension {
    protected double linearAnimationProgress = 0;
    protected double animationProgress = 0;
    private boolean closing = false;
    private final boolean shouldAnimate;
    public AnimatableScreenBase(boolean animatable) {
        super();
        this.shouldAnimate = animatable;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.render(graphicsHolder, mouseX, mouseY, delta);
        if(!shouldAnimate) {
            linearAnimationProgress = 1;
        } else {
            linearAnimationProgress = closing ? Math.max(0, linearAnimationProgress - (delta / 15)) : Math.min(1, linearAnimationProgress + (delta / 15));

            if(linearAnimationProgress <= 0 && closing) {
                onClose2();
            }
        }
        animationProgress = getEaseAnimation();
    }

    @Override
    public void onClose2() {
        if(closing || !shouldAnimate) {
            super.onClose2();
        } else {
            closing = true;
        }
    }

    private double getEaseAnimation() {
        double x = linearAnimationProgress;
        return 1 - Math.pow(1 - x, 4);
    }
}
