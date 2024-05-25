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

import java.util.List;

public class EnquiryScreen extends AnimatedScreen {
    private final static Identifier balance = new Identifier(Constants.MOD_ID, "textures/enquiry/transactions_blue.png");
    private final static Identifier font  = new Identifier(Constants.MOD_ID, "font1");
    private final List<TransactionEntry> entries;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final int remainingBalance;

    public EnquiryScreen(List<TransactionEntry> entries, int remainingBalance) {
        super(false);
        this.entries = entries;
        this.remainingBalance = remainingBalance;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);

        int screenWidth = this.client.getWindow().getScaledWidth();
        int screenHeight = this.client.getWindow().getScaledHeight();

        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);

        int baseWidth = 427;
        double scaledWidth = getWidthMapped();
        double scaledHeight = getHeightMapped();
        int startX = 20;
        int startY = 60;

        int renderY = startY;
        int count = 0;

        graphicsHolder.push();
        graphicsHolder.scale((float)(double)getWidthMapped() / baseWidth, (float)(double)getWidthMapped() / baseWidth, (float)(double)getWidthMapped() / baseWidth);

        GuiHelper.drawTexture(guiDrawing, balance, (screenWidth - (int) scaledWidth) / 2, (screenHeight - (int) scaledHeight) / 2, (int) scaledWidth, (int) scaledHeight);

        graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "enquiry_screen.points", "0"), font), startX, startY - 10, 0xFFFFFF, false, GraphicsHolder.getDefaultLight());

        for (int i = entries.size() - 1; i >= 0 && count < 10; i--) {
            MutableText renderText = getEntryText(i);
            graphicsHolder.drawText(TextUtil.withFont(renderText, font), startX, renderY, 0xFFFFFF, false, GraphicsHolder.getDefaultLight());
            renderY += 10;
            count++;
        }

        if (!entries.isEmpty()) {
            TransactionEntry lastTransactionEntry = entries.get(entries.size() - 1);
            if (remainingBalance >= 0) {
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "enquiry_screen.balance", "$" + String.valueOf(remainingBalance)), font), startX, startY - 20, 0xFFFFFF, false, GraphicsHolder.getDefaultLight());
            } else if (remainingBalance < 0) {
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "enquiry_screen.balance", "-$" + String.valueOf(Math.abs(remainingBalance))), font), startX, startY - 20, 0xFFFFFF, false, GraphicsHolder.getDefaultLight());
            }
        }

        long lastTransactionTime = 0;

        for (TransactionEntry transactionEntry : entries) {
            if (transactionEntry.amount > 0 && transactionEntry.time > lastTransactionTime) {
                lastTransactionTime = transactionEntry.time;
            }
        }

        if (lastTransactionTime != 0) {
            String formattedDate = TransactionEntry.formatter.format(lastTransactionTime);
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "enquiry_screen.add_balance", formattedDate), font), startX, startY + 100, 0xFFFFFF, false, GraphicsHolder.getDefaultLight());
        }

        graphicsHolder.pop();
    }

    private MutableText getEntryText(int i) {
        TransactionEntry transactionEntry = entries.get(i);
        String str;
        if (transactionEntry.amount < 0) {
            str = String.format("%s     %s     -$%.2f", transactionEntry.getFormattedDate(), transactionEntry.source, Math.abs((double) transactionEntry.amount));
        } else {
            str = String.format("%s     %s     +$%.2f", transactionEntry.getFormattedDate(), transactionEntry.source, (double) transactionEntry.amount);
        }
        return TextUtil.literal(str);
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}