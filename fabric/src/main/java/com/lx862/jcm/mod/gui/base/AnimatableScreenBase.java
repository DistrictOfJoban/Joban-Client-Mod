package com.lx862.jcm.mod.gui.base;

import net.minecraft.SharedConstants;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class AnimatableScreenBase extends ScreenBase {
    protected double linearAnimationProgress = 0;
    protected double animationProgress;
    protected boolean closing = false;
    private final boolean shouldAnimate;
    public AnimatableScreenBase(boolean animatable) {
        super();
        this.shouldAnimate = animatable;
        this.animationProgress = animatable ? 0 : 1;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
        double frameDelta = (tickDelta / SharedConstants.TICKS_PER_SECOND);
        if(!shouldAnimate) {
            linearAnimationProgress = 1;
        } else {
            linearAnimationProgress = closing ? Math.max(0, linearAnimationProgress - frameDelta) : Math.min(1, linearAnimationProgress + frameDelta);

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
        return 1 - Math.pow(1 - x, 5);
    }
}
