package com.lx862.jcm.mod.render.gui.widget;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a set of widget within a defined area.
 * Able to tile the widgets horizontally.<br>
 * Unlike {@link WidgetSet}, this widget does not automatically distribute the widget evenly and you can control the width of each element.
 */
public class HorizontalWidgetSet extends ClickableWidgetExtension implements WidgetsWrapper, RenderHelper {
    public int widgetXMargin;
    private final List<MappedWidget> widgets = new ArrayList<>();

    public HorizontalWidgetSet(int widgetXMargin) {
        super(0, 0, 0, 20);
        this.widgetXMargin = widgetXMargin;
    }
    public HorizontalWidgetSet() {
        this(5);
    }

    /**
     * Set the X, Y, Width and Height of the widget. This is required to be called as the default width is 0
     */
    public void setXYSize(int x, int y, int width, int height) {
        setX2(x);
        setY2(y);
        setWidth2(width);
        setHeightMapped(height);
        positionWidgets();
    }

    public void addWidget(MappedWidget widget) {
        widgets.add(widget);
    }

    @Override
    public int getWidth2() {
        int total = 0;
        for(MappedWidget mappedWidget : widgets) {
            total += mappedWidget.getWidth();
            total += widgetXMargin;
        }
        if(total > 1) {
            total -= widgetXMargin;
        }
        return total;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        for(MappedWidget mappedWidget : widgets) {
            mappedWidget.render(graphicsHolder, mouseX, mouseY, delta);
        }
    }

    @Override
    public void setVisibleMapped(boolean visible) {
        for(MappedWidget widget : widgets) {
            widget.setVisible(visible);
        }
        super.setVisibleMapped(visible);
    }

    public void setAllX(int newX) {
        super.setX2(newX);
        positionWidgets();
    }

    public void setAllY(int newY) {
        super.setY2(newY);
        positionWidgets();
    }

    private void positionWidgets() {
        int accX = 0;

        for (MappedWidget widget : widgets) {
            widget.setX(getX2() + accX);
            widget.setY(getY2());
            int clampedWidth = Math.min(width - accX, widget.getWidth());
            widget.setWidth(clampedWidth);
            accX += clampedWidth + widgetXMargin;
        }
    }
}
