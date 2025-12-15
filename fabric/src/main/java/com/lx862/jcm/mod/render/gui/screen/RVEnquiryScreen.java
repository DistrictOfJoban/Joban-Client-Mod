package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.TransactionEntry;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.screen.base.AnimatedScreen;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mod.data.IGui;

import java.text.SimpleDateFormat;
import java.util.List;

public class RVEnquiryScreen extends AnimatedScreen {
    private static final Identifier cardScreenTexture = Constants.id("textures/enquiry/card.png");
    private static final Identifier balanceTexture = Constants.id("textures/enquiry/transactions.png");
    private static final Identifier octopusCardTexture = Constants.id("textures/enquiry/octopus_card.png");
    private static final Identifier font = new Identifier("mtr", "mtr");
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final List<TransactionEntry> entries;
    private final BlockPos pos;
    private final long remainingBalance;
    private boolean showBalance;

    public RVEnquiryScreen(BlockPos pos, List<TransactionEntry> entries, int remainingBalance) {
        super(false);
        this.pos = pos;
        this.entries = entries;
        this.remainingBalance = remainingBalance;
        this.showBalance = true; // Simplify mechanics
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.renderBackground(graphicsHolder);

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);

        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
        int baseWidth = 427;
        double scaledWidth = getWidthMapped();
        double scaledHeight = getHeightMapped();
        int startX = 20;
        int startY = 70;

        int rectX = (screenWidth - 150) / 2;
        int rectY = ((screenHeight - 70) / 2) + 65;
        int rectWidth = 150;
        int rectHeight = 70;

        boolean cursorWithinRectangle = mouseX >= rectX && mouseX <= rectX + rectWidth && mouseY >= rectY && mouseY <= rectY + rectHeight;
        if(cursorWithinRectangle) {
            showBalance = true;
        }

        if (!showBalance) {
            GuiHelper.drawTexture(guiDrawing, cardScreenTexture, (screenWidth - (int) scaledWidth) / 2.0, (screenHeight - (int) scaledHeight) / 2.0, (int) scaledWidth, (int) scaledHeight);
            GuiHelper.drawTexture(guiDrawing, octopusCardTexture, mouseX, mouseY, 140, 86);
        } else {
            GuiHelper.drawTexture(guiDrawing, balanceTexture, (screenWidth - (int) scaledWidth) / 2.0, (screenHeight - (int) scaledHeight) / 2.0, (int) scaledWidth, (int) scaledHeight);

            int renderY = startY;

            graphicsHolder.push();
            graphicsHolder.scale((float)(double)getWidthMapped() / baseWidth, (float)(double)getWidthMapped() / baseWidth, (float)(double)getWidthMapped() / baseWidth);

            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.balance"), font), startX, 20, 0, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("Octopus"), font), startX + 305, 75, 0, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.processor"), font), startX + 305, 85, 0, false, GraphicsHolder.getDefaultLight());

            String processorId = String.format("%06d", pos.asLong() % 1000000);
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal(processorId), font), startX + 305, 95, 0, false, GraphicsHolder.getDefaultLight());

            for (int i = 0; i < 10; i++) {
                MutableText renderText = getEntryText(i);
                if(renderText == null) continue;

                graphicsHolder.drawText(TextUtil.withFont(renderText, font), startX, renderY, 0, false, GraphicsHolder.getDefaultLight());
                renderY += 10;
            }

            if (!entries.isEmpty()) {
                if (remainingBalance >= 0) {
                    graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("$" + remainingBalance), font), startX + 270, 20, 0x000000, false, GraphicsHolder.getDefaultLight());
                } else {
                    graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("-$" + Math.abs(remainingBalance)), font), startX + 270, 20, 0x000000, false, GraphicsHolder.getDefaultLight());
                }
            }

            long lastDate = 0;

            for (TransactionEntry transactionEntry : entries) {
                if (transactionEntry.amount() > 0) {
                    lastDate = transactionEntry.time();
                }
            }

            if (lastDate != 0) {
                String formattedDate = TIME_FORMATTER.format(lastDate);
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.last.a"), font), startX + 305, 115, 0, false, GraphicsHolder.getDefaultLight());
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.last.b"), font), startX + 305, 125, 0, false, GraphicsHolder.getDefaultLight());
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.last.c"), font), startX + 305, 135, 0, false, GraphicsHolder.getDefaultLight());
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal(formattedDate), font), startX + 305, 145, 0x000000, false, GraphicsHolder.getDefaultLight());
            }

            graphicsHolder.pop();
        }
    }

    private MutableText getEntryText(int i) {
        if(i >= entries.size()) return null;
        TransactionEntry transactionEntry = entries.get(i);
        String renderTextString;
        if (transactionEntry.amount() < 0) {
            renderTextString = String.format("%s     %s     -$%.2f", TIME_FORMATTER.format(transactionEntry.time()), IGui.formatStationName(transactionEntry.source()), Math.abs((double) transactionEntry.amount()));
        } else if (transactionEntry.amount() > 0) {
            renderTextString = String.format("%s     %s     +$%.2f", TIME_FORMATTER.format(transactionEntry.time()), IGui.formatStationName(transactionEntry.source()), (double) transactionEntry.amount());
        } else {
            renderTextString = String.format("%s     %s     $%.2f", TIME_FORMATTER.format(transactionEntry.time()), IGui.formatStationName(transactionEntry.source()), (double) transactionEntry.amount());
        }
        return TextUtil.literal(renderTextString);
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
