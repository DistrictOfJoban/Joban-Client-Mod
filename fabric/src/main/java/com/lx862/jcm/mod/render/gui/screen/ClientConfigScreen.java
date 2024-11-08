package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.screen.base.TitledScreen;
import com.lx862.jcm.mod.render.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.render.gui.widget.MappedWidget;
import com.lx862.jcm.mod.render.gui.widget.WidgetSet;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.CheckboxWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.io.File;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class ClientConfigScreen extends TitledScreen implements GuiHelper {
    private static final Identifier TEXTURE_BACKGROUND = new Identifier("jsblock:textures/gui/config_screen/bg.png");
    private static final Identifier TEXTURE_STAR = new Identifier("jsblock:textures/gui/config_screen/stars.png");
    private static final Identifier TEXTURE_TERRAIN = new Identifier("jsblock:textures/gui/config_screen/terrain.png");
    private final WidgetSet bottomRowWidget;
    private final ListViewWidget listViewWidget;
    private boolean discardConfig = false;

    CheckboxWidgetExtension disableRenderingButton = new CheckboxWidgetExtension(0, 0, 20, 20, false, bool -> {
        JCMClient.getConfig().disableRendering = bool;
    });

    CheckboxWidgetExtension useCustomFontButton = new CheckboxWidgetExtension(0, 0, 20, 20, false, bool -> {
        JCMClient.getConfig().useCustomFont = bool;
    });

    CheckboxWidgetExtension useNewTextRendererButton = new CheckboxWidgetExtension(0, 0, 20, 20, false, bool -> {
        JCMClient.getConfig().useNewTextRenderer = bool;
    });

    CheckboxWidgetExtension debugModeButton = new CheckboxWidgetExtension(0, 0, 20, 20, false, bool -> {
        JCMClient.getConfig().debug = bool;
    });

    ButtonWidgetExtension textAtlasButton = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "config.listview.widget.open"), (buttonWidget -> {
        MinecraftClient.getInstance().openScreen(new Screen(
                new TextureTextAtlasScreen().withPreviousScreen(new Screen(this))
        ));
    }));
    
    public ClientConfigScreen() {
        super(true);
        bottomRowWidget = new WidgetSet(20);
        listViewWidget = new ListViewWidget();
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "brand");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "config.version", Constants.MOD_VERSION);
    }

    @Override
    protected void init2() {
        super.init2();
        listViewWidget.reset();
        bottomRowWidget.reset();

        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)((height - 60) * 0.75);
        int startX = (width - contentWidth) / 2;
        int startY = TEXT_PADDING * 5;

        int bottomEntryHeight = (height - startY - listViewHeight - (BOTTOM_ROW_MARGIN * 2));

        addConfigEntries();
        addBottomButtons();
        addChild(new ClickableWidget(listViewWidget));
        addChild(new ClickableWidget(bottomRowWidget));
        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        bottomRowWidget.setXYSize(startX, startY + listViewHeight + BOTTOM_ROW_MARGIN, contentWidth, bottomEntryHeight);
    }

    private void setEntryStateFromClientConfig() {
        disableRenderingButton.setChecked(JCMClient.getConfig().disableRendering);
        useCustomFontButton.setChecked(JCMClient.getConfig().useCustomFont);
        useNewTextRendererButton.setChecked(JCMClient.getConfig().useNewTextRenderer);
        debugModeButton.setChecked(JCMClient.getConfig().debug);
    }

    private void addConfigEntries() {
        setEntryStateFromClientConfig();

        // General
        listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "config.listview.category.general"));

        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "config.listview.title.disable_rendering"), new MappedWidget(disableRenderingButton));
        addChild(new ClickableWidget(disableRenderingButton));

        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "config.listview.title.custom_font"), new MappedWidget(useCustomFontButton));
        addChild(new ClickableWidget(useCustomFontButton));

        /*  New Text Renderer is deprecated, we shouldn't offer player to switch to it anymore if it isn't enabled */
        if(JCMClient.getConfig().useNewTextRenderer) {
            listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "config.listview.category.experimental"));
            listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "config.listview.title.new_text_rendering"), new MappedWidget(useNewTextRendererButton));
            addChild(new ClickableWidget(useNewTextRendererButton));
        }

        // Debug
        listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "config.listview.category.debug"));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "config.listview.title.debug_mode"), new MappedWidget(debugModeButton));
        addChild(new ClickableWidget(debugModeButton));

        if(JCMClient.getConfig().useNewTextRenderer) {
            listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "config.listview.title.open_atlas_screen"), new MappedWidget(textAtlasButton));
            addChild(new ClickableWidget(textAtlasButton));
        }
    }


    private void addBottomButtons() {
        ButtonWidgetExtension latestLogButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "config.latest_log"), (btn) -> {
            openLatestLog();
        });

        ButtonWidgetExtension crashLogButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "config.crash_log"), (btn) -> {
            openLatestCrashReport();
        });

        ButtonWidgetExtension saveButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "config.save"), (btn) -> {
            onClose2();
        });

        ButtonWidgetExtension discardButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "config.discard"), (btn) -> {
            discardConfig = true;
            onClose2();
        });

        ButtonWidgetExtension resetButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "config.reset"), (btn) -> {
            JCMClient.getConfig().reset();
            setEntryStateFromClientConfig();
        });

        addChild(new ClickableWidget(latestLogButton));
        addChild(new ClickableWidget(crashLogButton));

        addChild(new ClickableWidget(saveButton));
        addChild(new ClickableWidget(discardButton));
        addChild(new ClickableWidget(resetButton));

        bottomRowWidget.addWidget(latestLogButton);
        bottomRowWidget.addWidget(crashLogButton);
        bottomRowWidget.insertRow();
        bottomRowWidget.addWidget(saveButton);
        bottomRowWidget.addWidget(discardButton);
        bottomRowWidget.addWidget(resetButton);
    }

    @Override
    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        double terrainHeight = (width / 3.75);
        double starSize = Math.max(width, height);
        float starUVSize = (float) (starSize / 384F);
        double translateY = height * (1 - animationProgress);
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        GuiHelper.drawTexture(guiDrawing, TEXTURE_BACKGROUND, 0, 0, width, height);
        GuiHelper.drawTexture(guiDrawing, TEXTURE_STAR, 0, translateY * 0.2, starSize, starSize, 0, 0, starUVSize, starUVSize);
        GuiHelper.drawTexture(guiDrawing, TEXTURE_TERRAIN, 0, translateY + height - terrainHeight, width, terrainHeight);
    }

    @Override
    public void onClose2() {
        if(!closing) {
            if (!discardConfig) {
                JCMClient.getConfig().write();
            } else {
                // Don't save our change to disk, and read it from disk, effectively discarding the config
                JCMClient.getConfig().read();
            }
        }

        super.onClose2();
    }

    public static void openLatestLog() {
        File latestLog = Paths.get(MinecraftClient.getInstance().getRunDirectoryMapped().toString(), "logs", "latest.log").toFile();
        if(latestLog.exists()) {
            Util.getOperatingSystem().open(latestLog);
        }
    }

    public static void openLatestCrashReport() {
        File crashReportDir = Paths.get(MinecraftClient.getInstance().getRunDirectoryMapped().toString(), "crash-reports").toFile();

        if(crashReportDir.exists()) {
            File[] crashReports = crashReportDir.listFiles();
            if(crashReports != null && crashReportDir.length() > 0) {
                SimpleDateFormat crashReportFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

                Arrays.sort(crashReports, (o1, o2) -> {
                    String filename1 = o1.getName().replace("crash-", "").replace("-client", "");
                    String filename2 = o2.getName().replace("crash-", "").replace("-client", "");
                    if(o1 == o2) return 0;

                    try {
                        long ts1 = crashReportFormat.parse(filename1).getTime();
                        long ts2 = crashReportFormat.parse(filename2).getTime();
                        return ts1 > ts2 ? -1 : 1;
                    } catch (ParseException e) {
                        JCMLogger.debug("Cannot compare crash report file name " + filename1 + " <-> " + filename2);
                    }
                    return 1;
                });

                JCMLogger.debug("Latest crash report is: " + crashReports[0].getName());
                Util.getOperatingSystem().open(crashReports[0]);
            }
        }
    }
}
