package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.fundamental.ClipStack;
import org.lwjgl.glfw.GLFW;
import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public abstract class AbstractScrollViewWidget extends ClickableWidgetExtension {
    public static final int SCROLLBAR_WIDTH = 6;
    protected double currentScroll = 0;
    private boolean scrollbarDragging = false;
    public AbstractScrollViewWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public boolean mouseClicked2(double mouseX, double mouseY, int button) {
        int scrollbarEndX = getX2() + getWidth2();
        int scrollbarStartX = scrollbarEndX - getScrollbarOffset();

        if(button == 0 && mouseX >= scrollbarStartX && mouseX <= scrollbarEndX && mouseY >= getY2() && mouseY <= getY2() + getHeight2()) {
            scrollbarDragging = true;
        } else {
            scrollbarDragging = false;
        }

        return super.mouseClicked2(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased2(double mouseX, double mouseY, int button) {
        if(button == 0) {
            scrollbarDragging = false;
        }

        return super.mouseReleased2(mouseX, mouseY, button);
    }

    public boolean mouseDragged2(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (visible && isFocused2() && scrollbarDragging) {
            double remainingView = getContentHeight() - getHeight2();
            double percentage = 1 - (((getY2() + getHeight2()) - mouseY) / getHeight2());
            setScroll(remainingView * percentage);
            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
        amount *= 15;
        double oldScroll = currentScroll;
        setScroll(currentScroll - amount);
        return oldScroll != currentScroll;
    }

    @Override
    public boolean keyPressed2(int keyCode, int scanCode, int modifiers) {
        boolean bl = keyCode == GLFW.GLFW_KEY_UP;
        boolean bl2 = keyCode == GLFW.GLFW_KEY_DOWN;
        if (bl || bl2) {
            double oldScroll = currentScroll;
            if(bl)
                setScroll(currentScroll - 15);
            else
                setScroll(currentScroll + 15);
            if(oldScroll != currentScroll) return true;
        }

        return super.keyPressed2(keyCode, scanCode, modifiers);
    }

    protected boolean contentOverflowed() {
        return getContentHeight() > getHeight2();
    }

    protected int getScrollbarOffset() {
        return contentOverflowed() ? SCROLLBAR_WIDTH : 0;
    }

    public void setScroll(double scroll) {
        if(scroll < 0 || !contentOverflowed()) {
            currentScroll = 0;
        } else if(scroll > getContentHeight() - getHeight2()) {
            currentScroll = getContentHeight() - getHeight2();
        } else {
            currentScroll = scroll;
        }
    }

    private void drawScrollbar(GuiDrawing guiDrawing) {
        if(!contentOverflowed()) return;

        int fullHeight = getContentHeight();
        int visibleHeight = getHeight2();
        double scrollbarHeight =  visibleHeight * ((double)visibleHeight / fullHeight);
        double bottomOffset = currentScroll / (fullHeight - visibleHeight);
        double yOffset = bottomOffset * (visibleHeight - scrollbarHeight);
        GuiHelper.drawRectangle(guiDrawing, getX2() + getWidth2() - SCROLLBAR_WIDTH, getY2() + yOffset, SCROLLBAR_WIDTH, scrollbarHeight, 0xFFC0C0C0);

        // Border
        GuiHelper.drawRectangle(guiDrawing, getX2() + getWidth2() - 1, getY2() + yOffset, 1, scrollbarHeight, 0xFF808080);
        GuiHelper.drawRectangle(guiDrawing, getX2() + getWidth2() - SCROLLBAR_WIDTH, getY2() + yOffset + scrollbarHeight - 1, SCROLLBAR_WIDTH, 1, 0xFF808080);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        ClipStack.push(getX2(), getY2(), getWidth2(), getHeight2());

        ClipStack.push(getX2(), getY2(), getWidth2() - getScrollbarOffset(), getHeight2());
        renderContent(graphicsHolder, mouseX, mouseY, tickDelta);
        ClipStack.pop();

        drawScrollbar(guiDrawing);
        ClipStack.pop();
    }

    public abstract void renderContent(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta);
    protected abstract int getContentHeight();
}
