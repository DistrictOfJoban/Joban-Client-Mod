package com.lx862.jcm.mod.gui;

import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ScreenExtension;

public abstract class AnimatableScreenBase extends ScreenExtension {
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
            animationProgress = 1;
        } else {
            animationProgress = closing ? Math.max(0, animationProgress - (delta / 15)) : Math.min(1, animationProgress + (delta / 15));

            if(animationProgress <= 0 && closing) {
                onClose2();
            }
        }
    }

    @Override
    public void onClose2() {
        if(closing || !shouldAnimate) {
            super.onClose2();
        } else {
            closing = true;
        }
    }

    protected double getEaseAnimation() {
        double x = animationProgress;
        return 1 - Math.pow(1 - x, 4);
    }
}
