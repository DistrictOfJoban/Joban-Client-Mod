package com.lx862.jcm.mod.gui.screen;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.gui.screen.SavableScreen;
import com.lx862.amberui.gui.widget.ContentItem;
import com.lx862.amberui.gui.widget.HorizontalWidgetSet;
import com.lx862.amberui.gui.widget.ListViewWidget;
import com.lx862.amberui.gui.widget.MappedWidget;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.data.pids.preset.ScriptPIDSPreset;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.scripting.psd.PSDBinding;
import com.lx862.jcm.mod.scripting.psd.PSDClientBindings;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.mtrscripting.mod.gui.screen.EyecandySelectionScreen;
import com.lx862.mtrscripting.mod.network.packet.psd.PSDScriptBindC2SPacket;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.mtrscripting.mod.resource.PSDResourceProvider;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

import java.util.ArrayList;
import java.util.List;

/**
 * The PSD Script Editor's selection screen — lists every {@code psdScripts}
 * entry of the clicked surface ("glass" / "top") plus every scripted PIDS
 * preset, with a "no script" entry to remove the binding. Selecting an entry
 * sends the bind packet to the server, which stores it and broadcasts it to
 * every player in the dimension.
 */
public class PSDScriptSelectionScreen extends SavableScreen implements RenderHelper, GuiHelper {
    private final BlockPos blockPos;
    /** The other half of a glass column (null for psd_top bindings) — a choice binds both. */
    private final BlockPos partnerPos;
    private final PSDWrapper.PSDType type;
    private final TextFieldWidgetExtension searchBox;
    private final ListViewWidget listViewWidget;
    private final List<Runnable> selectListener = new ArrayList<>();

    private String currentScriptId = "";
    private boolean currentIsPidsPreset = false;

    /** One selectable option. */
    private static final class Option {
        final String id;
        final String displayName;
        final boolean pidsPreset;
        final boolean isNone;
        final int color;

        Option(String id, String displayName, boolean pidsPreset, boolean isNone, int color) {
            this.id = id;
            this.displayName = displayName;
            this.pidsPreset = pidsPreset;
            this.isNone = isNone;
            this.color = color;
        }
    }

    public PSDScriptSelectionScreen(BlockPos blockPos, BlockPos partnerPos, PSDWrapper.PSDType type) {
        super(false);
        this.blockPos = blockPos;
        this.partnerPos = partnerPos;
        this.type = type;
        this.listViewWidget = new ListViewWidget();
        this.searchBox = new TextFieldWidgetExtension(0, 0, 0, 22, 60, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "widget.search").getString());

        final PSDBinding binding = PSDClientBindings.get(blockPos);
        if (binding != null) {
            this.currentScriptId = binding.scriptId;
            this.currentIsPidsPreset = binding.pidsPreset;
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
        return TextUtil.translatable(TextCategory.GUI, "psd_script.title");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "psd_script.subtitle", type == PSDWrapper.PSDType.GLASS
                ? TextUtil.translatable(TextCategory.GUI, "psd_script.surface.glass").getString()
                : TextUtil.translatable(TextCategory.GUI, "psd_script.surface.top").getString());
    }

    private List<Option> collectOptions() {
        final List<Option> options = new ArrayList<>();
        options.add(new Option("", TextUtil.translatable(TextCategory.GUI, "psd_script.none").getString(), false, true, 0xFF888888));

        for (PSDResourceProvider.PSDScriptConfiguration config : MtrScriptingResourceManager.psd.getScriptsOfType(type)) {
            options.add(new Option(config.id(), config.id(), false, false, 0xFFFC9700));
        }

        /* Yanyang fix: scripted PIDS presets are bindable to PSD glass/top —
           the render path (PSDScriptRenderer) resolves any preset id through
           PIDSManager.getPreset() with no builtin filter, yet the old list
           only iterated getCustomPresets(), so the builtin PIDS 1A preset
           (the scripted preset this mod itself ships) never appeared in the
           editor and, without a pack-provided scripted preset, the whole
           PIDS section was empty. Offer builtin scripted presets first, then
           pack-provided ones — the same grouping PIDSPresetScreen uses.
           Static image presets (JsonPIDSPreset) can't draw on the PSD canvas
           and stay filtered out. */
        final List<PIDSPresetBase> scriptedPresets = new ArrayList<>();
        for (PIDSPresetBase preset : PIDSManager.getBuiltInPresets()) {
            if (preset instanceof ScriptPIDSPreset) scriptedPresets.add(preset);
        }
        for (PIDSPresetBase preset : PIDSManager.getCustomPresets()) {
            if (preset instanceof ScriptPIDSPreset) scriptedPresets.add(preset);
        }
        for (PIDSPresetBase preset : scriptedPresets) {
            options.add(new Option(preset.getId(), preset.getName(), true, false, 0xFF00A870));
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
            final boolean selected = option.id.equals(currentScriptId) && option.pidsPreset == currentIsPidsPreset && !(option.isNone && !currentScriptId.isEmpty());
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
                TextUtil.literal(option.displayName + (option.pidsPreset ? " (PIDS)" : "")),
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
        currentIsPidsPreset = option.pidsPreset;
        com.lx862.jcm.mod.registry.Networking.sendPacketToServer(new PSDScriptBindC2SPacket(blockPos, option.id, option.pidsPreset, type.name()));
        if (partnerPos != null) {
            com.lx862.jcm.mod.registry.Networking.sendPacketToServer(new PSDScriptBindC2SPacket(partnerPos, option.id, option.pidsPreset, type.name()));
        }
        /* Optimistically apply locally too — the server's authoritative
           broadcast replaces it a tick later. */
        PSDClientBindings.acceptLocalBind(blockPos, option.id, option.pidsPreset, type);
        if (partnerPos != null) {
            PSDClientBindings.acceptLocalBind(partnerPos, option.id, option.pidsPreset, type);
        }
    }

    @Override
    protected void onSave() {
        // The bind packet was already sent per choice; nothing to save.
    }
}
