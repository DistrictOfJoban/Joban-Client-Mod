package com.lx862.jcm.mod.gui;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.config.ClientConfig;
import com.lx862.jcm.mod.gui.base.BasicScreenBase;
import com.lx862.jcm.mod.gui.widget.ButtonSetsWidget;
import com.lx862.jcm.mod.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;

import java.io.File;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class ClientConfigScreen extends BasicScreenBase implements GuiHelper {
    private static final Identifier TEXTURE_BACKGROUND = new Identifier("jsblock:textures/gui/config_screen/bg.png");
    private static final Identifier TEXTURE_STAR = new Identifier("jsblock:textures/gui/config_screen/stars.png");
    private static final Identifier TEXTURE_TERRAIN = new Identifier("jsblock:textures/gui/config_screen/terrain.png");
    private final ButtonSetsWidget bottomRowWidget;
    private final ListViewWidget listViewWidget;
    private final boolean welcome = Calendar.getInstance().get(Calendar.MONTH) == Calendar.JUNE || Math.random() > 0.9;
    private boolean discardConfig = false;



    CheckboxWidgetExtension disableRenderingButton = new CheckboxWidgetExtension(0, 0, 20, 20, false, bool -> {
        ClientConfig.DISABLE_RENDERING.set(bool);
    });

    CheckboxWidgetExtension useCustomFontButton = new CheckboxWidgetExtension(0, 0, 20, 20, false, bool -> {
        ClientConfig.USE_CUSTOM_FONT.set(bool);
    });

    CheckboxWidgetExtension debugModeButton = new CheckboxWidgetExtension(0, 0, 20, 20, false, bool -> {
        ClientConfig.DEBUG_MODE.set(bool);
    });

    ButtonWidgetExtension testScreenButton = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "config.entries.widget.open_test_screen"), (buttonWidget -> {
        MinecraftClient.getInstance().openScreen(new Screen(
                new TestScreen().withPreviousScreen(new Screen(this))
        ));
    }));
    public ClientConfigScreen() {
        super(true);
        bottomRowWidget = new ButtonSetsWidget(20);
        listViewWidget = new ListViewWidget(20);
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
        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);

        addBottomButtons();
        bottomRowWidget.setXYSize(startX, startY + listViewHeight + BOTTOM_ROW_MARGIN, contentWidth, bottomEntryHeight);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
        listViewWidget.render(graphicsHolder, mouseX, mouseY, tickDelta);
    }
    private void setEntryStateFromClientConfig() {
        disableRenderingButton.setChecked(ClientConfig.DISABLE_RENDERING.get());
        useCustomFontButton.setChecked(ClientConfig.USE_CUSTOM_FONT.get());
        debugModeButton.setChecked(ClientConfig.DEBUG_MODE.get());
    }

    private void addConfigEntries() {
        setEntryStateFromClientConfig();

        listViewWidget.addCategory(TextHelper.literal("General"));
        listViewWidget.add(TextUtil.literal(ClientConfig.DISABLE_RENDERING.getTitle()), new MappedWidget(disableRenderingButton));
        listViewWidget.add(TextUtil.literal(ClientConfig.USE_CUSTOM_FONT.getTitle()), new MappedWidget(useCustomFontButton));

        listViewWidget.addCategory(TextHelper.literal("Debug"));
        listViewWidget.add(TextUtil.literal(ClientConfig.DEBUG_MODE.getTitle()), new MappedWidget(debugModeButton));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "config.entries.open_test_screen"), new MappedWidget(testScreenButton));

        addChild(new ClickableWidget(disableRenderingButton));
        addChild(new ClickableWidget(useCustomFontButton));
        addChild(new ClickableWidget(debugModeButton));
        addChild(new ClickableWidget(testScreenButton));
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
            ClientConfig.resetConfig();
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
        drawTexture(guiDrawing, TEXTURE_BACKGROUND, 0, 0, width, height);
        drawTexture(guiDrawing, TEXTURE_STAR, 0, translateY * 0.2, starSize, starSize, 0, 0, starUVSize, starUVSize);

        if(welcome) drawPride(graphicsHolder);
        drawTexture(guiDrawing, TEXTURE_TERRAIN, 0, translateY + height - terrainHeight, width, terrainHeight);
    }

    private void drawPride(GraphicsHolder graphicsHolder) {
        graphicsHolder.push();
        graphicsHolder.rotateYDegrees(5);
        graphicsHolder.scale((float) animationProgress, 1, 1);
        double prideAnimationProgress = closing ? 1 : animationProgress;
        int startY = (height / 2);
        int h = height / 46;

        // TODO: where my matrices :(
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        drawRectangle(guiDrawing, 0, startY, width * prideAnimationProgress, h, 0xFFDF6277);
        drawRectangle(guiDrawing, 0, startY + h, width * prideAnimationProgress, h, 0xFFFB9168);
        drawRectangle(guiDrawing, 0, startY + h*2, width * prideAnimationProgress, h, 0xFFF3DB6C);
        drawRectangle(guiDrawing, 0, startY + h*3, width * prideAnimationProgress, h, 0xFF7AB392);
        drawRectangle(guiDrawing, 0, startY + h*4, width * prideAnimationProgress, h, 0xFF4B7CBC);
        drawRectangle(guiDrawing, 0, startY + h*5, width * prideAnimationProgress, h, 0xFF6F488C);
        graphicsHolder.pop();
    }

    @Override
    public void onClose2() {
        if(!discardConfig) {
            ClientConfig.writeFile();
        } else {
            // Don't save our change to disk, and read it from disk, effectively discarding the config
            ClientConfig.readFile();
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
