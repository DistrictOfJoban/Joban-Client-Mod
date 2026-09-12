package com.lx862.mtrscripting.mod.gui.screen;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.gui.screen.base.SavableScreen;
import com.lx862.jcm.mod.render.gui.widget.ContentItem;
import com.lx862.jcm.mod.render.gui.widget.HorizontalWidgetSet;
import com.lx862.jcm.mod.render.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.render.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.JCMBlockEyecandyExtra;
import com.lx862.mtrscripting.mod.resource.MTRContentResourceManager;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;
import org.mtr.mod.block.BlockEyeCandy;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.resource.ObjectResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EyecandySelectionScreen extends SavableScreen implements RenderHelper, GuiHelper {
    private final TextFieldWidgetExtension searchBox;
    private final ListViewWidget listViewWidget;
    private final Runnable callback;
    private final BlockPos blockPos;
    private String currentObject;

    private final LongArrayList selectedIndices;
    private final Map<String, String> storedConfigEntries = new HashMap<>();

    private final ObjectImmutableList<ObjectResource> loadedObjects = CustomResourceLoader.getObjects();

    public EyecandySelectionScreen(BlockPos blockPos, LongArrayList selectedIndices, Runnable callback) {
        super(false);
        this.blockPos = blockPos;
        this.selectedIndices = selectedIndices;
        BlockEntity be = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(blockPos);
        if(be != null && be.data instanceof BlockEyeCandy.BlockEntity) {
            this.currentObject = ((BlockEyeCandy.BlockEntity)be.data).getModelId();
            this.storedConfigEntries.putAll(new HashMap<>(((JCMBlockEyecandyExtra)be.data).jsblock$getCustomConfig()));
        } else {
            this.currentObject = "";
        }
        this.callback = callback;
        this.listViewWidget = new ListViewWidget();
        this.searchBox = new TextFieldWidgetExtension(0, 0, 0, 22, 60, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "widget.search").getString());
    }

    @Override
    protected void init2() {
        super.init2();
        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)((height - 90) * 0.76);
        int startX = (width - contentWidth) / 2;
        int searchStartY = TEXT_PADDING * 5;
        int startY = searchStartY + (TEXT_PADDING * 3);

        selectListener.clear();
        listViewWidget.reset();
        addConfigEntries(this.listViewWidget);
        searchBox.setX2(startX);
        searchBox.setY2(searchStartY);
        searchBox.setWidth2(contentWidth);
        searchBox.setChangedListener2(listViewWidget::setSearchTerm);
        searchBox.setText2(searchBox.getText2());

        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addChild(new ClickableWidget(listViewWidget));
        addChild(new ClickableWidget(searchBox));

        addBottomRowEntry(startX, startY + listViewHeight + 6, contentWidth, getHeightMapped() - (startY + listViewHeight));
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "eyecandy.title");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "eyecandy.subtitle", currentObject);
    }

    public void addConfigEntries(ListViewWidget listViewWidget) {
        for(ObjectResource objectResource : loadedObjects) {
            addObjectEntry(listViewWidget, objectResource);
        }
        updateSelectedEntry();
    }

    private final List<Runnable> selectListener = new ArrayList<>();

    private void updateSelectedEntry() {
        selectListener.forEach(Runnable::run);
    }

    private void addObjectEntry(ListViewWidget listViewWidget, ObjectResource objectResource) {
        ButtonWidgetExtension selectBtn = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.choose"), (btn) -> {
            choose(objectResource);
            updateSelectedEntry();
        });

        selectListener.add(() -> {
            if(objectResource.getId().equals(currentObject)) {
                selectBtn.setMessage2(Text.cast(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.selected")));
                selectBtn.active = false;
            } else {
                selectBtn.setMessage2(Text.cast(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.choose")));
                selectBtn.active = true;
            }
        });

        MTRContentResourceManager.EyecandyScriptConfiguration scriptConfig = MTRContentResourceManager.getEyecandyScript(objectResource.getId());
        boolean hasCustomConfig = scriptConfig != null && scriptConfig.eyecandyCustomConfig() != null;

        ButtonWidgetExtension editBtn = new ButtonWidgetExtension(0, 0, 20, 20, TextUtil.translatable(TextCategory.GUI, "eyecandy.listview.widget.edit"), (btn) -> {
            getClientMapped().openScreen(
                    new Screen(
                            new EyecandyCustomConfigScreen(
                                objectResource.getName(),
                                objectResource.getId(),
                                blockPos,
                                this.storedConfigEntries,
                                scriptConfig.eyecandyCustomConfig()
                            ).withPreviousScreen(new Screen(this))
                    )
            );
        });

        HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
        if(hasCustomConfig) widgetSet.addWidget(new MappedWidget(editBtn));
        widgetSet.addWidget(new MappedWidget(selectBtn));
        widgetSet.setXYSize(0, 0, 110, 20);

        if(hasCustomConfig) addChild(new ClickableWidget(editBtn));
        addChild(new ClickableWidget(selectBtn));
        addChild(new ClickableWidget(widgetSet));
        ContentItem contentItem = new ContentItem(TextUtil.literal(objectResource.getName()), new MappedWidget(widgetSet), 26);

        contentItem.setIconCallback((guiDrawing, startX, startY, width, height) -> {
            drawIcon(objectResource.getColor(), guiDrawing, startX, startY, width, height);
        });
        listViewWidget.add(contentItem);
    }

    public static void drawIcon(int color, GuiDrawing guiDrawing, int startX, int startY, int width, int height) {
        // Background
        GuiHelper.drawRectangle(guiDrawing, startX+4, startY+4, width-8, height-8, ARGB_BLACK | color);
    }

    private void choose(ObjectResource objectResource) {
        selectedIndices.clear();
        selectedIndices.add(loadedObjects.indexOf(objectResource));
        currentObject = objectResource.getId();
        sendUpdate();
    }

    @Override
    protected void onSave() {
        sendUpdate();
    }

    private void sendUpdate() {
        this.callback.run();

        MTRContentResourceManager.EyecandyScriptConfiguration scriptConfig = MTRContentResourceManager.getEyecandyScript(currentObject);
        boolean hasCustomConfig = scriptConfig != null && scriptConfig.eyecandyCustomConfig() != null;

        if(hasCustomConfig) {
            Map<String, String> entries = new HashMap<>();
            for(EyecandyCustomConfig.Entry key : scriptConfig.eyecandyCustomConfig().entries()) {
                String value = storedConfigEntries.getOrDefault(key.id(), key.defaultValue());
                if(value != null) entries.put(key.id(), value);
            }

            EyecandyCustomConfigScreen.sendCustomConfigUpdate(currentObject, blockPos, entries);
        }
    }
}
