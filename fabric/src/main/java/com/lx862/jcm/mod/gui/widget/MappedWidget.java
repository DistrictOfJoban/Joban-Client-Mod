package com.lx862.jcm.mod.gui.widget;

import org.mtr.mapping.mapper.*;

/**
 * A wrapper around common WidgetExtensions from Minecraft Mappings
 */
public class MappedWidget {
    private final Object widget;
    public MappedWidget(Object widget) {
        this.widget = widget;
    }

    public int getX() {
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
        return 0;
    }

    public int getY() {
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
            return ((SliderWidgetExtension) widget).getHeight2();
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
    }

    public void setX(int newX) {
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
    }

    public void setY(int newY) {
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
    }
}
