package com.lx862.jcm.mod.render.gui.widget;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.mapper.ClickableWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.function.Consumer;

public class BlockPosWidget extends ClickableWidgetExtension implements MultiWidgetContainer {
    private final CoordTextField posXTextField;
    private final CoordTextField posYTextField;
    private final CoordTextField posZTextField;
    public BlockPosWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.posXTextField = new CoordTextField(0, 0, width / 3, height, -29999999, 29999999, 0);
        this.posYTextField = new CoordTextField(width / 3, 0, width / 3, height, -29999999, 29999999, 0);
        this.posZTextField = new CoordTextField((width / 3) * 2, 0, width / 3, height, -29999999, 29999999, 0);

        this.posXTextField.setChangedListener2(this::setPosition);
        this.posYTextField.setChangedListener2(this::setPosition);
        this.posZTextField.setChangedListener2(this::setPosition);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        this.posXTextField.render(graphicsHolder, mouseX, mouseY, delta);
        this.posYTextField.render(graphicsHolder, mouseX, mouseY, delta);
        this.posZTextField.render(graphicsHolder, mouseX, mouseY, delta);
    }

    @Override
    public void setAllX(int newX) {
        super.setX2(newX);
        positionWidgets();
    }

    @Override
    public void setAllY(int newY) {
        super.setY2(newY);
        positionWidgets();
    }

    @Override
    public void setActiveMapped(boolean active) {
        this.posXTextField.setEditable2(active);
        this.posYTextField.setEditable2(active);
        this.posZTextField.setEditable2(active);
        this.posXTextField.setActiveMapped(active);
        this.posYTextField.setActiveMapped(active);
        this.posZTextField.setActiveMapped(active);
        super.setActiveMapped(active);
    }

    @Override
    public void setVisibleMapped(boolean visible) {
        this.posXTextField.setVisible2(visible);
        this.posYTextField.setVisible2(visible);
        this.posZTextField.setVisible2(visible);
        super.setVisibleMapped(visible);
    }

    public void positionWidgets() {
        int perWidth = getWidth2() / 3;
        this.posXTextField.setX2(getX2());
        this.posYTextField.setX2(getX2() + perWidth);
        this.posZTextField.setX2(getX2() + perWidth + perWidth);
        this.posXTextField.setY2(getY2());
        this.posYTextField.setY2(getY2());
        this.posZTextField.setY2(getY2());
        this.posXTextField.setWidth2(perWidth);
        this.posYTextField.setWidth2(perWidth);
        this.posZTextField.setWidth2(perWidth);
    }

    public void addWidget(Consumer<ClickableWidget> callback) {
        callback.accept(new ClickableWidget(posXTextField));
        callback.accept(new ClickableWidget(posYTextField));
        callback.accept(new ClickableWidget(posZTextField));
    }

    public void setBlockPos(BlockPos newPos) {
        this.posXTextField.setValue(newPos.getX());
        this.posYTextField.setValue(newPos.getY());
        this.posZTextField.setValue(newPos.getZ());
    }

    public BlockPos getBlockPos() {
        return new BlockPos(posXTextField.getNumber(), posYTextField.getNumber(), posZTextField.getNumber());
    }

    private void setPosition(String str) {
        str = str.trim();
        String[] strSplit = str.split("\\s+");

        if(!str.isEmpty() && strSplit.length > 1) {
            for(int i = 0; i < strSplit.length; i++) {
                CoordTextField field = null;
                if(i == 0) field = posXTextField;
                if(i == 1) field = posYTextField;
                if(i == 2) field = posZTextField;

                try {
                    int parsed = Integer.parseInt(strSplit[i]);
                    if(field != null) field.setValue(parsed);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }
}
