package com.lx862.jcm.mod.render.screen;

import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.screen.base.BasicScreenBase;
import com.lx862.jcm.mod.render.screen.widget.ListEntry;
import com.lx862.jcm.mod.render.screen.widget.ListViewWidget;
import com.lx862.jcm.mod.render.screen.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.ButtonWidgetExtension;

import java.util.function.Consumer;

public class PIDSPresetScreen extends BasicScreenBase implements GuiHelper {
    private final ListViewWidget listViewWidget;
    private final Consumer<String> callback;
    private final String selectedPreset;
    public PIDSPresetScreen(String selectedPreset, Consumer<String> callback) {
        super(false);
        this.callback = callback;
        this.listViewWidget = new ListViewWidget();
        this.selectedPreset = selectedPreset;
    }

    @Override
    protected void init2() {
        super.init2();
        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)((height - 60) * 0.75);
        int startX = (width - contentWidth) / 2;
        int startY = TEXT_PADDING * 5;

        listViewWidget.reset();
        addConfigEntries();
        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addChild(new ClickableWidget(listViewWidget));
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "pids_preset.title");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "pids_preset.subtitle", selectedPreset);
    }

    public void addConfigEntries() {
        listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.category.builtin"));
        for(PIDSPresetBase preset : PIDSManager.getBuiltInPresets()) {
            addPreset(preset);
        }

        listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.category.custom"));
        for(PIDSPresetBase preset : PIDSManager.getCustomPresets()) {
            addPreset(preset);
        }
    }

    private void addPreset(PIDSPresetBase preset) {
        ButtonWidgetExtension we = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.choose"), (btn) -> {
            choose(preset.getId());
        });

        if(preset.getId().equals(selectedPreset)) {
            we.setMessage2(Text.cast(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.selected")));
            we.active = false;
        }

        addChild(new ClickableWidget(we));
        ListEntry listEntry = new ListEntry(TextUtil.literal(preset.getName()), new MappedWidget(we), false, 26);
        listEntry.setIcon(preset.getPreviewImage());
        listViewWidget.add(listEntry);
    }

    private void choose(String id) {
        callback.accept(id);
        onClose2();
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
