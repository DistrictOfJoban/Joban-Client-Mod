package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.Entry;
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
    private final Identifier balance;
    private final Identifier font;
    private final List<Entry> entries;
    private final MinecraftClient client = MinecraftClient.getInstance();

    public EnquiryScreen(List<Entry> entries) {
        super(false);
        this.entries = entries;

        this.balance = new Identifier(Constants.MOD_ID, "textures/enquiry/transactions_blue.png");
        this.font = new Identifier(Constants.MOD_ID, "font1");
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
            MutableText renderText = getMutableText(i);
            graphicsHolder.drawText(TextUtil.withFont(renderText, font), startX, renderY, 0xFFFFFF, false, GraphicsHolder.getDefaultLight());
            renderY += 10;
            count++;
        }

        if (!entries.isEmpty()) {
            Entry lastEntry = entries.get(entries.size() - 1);
            if (lastEntry.balance() >= 0) {
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "enquiry_screen.balance", "$" + String.valueOf(lastEntry.balance())), font), startX, startY - 20, 0xFFFFFF, false, GraphicsHolder.getDefaultLight());
            } else if (lastEntry.balance() < 0) {
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "enquiry_screen.balance", "-$" + String.valueOf(Math.abs(lastEntry.balance()))), font), startX, startY - 20, 0xFFFFFF, false, GraphicsHolder.getDefaultLight());
            }
        }

        String lastDate = "";

        for (Entry entry : entries) {
            if (entry.fare() > 0) {
                lastDate = entry.date();
            }
        }

        if (!lastDate.isEmpty()) {
            String formattedDate = lastDate.substring(0, 10);

            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "enquiry_screen.add_balance", formattedDate), font), startX, startY + 100, 0xFFFFFF, false, GraphicsHolder.getDefaultLight());
        }

        graphicsHolder.pop();
    }

    private MutableText getMutableText(int i) {
        Entry entry = entries.get(i);
        String renderTextString;
        if (entry.fare() < 0) {
            renderTextString = String.format("%s     %s     -$%.2f", entry.date(), entry.station(), Math.abs((double) entry.fare()));
        } else {
            renderTextString = String.format("%s     %s     +$%.2f", entry.date(), entry.station(), (double) entry.fare());
        }
        MutableText renderText = TextUtil.literal(renderTextString);
        return renderText;
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}