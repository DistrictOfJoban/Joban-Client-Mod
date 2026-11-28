package com.lx862.jcm.mod.gui.screen;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.gui.screen.TitledScreen;
import com.lx862.amberui.gui.widget.ContentItem;
import com.lx862.amberui.gui.widget.HorizontalWidgetSet;
import com.lx862.amberui.gui.widget.ListViewWidget;
import com.lx862.amberui.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The in-game script browser — searches Modrinth for resource packs
 * (the form community script packs ship in), shows title / author /
 * downloads, and installs a pack's latest primary file straight into
 * {@code resourcepacks/} with one click. Enable the pack in the resource
 * pack screen and press F3+T (or let the hot-reload watcher do it) to
 * activate the scripts it carries.
 */
public class ModrinthBrowserScreen extends TitledScreen implements GuiHelper {

    private static final int SEARCH_LIMIT = 20;

    private final TextFieldWidgetExtension searchBox;
    private final ListViewWidget listViewWidget;
    private final List<ModrinthApi.SearchHit> hits = new ArrayList<>();
    private final List<Runnable> installListeners = new ArrayList<>();
    private String statusKey = "modrinth.ready";
    private String statusDetail = "";
    private boolean busy;

    public ModrinthBrowserScreen() {
        super(false);
        this.listViewWidget = new ListViewWidget();
        this.searchBox = new TextFieldWidgetExtension(0, 0, 0, 22, 60, TextCase.DEFAULT, null,
                TextUtil.translatable(TextCategory.GUI, "modrinth.search_hint").getString());
    }

    @Override
    protected void init2() {
        super.init2();
        int contentWidth = (int) Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int) ((height - 110) * 0.76);
        int startX = (width - contentWidth) / 2;
        int searchStartY = TEXT_PADDING * 5;
        int startY = searchStartY + (TEXT_PADDING * 3);

        listViewWidget.reset();
        installListeners.clear();
        rebuildList();

        searchBox.setX2(startX);
        searchBox.setY2(searchStartY);
        searchBox.setWidth2(contentWidth);
        addChild(new org.mtr.mapping.holder.ClickableWidget(searchBox));
        addChild(new org.mtr.mapping.holder.ClickableWidget(new ButtonWidgetExtension(startX + contentWidth - 80, searchStartY, 80, 20,
                TextUtil.translatable(TextCategory.GUI, "modrinth.search"), btn -> runSearch())));

        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addChild(new org.mtr.mapping.holder.ClickableWidget(listViewWidget));
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "modrinth.title");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, statusKey, statusDetail);
    }

    private void runSearch() {
        if (busy) return;
        final String query = searchBox.getText2().trim();
        if (query.isEmpty()) return;
        busy = true;
        setStatus("modrinth.searching", "");
        ModrinthApi.thenOnClient(ModrinthApi.search(query, SEARCH_LIMIT),
                result -> {
                    busy = false;
                    hits.clear();
                    hits.addAll(result);
                    setStatus(result.isEmpty() ? "modrinth.no_results" : "modrinth.results", String.valueOf(result.size()));
                    init2();
                },
                error -> {
                    busy = false;
                    setStatus("modrinth.error", error);
                    JCMLogger.warn("Modrinth search failed: {}", error);
                });
    }

    private void install(ModrinthApi.SearchHit hit) {
        if (busy) return;
        busy = true;
        setStatus("modrinth.resolving", hit.title());
        ModrinthApi.thenOnClient(ModrinthApi.primaryFile(hit.projectId()),
                file -> {
                    if (file == null) {
                        busy = false;
                        setStatus("modrinth.error", "no version files");
                        return;
                    }
                    setStatus("modrinth.downloading", file.filename());
                    ModrinthApi.thenOnClient(ModrinthApi.downloadToResourcePacks(file),
                            saved -> {
                                busy = false;
                                setStatus("modrinth.installed", saved.getFileName().toString());
                                refreshButtons();
                            },
                            error -> {
                                busy = false;
                                setStatus("modrinth.error", error);
                            });
                },
                error -> {
                    busy = false;
                    setStatus("modrinth.error", error);
                });
    }

    private void refreshButtons() {
        installListeners.forEach(Runnable::run);
    }

    private void setStatus(String key, String detail) {
        this.statusKey = key;
        this.statusDetail = detail;
        /* Subtitle is drawn by the frame loop; nothing else to do. */
    }

    private void rebuildList() {
        for (ModrinthApi.SearchHit hit : hits) {
            final ButtonWidgetExtension installBtn = new ButtonWidgetExtension(0, 0, 80, 20,
                    TextUtil.translatable(TextCategory.GUI, "modrinth.install"), btn -> install(hit));
            installListeners.add(() -> installBtn.active = !busy);

            final HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
            widgetSet.addWidget(new MappedWidget(installBtn));
            widgetSet.setXYSize(0, 0, 90, 20);

            addChild(new org.mtr.mapping.holder.ClickableWidget(installBtn));
            final ContentItem contentItem = new ContentItem(
                    TextUtil.literal(hit.title() + " - " + hit.author()
                            + " (" + ModrinthApi.formatDownloads(hit.downloads()) + ")"),
                    new MappedWidget(widgetSet), 26);
            listViewWidget.add(contentItem);
        }
    }

    /** Opens the browser (keybind). */
    public static void open() {
        org.mtr.mapping.holder.MinecraftClient.getInstance().openScreen(new Screen(new ModrinthBrowserScreen()));
    }
}
