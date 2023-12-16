package com.lx862.jcm.mod.render.screen.widget;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a set of widget within a defined area.
 * Able to tile the widgets horizontally.
 */
public class HorizontalWidgetSet extends ClickableWidgetExtension implements RenderHelper {
    public int widgetXMargin;
    private final List<MappedWidget> widgets = new ArrayList<>();

    public HorizontalWidgetSet(int widgetXMargin) {
        super(0, 0, 0, 20);
        this.widgetXMargin = widgetXMargin;
    }
    public HorizontalWidgetSet() {
        this(5);
    }

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

    public void reset() {
        this.widgets.clear();
    }

    @Override
    public int getWidth2() {
        int total = 0;
        for(MappedWidget mappedWidget : widgets) {
            total += mappedWidget.getWidth();
        }
        return total;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        for(MappedWidget mappedWidget : widgets) {
            mappedWidget.render(graphicsHolder, mouseX, mouseY, delta);
        }
    }

    public void setAllX(int newX) {
        int incX = 0;
        for(MappedWidget mappedWidget : widgets) {
            mappedWidget.setX(newX + incX);
            incX += mappedWidget.getWidth() + widgetXMargin;
        }
        super.setX2(newX);
    }

    public void setAllY(int newY) {
        for(MappedWidget mappedWidget : widgets) {
            mappedWidget.setY(newY);
        }
        super.setY2(newY);
    }

    private void positionWidgets() {
        int accX = 0;

        for (MappedWidget widget : widgets) {
            widget.setX(getX2() + accX);
            widget.setY(getY2());
            accX += widget.getWidth() + widgetXMargin;
        }
    }
}
