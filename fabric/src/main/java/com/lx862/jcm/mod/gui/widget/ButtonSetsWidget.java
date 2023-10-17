package com.lx862.jcm.mod.gui.widget;

import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.ClickableWidgetExtension;

import java.util.ArrayList;
import java.util.List;

public class ButtonSetsWidget extends ClickableWidgetExtension implements RenderHelper {
    public static final int WIDGET_X_MARGIN = 10;
    private final List<List<ButtonWidgetExtension>> widgetRows = new ArrayList<>();
    private final int maxWidgetHeight;
    public ButtonSetsWidget(int maxWidgetHeight) {
        super(0, 0, 0, 0);
        this.maxWidgetHeight = maxWidgetHeight;
    }

    public void setXYSize(int x, int y, int width, int height) {
        setX2(x);
        setY2(y);
        setWidth2(width);
        setHeightMapped(height);
        positionWidgets();
    }

    public void addWidget(ButtonWidgetExtension widget) {
        if(widgetRows.isEmpty()) {
            insertRow();
        }

        int lastRowIndex = widgetRows.size() - 1;
        List<ButtonWidgetExtension> lastRow = widgetRows.get(lastRowIndex);
        lastRow.add(widget);
        widgetRows.set(lastRowIndex, lastRow);
    }

    public void insertRow() {
        widgetRows.add(new ArrayList<>());
    }

    public void reset() {
        this.widgetRows.clear();
    }

    private void positionWidgets() {
        int x = getX2();
        int y = getY2();
        int width = getWidth2() + WIDGET_X_MARGIN;
        int widgetHeight = Math.min(maxWidgetHeight, (int)(maxWidgetHeight * ((double)getHeight2() / (widgetRows.size() * maxWidgetHeight))));

        for(int i = 0; i < widgetRows.size(); i++) {
            List<ButtonWidgetExtension> rowWidgets = widgetRows.get(i);

            double perWidgetWidth = ((double)width / rowWidgets.size()) - WIDGET_X_MARGIN;
            int rowY = y + (i * widgetHeight);

            for (int j = 0; j < rowWidgets.size(); j++) {
                ButtonWidgetExtension widget = rowWidgets.get(j);
                double widgetStartX = x + (j * perWidgetWidth) + (j * WIDGET_X_MARGIN);

                widget.setX((int)Math.round(widgetStartX));
                widget.setY(rowY);
                widget.setWidth((int)Math.round(perWidgetWidth));
                // TODO: How
                //widget.setHeight(widgetHeight);
            }
        }
    }
}
