package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.Entry;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.screen.base.AnimatedScreen;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.List;

public class EnquiryScreen extends AnimatedScreen {
    private final Identifier putCardScreen;
    private final Identifier balance;
    private final Identifier font;
    private final Identifier octopuscard;
    private final List<Entry> entries;
    private boolean showBalance = false;
    private final MinecraftClient client = MinecraftClient.getInstance();
    public EnquiryScreen(boolean animatable, List<Entry> entries) {
        super(animatable);
        this.entries = entries;

        this.putCardScreen = new Identifier(Constants.MOD_ID, "textures/enquiry/card.png");
        this.balance = new Identifier(Constants.MOD_ID, "textures/enquiry/transactions.png");
        this.font = new Identifier(Constants.MOD_ID, "font1");
        this.octopuscard = new Identifier(Constants.MOD_ID, "textures/enquiry/octopus_card.png");
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
            for (int i = entries.size() - 1; i >= 0 && count < 10; i--) {
                MutableText renderText = getMutableText(i);
                graphicsHolder.drawText(TextUtil.withFont(renderText, font), startX, renderY, 0x000000, false, GraphicsHolder.getDefaultLight());
                renderY += 10;
                count++;
            }

            if (!entries.isEmpty()) {
                Entry lastEntry = entries.get(entries.size() - 1);
                if (lastEntry.balance() >= 0) {
                    graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("$" + String.valueOf(lastEntry.balance())), font), startX + 270, 20, 0x000000, false, GraphicsHolder.getDefaultLight());
                } else if (lastEntry.balance() < 0) {
                    graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("-$" + String.valueOf(Math.abs(lastEntry.balance()))), font), startX + 270, 20, 0x000000, false, GraphicsHolder.getDefaultLight());
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

                graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal(formattedDate), font), startX + 305, 145, 0x000000, false, GraphicsHolder.getDefaultLight());
            }

            graphicsHolder.pop();
        }
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
