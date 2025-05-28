package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.block.OperatorButtonBlock;
import com.lx862.jcm.mod.network.block.OperatorButtonUpdatePacket;
import com.lx862.jcm.mod.registry.Blocks;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.screen.base.BlockConfigScreen;
import com.lx862.jcm.mod.render.gui.widget.WidgetSet;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.ColorHelper;
import org.mtr.mod.data.IGui;
import org.mtr.mod.item.ItemDriverKey;

import java.util.function.Consumer;

public class OperatorButtonScreen extends BlockConfigScreen {
    private static final int KEY_BUTTON_SIZE = 50;
    private final boolean[] keyRequirements;
    private KeyToggleButton hoveredButton = null;

    public OperatorButtonScreen(BlockPos blockPos, boolean[] keyRequirements) {
        super(blockPos);
        this.keyRequirements = keyRequirements;
    }

    @Override
    public void init2() {
        super.init2();
        int centerX = getWidthMapped() / 2;
        int contentWidth = getContentWidth();
        int startX = (width - contentWidth) / 2;
        int startY = (int)(getHeightMapped() / 2.5f);

        int keyRowWidth = (KEY_BUTTON_SIZE * OperatorButtonBlock.ACCEPTED_KEYS.length) + ((OperatorButtonBlock.ACCEPTED_KEYS.length-1) * 10);
        WidgetSet keyRow = new WidgetSet(KEY_BUTTON_SIZE, 10);

        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            final int keyIdx = i;
            KeyToggleButton keyToggleButton = new KeyToggleButton((ItemDriverKey) OperatorButtonBlock.ACCEPTED_KEYS[keyIdx].data, keyRequirements[keyIdx], (itm) -> {
                hoveredButton = itm;
            }, 0, 0, KEY_BUTTON_SIZE, KEY_BUTTON_SIZE, (btn) -> {
                keyRequirements[keyIdx] = ((KeyToggleButton)btn.data).isAllowed();
            });
            keyRow.addWidget(keyToggleButton);
            addChild(new ClickableWidget(keyToggleButton));
        }
        keyRow.setXYSize(centerX - (keyRowWidth / 2), startY - (KEY_BUTTON_SIZE / 2), keyRowWidth, KEY_BUTTON_SIZE);
        addChild(new ClickableWidget(keyRow));

        addBottomRowEntry(startX, getHeightMapped() - 40, contentWidth, 40);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.render(graphicsHolder, mouseX, mouseY, delta);
        int startY = (int)(getHeightMapped() / 2.5f);

        MutableText allowedKeyText = TextUtil.translatable(TextCategory.GUI, "operator_button.keys_title").formatted(TextFormatting.UNDERLINE);
        graphicsHolder.push();
        graphicsHolder.translate(getWidthMapped() / 2d, startY - 45, 0);
        graphicsHolder.scale(1.5f, 1.5f, 1.5f);
        graphicsHolder.drawCenteredText(allowedKeyText, 0, 0, 0xFFFFFFFF);
        graphicsHolder.pop();

        if(hoveredButton != null) {
            ItemDriverKey key = hoveredButton.getKeyItem();
            MutableText itemText = TextUtil.translatable(key.getTranslationKey2());
            MutableText prefixTick = TextUtil.literal("✔ ").formatted(TextFormatting.DARK_GREEN);
            MutableText prefixCross = TextUtil.literal("✗ ").formatted(TextFormatting.DARK_RED);

            graphicsHolder.push();
            graphicsHolder.translate(getWidthMapped() / 2d, startY + 40, 0);
            graphicsHolder.translate(0, 0, 0);
            graphicsHolder.scale(1.5f, 1.5f, 1.5f);
            graphicsHolder.drawCenteredText(TextHelper.append((hoveredButton.isAllowed() ? prefixTick : prefixCross), itemText.formatted(TextFormatting.WHITE)), 0, 0, key.color);
            graphicsHolder.pop();

            graphicsHolder.push();
            graphicsHolder.translate(getWidthMapped() / 2d, startY + 60, 0);

            MutableText canDrive = TextHelper.append(TextUtil.translatable(TextCategory.GUI, "operator_button.key.drivable").formatted(TextFormatting.YELLOW), getBooleanText(key.canDrive));
            graphicsHolder.drawCenteredText(canDrive, 0, 0, IGui.ARGB_WHITE);
            graphicsHolder.translate(0, 10, 0);

            MutableText canOpenDoor = TextHelper.append(TextUtil.translatable(TextCategory.GUI, "operator_button.key.door").formatted(TextFormatting.YELLOW), getBooleanText(key.canOpenDoors));
            graphicsHolder.drawCenteredText(canOpenDoor, 0, 0, IGui.ARGB_WHITE);
            graphicsHolder.translate(0, 10, 0);

            MutableText canBoardVehicles = TextHelper.append(TextUtil.translatable(TextCategory.GUI, "operator_button.key.board").formatted(TextFormatting.YELLOW), getBooleanText(key.canBoardAnyVehicle));
            graphicsHolder.drawCenteredText(canBoardVehicles, 0, 0, IGui.ARGB_WHITE);

            graphicsHolder.pop();
        }
    }

    @Override
    public MutableText getScreenTitle() {
        return Blocks.OPERATOR_BUTTON.get().getName();
    }

    @Override
    protected void onSave() {
        Networking.sendPacketToServer(new OperatorButtonUpdatePacket(blockPos, keyRequirements));
    }

    private MutableText getBooleanText(boolean bl) {
        MutableText text = TextUtil.translatable(bl ? "gui.yes" : "gui.no");
        return text.formatted(bl ? TextFormatting.GREEN : TextFormatting.RED);
    }

    static class KeyToggleButton extends ButtonWidgetExtension {
        private static final Identifier KEY_TEXTURE = new Identifier("mtr:textures/item/driver_key.png");
        private static final int BORDER_PADDING = 8;
        private static final int SELECTED_COLOR = 0xFF00FF00;
        private static final int DESELECTED_COLOR = 0xFFFF0000;
        private final ItemDriverKey keyItem;
        private final Consumer<KeyToggleButton> hoverCallback;
        private boolean allowed;
        private boolean lastHovered;

        public KeyToggleButton(ItemDriverKey keyItem, boolean allowed, Consumer<KeyToggleButton> hoverCallback, int x, int y, int width, int height, org.mtr.mapping.holder.PressAction onPress) {
            super(x, y, width, height, onPress);
            this.keyItem = keyItem;
            this.hoverCallback = hoverCallback;
            this.allowed = allowed;
        }

        @Override
        public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
            super.render(graphicsHolder, mouseX, mouseY, delta);

            if (this.visible) {
                GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
                ColorHelper.unpackColor(GuiHelper.MAX_ALPHA | keyItem.color, (a, r, g, b) -> {
                    #if MC_VERSION <= "11605"
                    RenderSystem.color4f(r / 255f, g / 255f, b / 255f, a / 255f);
                    #else
                    RenderSystem.setShaderColor(r / 255f, g / 255f, b / 255f, a / 255f);
                    #endif
                    GuiHelper.drawTexture(guiDrawing, KEY_TEXTURE, getX2() + BORDER_PADDING, getY2() + BORDER_PADDING, getWidth2() - (BORDER_PADDING * 2), getHeight2() - (BORDER_PADDING * 2));

                    #if MC_VERSION <= "11605"
                    RenderSystem.color4f(1, 1, 1, 1);
                    #else
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    #endif
                });

                int borderColor = allowed ? SELECTED_COLOR : DESELECTED_COLOR;
                GuiHelper.drawRectangle(guiDrawing, getX2(), getY2(), getWidth2(), 1, borderColor);
                GuiHelper.drawRectangle(guiDrawing, getX2(), getY2() + getHeight2()-1, getWidth2(), 1, borderColor);
                GuiHelper.drawRectangle(guiDrawing, getX2(), getY2(), 1, getHeight2(), borderColor);
                GuiHelper.drawRectangle(guiDrawing, getX2() + getWidth2()-1, getY2(), 1, getHeight2(), borderColor);

                if(this.lastHovered != isHovered()) {
                    hoverCallback.accept(isHovered() ? this : null);
                    this.lastHovered = isHovered();
                }
            }
        }

        @Override
        public void onPress2() {
            this.allowed = !this.allowed;
            super.onPress2();
        }

        public ItemDriverKey getKeyItem() {
            return keyItem;
        }

        public boolean isAllowed() {
            return this.allowed;
        }
    }
}
