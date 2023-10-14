package com.lx862.jcm.mod.gui.widget;

import com.lx862.jcm.mod.render.Renderable;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.ClickableWidgetExtension;

import java.util.ArrayList;
import java.util.List;

public class ButtonSetsWidget extends ClickableWidgetExtension implements Renderable {
    public static final int WIDGET_X_PADDING = 10;
    private final List<List<ButtonWidgetExtension>> widgetRows = new ArrayList<>();
    public ButtonSetsWidget() {
        super(0, 0, 0, 0);
    }

    public void setXYSize(int x, int y, int width, int height) {
        setX2(x);
        setY2(y);
        setWidth2(width);
        setHeight2(height);
        positionWidgets();
    }

    public void add(ButtonWidgetExtension widget) {
        if(widgetRows.isEmpty()) {
            widgetRows.add(new ArrayList<>());
        }

        int lastRowIndex = widgetRows.size() - 1;
        List<ButtonWidgetExtension> lastRow = widgetRows.get(lastRowIndex);
        lastRow.add(widget);
        widgetRows.set(lastRowIndex, lastRow);
    }

    public void addRow() {
        widgetRows.add(new ArrayList<>());
    }

    public void reset() {
        this.widgetRows.clear();
    }

    private void positionWidgets() {
        int x = getX2();
        int y = getY2();
        int width = getWidth2() + WIDGET_X_PADDING;
        int widgetHeight = Math.min(20, (int)(20 * ((double)getHeight2() / (widgetRows.size() * 20))));

        for(int i = 0; i < widgetRows.size(); i++) {
            List<ButtonWidgetExtension> rowWidgets = widgetRows.get(i);

            double perWidgetWidth = ((double)width / rowWidgets.size()) - WIDGET_X_PADDING;
            int rowY = y + (i * widgetHeight);

            for (int j = 0; j < rowWidgets.size(); j++) {
                ButtonWidgetExtension widget = rowWidgets.get(j);
                double widgetStartX = x + (j * perWidgetWidth) + (j * WIDGET_X_PADDING);

                widget.setX2((int)Math.round(widgetStartX));
                widget.setY2(rowY);
                widget.setWidth2((int)Math.round(perWidgetWidth));
                widget.setHeight2(widgetHeight);
            }
        }
    }
}
