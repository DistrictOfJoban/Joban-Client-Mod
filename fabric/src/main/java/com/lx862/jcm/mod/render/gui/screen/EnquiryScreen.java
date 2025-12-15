package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.TransactionEntry;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.gui.screen.base.AnimatedScreen;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mod.data.IGui;

import java.text.SimpleDateFormat;
import java.util.List;

public class EnquiryScreen extends AnimatedScreen {
    private static final int BACKGROUND_COLOR = 0xFF034AE2;
    private static final int CONTENT_WIDTH = 260;
    private static final int CONTENT_HEIGHT = 200;
    private final static Identifier font  = Constants.id("pids_lcd");
    private final int SCALE_WIDTH = 475;
    private final int DISPLAYED_ENTRIES = 10;
    private final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final List<TransactionEntry> entries;
    private final int remainingBalance;

    public EnquiryScreen(BlockPos pos, List<TransactionEntry> entries, int remainingBalance) {
        super(false);
        this.entries = entries;
        this.remainingBalance = remainingBalance;
    }

    @Override
    protected void init2() {
        ButtonWidgetExtension doneButton = new ButtonWidgetExtension(0, 0, 200, 20, TextUtil.translatable("gui.done"), (btn) -> onClose2());
        doneButton.setX2((getWidthMapped() / 2) - (doneButton.getWidth2() / 2));
        doneButton.setY2(getHeightMapped() - doneButton.getHeight2() - 10);
        addChild(new ClickableWidget(doneButton));
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.renderBackground(graphicsHolder);

        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        int screenWidth = getWidthMapped();
        int screenHeight = getHeightMapped();

        float scaleFactor = Math.min(1.75F, (float)(double)getWidthMapped() / SCALE_WIDTH);
        int scaledEnquiryScreenWidth = (int)(CONTENT_WIDTH * scaleFactor);
        int scaledEnquiryScreenHeight = (int)(CONTENT_HEIGHT * scaleFactor);

        GuiHelper.drawRectangle(guiDrawing, (screenWidth - scaledEnquiryScreenWidth) / 2.0, (screenHeight - scaledEnquiryScreenHeight) / 2.0, scaledEnquiryScreenWidth, scaledEnquiryScreenHeight, BACKGROUND_COLOR);
        graphicsHolder.drawCenteredText(TextUtil.translatable(TextCategory.BLOCK, "mtr_enquiry_machine"), getWidthMapped() / 2, ((screenHeight - scaledEnquiryScreenHeight) / 2) / 2, RenderHelper.ARGB_WHITE);

        int startX = 5;
        int startY = 5;

        graphicsHolder.push();
        graphicsHolder.translate((screenWidth - scaledEnquiryScreenWidth) / 2.0, (screenHeight - scaledEnquiryScreenHeight) / 2.0, 0);
        graphicsHolder.scale(scaleFactor, scaleFactor, scaleFactor);

        String balancePrefix = remainingBalance >= 0 ? "" : "-";
        graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "enquiry_screen.balance", balancePrefix + "$" + Math.abs(remainingBalance)), font), startX, startY, RenderHelper.ARGB_WHITE, false, GraphicsHolder.getDefaultLight());

        startY += 14;
//      MTR Balance point, dunno what to do with it yet
//        graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "enquiry_screen.points", "0"), font), startX, startY, RenderHelper.ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
        startY += 14;

        int entryToShow = Math.min(DISPLAYED_ENTRIES, entries.size());
        for(int i = 0; i < entryToShow; i++) {
            TransactionEntry entry = entries.get(i);

            graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal(TIME_FORMATTER.format(entry.time())), font), startX, startY + (i * 14), RenderHelper.ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal(IGui.formatStationName(entry.source())), font), startX + 90, startY + (i * 14), RenderHelper.ARGB_WHITE, false, GraphicsHolder.getDefaultLight());

            MutableText balText = TextUtil.withFont(entry.amount() < 0 ? TextUtil.literal("-$" + (double)-entry.amount()) : entry.amount() == 0 ?  TextUtil.literal("$" + (double)entry.amount()) : TextUtil.literal("+$" + (double)entry.amount()), font);
            int balTextWidth = GraphicsHolder.getTextWidth(balText);
            graphicsHolder.drawText(balText, CONTENT_WIDTH - balTextWidth - 5, startY + (i * 14), RenderHelper.ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
        }

        long lastAddValueTime = 0;
        for (TransactionEntry transactionEntry : entries) {
            if (transactionEntry.amount() > 0 && transactionEntry.time() > lastAddValueTime) {
                lastAddValueTime = transactionEntry.time();
            }
        }

        if (lastAddValueTime != 0) {
            String formattedDate = DATE_FORMATTER.format(lastAddValueTime);
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "enquiry_screen.add_balance", formattedDate), font), startX, CONTENT_HEIGHT - 15, RenderHelper.ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
        }

        graphicsHolder.pop();
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}