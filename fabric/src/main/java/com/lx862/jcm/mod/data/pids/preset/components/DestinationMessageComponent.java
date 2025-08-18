package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import org.mtr.core.data.Route;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mod.InitClient;
import org.mtr.mod.generated.lang.TranslationProvider;

public class DestinationMessageComponent extends TextComponent {
    private final int arrivalIndex;
    private final String customMessageOverride;

    public DestinationMessageComponent(double x, double y, double width, double height, KVPair additionalParam) {
        super(x, y, width, height, additionalParam);
        this.arrivalIndex = additionalParam.getInt("arrivalIndex", 0);
        this.customMessageOverride = additionalParam.get("customMessageOverride", "");
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        ObjectArrayList<ArrivalResponse> arrivals = context.arrivals;
        String displayString;

        ArrivalResponse arrival = arrivals.get(arrivalIndex);
        displayString = getDisplayString(arrival).string();
        if(arrival.getCircularState() == Route.CircularState.CLOCKWISE) {
            displayString = (isCjk(displayString, false) ? TranslationProvider.GUI_MTR_CLOCKWISE_VIA_CJK : TranslationProvider.GUI_MTR_CLOCKWISE_VIA).getString(displayString);
        } else if(arrival.getCircularState() == Route.CircularState.ANTICLOCKWISE) {
            displayString = (isCjk(displayString, false) ? TranslationProvider.GUI_MTR_ANTICLOCKWISE_VIA_CJK : TranslationProvider.GUI_MTR_ANTICLOCKWISE_VIA).getString(displayString);
        }

        drawText(graphicsHolder, guiDrawing, facing, displayString);
    }

    public PIDSDisplay getDisplayString(ArrivalResponse arrival) {
        String customMessage = customMessageOverride == null ? "" : customMessageOverride;
        final int languageTicks = (int) Math.floor(InitClient.getGameTick()) / TextComponent.SWITCH_LANG_DURATION;
        final String[] destinationSplit;
        final String[] customMessageSplit = customMessage.split("\\|", -1);
        final String[] tempDestinationSplit = arrival.getDestination().split("\\|", -1);
        if (arrival.getRouteNumber().isEmpty()) {
            destinationSplit = tempDestinationSplit;
        } else {
            final String[] tempNumberSplit = arrival.getRouteNumber().split("\\|", -1);
            int destinationIndex = 0;
            int numberIndex = 0;
            final ObjectArrayList<String> newDestinations = new ObjectArrayList<>();
            while (true) {
                final String newDestination = String.format("%s %s", tempNumberSplit[numberIndex % tempNumberSplit.length], tempDestinationSplit[destinationIndex % tempDestinationSplit.length]);
                if (newDestinations.contains(newDestination)) {
                    break;
                } else {
                    newDestinations.add(newDestination);
                }
                destinationIndex++;
                numberIndex++;
            }
            destinationSplit = newDestinations.toArray(new String[0]);
        }
        final int messageCount = destinationSplit.length + (customMessage.isEmpty() ? 0 : customMessageSplit.length);
        boolean renderCustomMessage = !customMessage.isEmpty() && languageTicks % messageCount >= destinationSplit.length;
        int languageIndex = (languageTicks % messageCount) - (renderCustomMessage ? destinationSplit.length : 0);
        String strToDisplay = renderCustomMessage ? customMessageSplit[languageIndex] : destinationSplit[languageIndex];

        return new PIDSDisplay(strToDisplay, renderCustomMessage);
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new DestinationMessageComponent(x, y, width, height, new KVPair(jsonObject));
    }

    public static class PIDSDisplay {
        private final String string;
        private final boolean isRenderingCustomMessage;

        public PIDSDisplay(String string, boolean isRenderingCustomMessage) {
            this.string = string;
            this.isRenderingCustomMessage = isRenderingCustomMessage;
        }

        public String string() {
            return this.string;
        }

        public boolean isRenderingCustomMessage() {
            return this.isRenderingCustomMessage;
        }
    }
}
