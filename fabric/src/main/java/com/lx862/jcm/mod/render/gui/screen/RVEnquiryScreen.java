package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.TransactionEntry;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.screen.base.AnimatedScreen;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.text.SimpleDateFormat;
import java.util.List;

public class RVEnquiryScreen extends AnimatedScreen {
    private static final Identifier putCardScreen = new Identifier(Constants.MOD_ID, "textures/enquiry/card.png");
    private static final Identifier balance = new Identifier(Constants.MOD_ID, "textures/enquiry/transactions.png");
    private static final Identifier font  = new Identifier(Constants.MOD_ID, "font1");
    private static final Identifier octopuscard  = new Identifier(Constants.MOD_ID, "textures/enquiry/octopus_card.png");
    private final List<TransactionEntry> entries;
    private boolean showBalance = false;
    private long remainingBalance;

    public RVEnquiryScreen(List<TransactionEntry> entries, int remainingBalance) {
        super(false);
        this.entries = entries;
        this.remainingBalance = remainingBalance;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);

        int baseWidth = 427;
        double scaledWidth = getWidthMapped();
        double scaledHeight = getHeightMapped();
        int startX = 20;
        int startY = 70;

        int rectX = (screenWidth - 150) / 2;
        int rectY = ((screenHeight - 70) / 2) + 65;
        int rectWidth = 150;
        int rectHeight = 70;

        boolean cursorWithinRectangle = mouseX >= rectX && mouseX <= rectX + rectWidth &&
                mouseY >= rectY && mouseY <= rectY + rectHeight;

        if(cursorWithinRectangle) {
            showBalance = true;
        }

        if (!showBalance) {
            GuiHelper.drawTexture(guiDrawing, putCardScreen, (screenWidth - (int) scaledWidth) / 2, (screenHeight - (int) scaledHeight) / 2, (int) scaledWidth, (int) scaledHeight);
            GuiHelper.drawTexture(guiDrawing, octopuscard, mouseX, mouseY, 140, 86);
        } else {
            GuiHelper.drawTexture(guiDrawing, balance, (screenWidth - (int) scaledWidth) / 2, (screenHeight - (int) scaledHeight) / 2, (int) scaledWidth, (int) scaledHeight);

            int renderY = startY;
            int count = 0;

            graphicsHolder.push();
            graphicsHolder.scale((float)(double)getWidthMapped() / baseWidth, (float)(double)getWidthMapped() / baseWidth, (float)(double)getWidthMapped() / baseWidth);

            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.balance"), font), startX, 20, 0x000000, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("Octopus"), font), startX + 305, 75, 0x000000, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.processor"), font), startX + 305, 85, 0x000000, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("849009"), font), startX + 305, 95, 0x000000, false, GraphicsHolder.getDefaultLight());

            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.last.a"), font), startX + 305, 115, 0x000000, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.last.b"), font), startX + 305, 125, 0x000000, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.last.c"), font), startX + 305, 135, 0x000000, false, GraphicsHolder.getDefaultLight());

            for (int i = entries.size() - 1; i >= 0 && count < 10; i--) {
                MutableText renderText = getMutableText(i);
                graphicsHolder.drawText(TextUtil.withFont(renderText, font), startX, renderY, 0x000000, false, GraphicsHolder.getDefaultLight());
                renderY += 10;
                count++;
            }

            if (!entries.isEmpty()) {
                TransactionEntry lastTransactionEntry = entries.get(entries.size() - 1);
                if (remainingBalance >= 0) {
                    graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("$" + String.valueOf(remainingBalance)), font), startX + 270, 20, 0x000000, false, GraphicsHolder.getDefaultLight());
                } else if (remainingBalance < 0) {
                    graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("-$" + String.valueOf(Math.abs(remainingBalance))), font), startX + 270, 20, 0x000000, false, GraphicsHolder.getDefaultLight());
                }
            }

            long lastDate = 0;

            for (TransactionEntry transactionEntry : entries) {
                if (transactionEntry.amount > 0) {
                    lastDate = transactionEntry.time;
                }
            }

            if (lastDate != 0) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                String formattedDate = formatter.format(lastDate);
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal(formattedDate), font), startX + 305, 145, 0x000000, false, GraphicsHolder.getDefaultLight());
            }

            graphicsHolder.pop();
        }
    }

    private MutableText getMutableText(int i) {
        TransactionEntry transactionEntry = entries.get(i);
        String renderTextString;
        if (transactionEntry.amount < 0) {
            renderTextString = String.format("%s     %s     -$%.2f", transactionEntry.time, transactionEntry.source, Math.abs((double) transactionEntry.amount));
        } else {
            renderTextString = String.format("%s     %s     +$%.2f", transactionEntry.time, transactionEntry.source, (double) transactionEntry.amount);
        }
        MutableText renderText = TextUtil.literal(renderTextString);
        return renderText;
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
