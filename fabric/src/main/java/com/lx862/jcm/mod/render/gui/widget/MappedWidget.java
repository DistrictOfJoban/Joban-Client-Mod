package com.lx862.jcm.mod.render.gui.widget;

import org.mtr.mapping.mapper.*;

/**
 * A wrapper around common WidgetExtensions from Minecraft Mappings
 */
public class MappedWidget {
    private final Object widget;
    public MappedWidget(Object widget) {
        if(!(widget instanceof MappedWidget || widget instanceof ButtonWidgetExtension || widget instanceof TextFieldWidgetExtension ||
                widget instanceof CheckboxWidgetExtension || widget instanceof SliderWidgetExtension || widget instanceof ClickableWidgetExtension)) {
            throw new IllegalArgumentException("Widget is not an instance of ButtonWidgetExtension, TextFieldWidgetExtension, CheckboxWidgetExtension or SliderWidgetExtension!");
        }

        this.widget = widget;
    }

    public int getX() {
        if(widget instanceof HorizontalWidgetSet) {
            return ((HorizontalWidgetSet) widget).getX2();
        }
        if(widget instanceof ButtonWidgetExtension) {
            return ((ButtonWidgetExtension) widget).getX2();
        }
        if(widget instanceof TextFieldWidgetExtension) {
            return ((TextFieldWidgetExtension) widget).getX2();
        }
        if(widget instanceof CheckboxWidgetExtension) {
            return ((CheckboxWidgetExtension) widget).getX2();
        }
        if(widget instanceof SliderWidgetExtension) {
            return ((SliderWidgetExtension) widget).getX2();
        }
        if(widget instanceof ClickableWidgetExtension) {
            return ((ClickableWidgetExtension) widget).getX2();
        }
        if(widget instanceof MappedWidget) {
            return ((MappedWidget) widget).getX();
        }
        return 0;
    }

    public int getY() {
        if(widget instanceof HorizontalWidgetSet) {
            return ((HorizontalWidgetSet) widget).getY2();
        }
        if(widget instanceof ButtonWidgetExtension) {
            return ((ButtonWidgetExtension) widget).getY2();
        }
        if(widget instanceof TextFieldWidgetExtension) {
            return ((TextFieldWidgetExtension) widget).getY2();
        }
        if(widget instanceof CheckboxWidgetExtension) {
            return ((CheckboxWidgetExtension) widget).getY2();
        }
        if(widget instanceof SliderWidgetExtension) {
            return ((SliderWidgetExtension) widget).getY2();
        }
        if(widget instanceof ClickableWidgetExtension) {
            return ((ClickableWidgetExtension) widget).getY2();
        }
        if(widget instanceof MappedWidget) {
            return ((MappedWidget) widget).getY();
        }
        return 0;
    }

    public int getWidth() {
        if(widget instanceof ButtonWidgetExtension) {
            return ((ButtonWidgetExtension) widget).getWidth2();
        }
        if(widget instanceof TextFieldWidgetExtension) {
            return ((TextFieldWidgetExtension) widget).getWidth2();
        }
        if(widget instanceof CheckboxWidgetExtension) {
            return ((CheckboxWidgetExtension) widget).getWidth2();
        }
        if(widget instanceof SliderWidgetExtension) {
            return ((SliderWidgetExtension) widget).getWidth2();
        }
        if(widget instanceof ClickableWidgetExtension) {
            return ((ClickableWidgetExtension) widget).getWidth2();
        }
        if(widget instanceof MappedWidget) {
            return ((MappedWidget) widget).getWidth();
        }
        return 0;
    }

    public int getHeight() {
        if(widget instanceof ButtonWidgetExtension) {
            return ((ButtonWidgetExtension) widget).getHeight2();
        }
        if(widget instanceof TextFieldWidgetExtension) {
            return ((TextFieldWidgetExtension) widget).getHeight2();
        }
        if(widget instanceof CheckboxWidgetExtension) {
            return ((CheckboxWidgetExtension) widget).getHeight2();
        }
        if(widget instanceof SliderWidgetExtension) {
            return ((SliderWidgetExtension) widget).getHeight2();
        }
        if(widget instanceof ClickableWidgetExtension) {
            return ((ClickableWidgetExtension) widget).getHeight2();
        }
        if(widget instanceof MappedWidget) {
            return ((MappedWidget) widget).getHeight();
        }
        return 0;
    }

    public void setWidth(int width) {
        if(widget instanceof ButtonWidgetExtension) {
            ((ButtonWidgetExtension) widget).setWidth2(width);
        }
        if(widget instanceof TextFieldWidgetExtension) {
            ((TextFieldWidgetExtension) widget).setWidth2(width);
        }
        if(widget instanceof CheckboxWidgetExtension) {
            ((CheckboxWidgetExtension) widget).setWidth2(width);
        }
        if(widget instanceof SliderWidgetExtension) {
            ((SliderWidgetExtension) widget).setWidth2(width);
        }
        if(widget instanceof ClickableWidgetExtension) {
            ((ClickableWidgetExtension) widget).setWidth2(width);
        }
        if(widget instanceof MappedWidget) {
            ((MappedWidget) widget).setWidth(width);
        }
    }

    public void setX(int newX) {
        if(widget instanceof MultiWidgetContainer) {
            ((MultiWidgetContainer) widget).setAllX(newX);
        }
        if(widget instanceof ButtonWidgetExtension) {
            ((ButtonWidgetExtension) widget).setX2(newX);
        }
        if(widget instanceof TextFieldWidgetExtension) {
            ((TextFieldWidgetExtension) widget).setX2(newX);
        }
        if(widget instanceof CheckboxWidgetExtension) {
            ((CheckboxWidgetExtension) widget).setX2(newX);
        }
        if(widget instanceof SliderWidgetExtension) {
            ((SliderWidgetExtension) widget).setX2(newX);
        }
        if(widget instanceof ClickableWidgetExtension) {
            ((ClickableWidgetExtension) widget).setX2(newX);
        }
        if(widget instanceof MappedWidget) {
            ((MappedWidget) widget).setX(newX);
        }
    }

    public void setY(int newY) {
        if(widget instanceof MultiWidgetContainer) {
            ((MultiWidgetContainer) widget).setAllY(newY);
        }
        if(widget instanceof ButtonWidgetExtension) {
            ((ButtonWidgetExtension) widget).setY2(newY);
        }
        if(widget instanceof TextFieldWidgetExtension) {
            ((TextFieldWidgetExtension) widget).setY2(newY);
        }
        if(widget instanceof CheckboxWidgetExtension) {
            ((CheckboxWidgetExtension) widget).setY2(newY);
        }
        if(widget instanceof SliderWidgetExtension) {
            ((SliderWidgetExtension) widget).setY2(newY);
        }
        if(widget instanceof ClickableWidgetExtension) {
            ((ClickableWidgetExtension) widget).setY2(newY);
        }
        if(widget instanceof MappedWidget) {
            ((MappedWidget) widget).setY(newY);
        }
    }

    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        if(widget instanceof ButtonWidgetExtension) {
            ((ButtonWidgetExtension) widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        }
        if(widget instanceof TextFieldWidgetExtension) {
            ((TextFieldWidgetExtension) widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        }
        if(widget instanceof CheckboxWidgetExtension) {
            ((CheckboxWidgetExtension) widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        }
        if(widget instanceof SliderWidgetExtension) {
            ((SliderWidgetExtension) widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        }
        if(widget instanceof ClickableWidgetExtension) {
            ((ClickableWidgetExtension) widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        }
        if(widget instanceof MappedWidget) {
            ((MappedWidget) widget).render(graphicsHolder, mouseX, mouseY, tickDelta);
        }
    }

    public void setVisible(boolean value) {
        if(widget instanceof ButtonWidgetExtension) {
            ((ButtonWidgetExtension) widget).setVisibleMapped(value);
        }
        if(widget instanceof TextFieldWidgetExtension) {
            ((TextFieldWidgetExtension) widget).setVisibleMapped(value);
        }
        if(widget instanceof CheckboxWidgetExtension) {
            ((CheckboxWidgetExtension) widget).setVisibleMapped(value);
        }
        if(widget instanceof SliderWidgetExtension) {
            ((SliderWidgetExtension) widget).setVisibleMapped(value);
        }
        if(widget instanceof ClickableWidgetExtension) {
            ((ClickableWidgetExtension) widget).setVisibleMapped(value);
        }
        if(widget instanceof MappedWidget) {
            ((MappedWidget) widget).setVisible(value);
        }
    }

    public void setAlpha(float alpha) {
        if(widget instanceof ButtonWidgetExtension) {
            ((ButtonWidgetExtension) widget).setAlpha2(alpha);
        }
        if(widget instanceof TextFieldWidgetExtension) {
            ((TextFieldWidgetExtension) widget).setAlpha2(alpha);
        }
        if(widget instanceof CheckboxWidgetExtension) {
            ((CheckboxWidgetExtension) widget).setAlpha2(alpha);
        }
        if(widget instanceof SliderWidgetExtension) {
            ((SliderWidgetExtension) widget).setAlpha2(alpha);
        }
        if(widget instanceof ClickableWidgetExtension) {
            ((ClickableWidgetExtension) widget).setAlpha2(alpha);
        }
        if(widget instanceof MappedWidget) {
            ((MappedWidget) widget).setAlpha(alpha);
        }
    }
}
