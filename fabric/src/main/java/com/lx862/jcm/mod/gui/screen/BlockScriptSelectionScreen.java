package com.lx862.jcm.mod.gui.screen;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.gui.screen.SavableScreen;
import com.lx862.amberui.gui.widget.ContentItem;
import com.lx862.amberui.gui.widget.HorizontalWidgetSet;
import com.lx862.amberui.gui.widget.ListViewWidget;
import com.lx862.amberui.gui.widget.MappedWidget;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.scripting.block.NativeBlockScriptBindings;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.mtrscripting.mod.gui.screen.EyecandySelectionScreen;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

import java.util.ArrayList;
import java.util.List;

/**
 * The block-script selection screen for the three ABI 16 kinds — lists
 * every {@code levelCrossingScripts} / {@code fareMachineScripts} /
 * {@code announcementScripts} entry with a "no script" entry to remove
 * the binding. Opened by right-clicking a FangSu level crossing / ticket
 * machine / announcement speaker with the PSD Script Editor item; the
 * binding is client-local (visual-only — see
 * {@link NativeBlockScriptBindings}).
 */
public class BlockScriptSelectionScreen extends SavableScreen implements RenderHelper, GuiHelper {
    private final BlockPos pos;
    private final String kind;
    private final String typeName;
    private final TextFieldWidgetExtension searchBox;
    private final ListViewWidget listViewWidget;
    private final List<Runnable> selectListener = new ArrayList<>();

    private String currentScriptId = "";

    /** One selectable option. */
    private static final class Option {
        final String id;
        final String displayName;
        final boolean isNone;
        final int color;

        Option(String id, String displayName, boolean isNone, int color) {
            this.id = id;
            this.displayName = displayName;
            this.isNone = isNone;
            this.color = color;
        }
    }

    public BlockScriptSelectionScreen(BlockPos pos, String kind) {
        super(false);
        this.pos = pos;
        this.kind = kind;
        this.typeName = kind;
        this.listViewWidget = new ListViewWidget();
        this.searchBox = new TextFieldWidgetExtension(0, 0, 0, 22, 60, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "widget.search").getString());

        final NativeBlockScriptBindings.Binding existing = NativeBlockScriptBindings.get(pos);
        if (existing != null && existing.kind().equals(kind)) {
            this.currentScriptId = existing.scriptId();
        }
    }

    @Override
    protected void init2() {
        super.init2();
        int contentWidth = (int) Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int) ((height - 90) * 0.76);
        int startX = (width - contentWidth) / 2;
        int searchStartY = TEXT_PADDING * 5;
        int startY = searchStartY + (TEXT_PADDING * 3);

        selectListener.clear();
        listViewWidget.reset();
        addOptions();
        updateSelectedEntry();

        searchBox.setX2(startX);
        searchBox.setY2(searchStartY);
        searchBox.setWidth2(contentWidth);
        searchBox.setChangedListener2(listViewWidget::setSearchTerm);
        searchBox.setText2(searchBox.getText2());

        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addChild(new ClickableWidget(listViewWidget));
        addChild(new ClickableWidget(searchBox));
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "block_script.title");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "block_script.subtitle", surfaceName());
    }

    private String surfaceName() {
        return TextUtil.translatable(TextCategory.GUI, "block_script.kind." + typeName).getString();
    }

    private List<Option> collectOptions() {
        final List<Option> options = new ArrayList<>();
        options.add(new Option("", TextUtil.translatable(TextCategory.GUI, "psd_script.none").getString(), true, 0xFF888888));
        for (String id : MtrScriptingResourceManager.blockScripts.getScriptsOfType(typeName)) {
            options.add(new Option(id, id, false, 0xFF37B3E0));
        }
        return options;
    }

    private void addOptions() {
        for (Option option : collectOptions()) {
            addOptionEntry(option);
        }
    }

    private void addOptionEntry(Option option) {
        final ButtonWidgetExtension selectBtn = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.choose"), (btn) -> {
            choose(option);
            updateSelectedEntry();
        });

        selectListener.add(() -> {
            final boolean selected = option.id.equals(currentScriptId) && !(option.isNone && !currentScriptId.isEmpty());
            if (selected) {
                selectBtn.setMessage2(Text.cast(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.selected")));
                selectBtn.active = false;
            } else {
                selectBtn.setMessage2(Text.cast(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.choose")));
                selectBtn.active = true;
            }
        });

        final HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
        widgetSet.addWidget(new MappedWidget(selectBtn));
        widgetSet.setXYSize(0, 0, 70, 20);

        addChild(new ClickableWidget(selectBtn));
        final ContentItem contentItem = new ContentItem(
                TextUtil.literal(option.displayName),
                new MappedWidget(widgetSet), 26);

        contentItem.setIconCallback((guiDrawing, startX, startY, width, height) ->
                EyecandySelectionScreen.drawIcon(option.color, guiDrawing, startX, startY, width, height));
        listViewWidget.add(contentItem);
    }

    private void updateSelectedEntry() {
        selectListener.forEach(Runnable::run);
    }

    private void choose(Option option) {
        currentScriptId = option.id;
        NativeBlockScriptBindings.set(pos, kind, option.id);
    }

    @Override
    protected void onSave() {
        // Applied immediately per choice; nothing to save.
    }
}
