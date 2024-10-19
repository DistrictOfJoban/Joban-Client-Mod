package com.lx862.jcm.mod.data.scripting;

import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.scripting.util.Matrices;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.ArrayList;
import java.util.List;

public class PIDSScriptObject {
    private final List<PIDSComponent> drawCalls;
    private final String[] customMessages;
    private final boolean[] rowHidden;
    private final boolean platformNumberHidden;

    // TODO: Matrices won't work yet
    public final Matrices matrices;

    public PIDSScriptObject(GraphicsHolder graphicsHolder, String[] customMessages, boolean[] rowHidden, boolean platformNumberHidden) {
        this.drawCalls = new ArrayList<>();
        this.rowHidden = rowHidden;
        this.customMessages = customMessages;
        this.platformNumberHidden = platformNumberHidden;
        this.matrices = new Matrices(graphicsHolder);
    }

    public void drawComponent(PIDSComponent component) {
        if(component != null) {
            this.drawCalls.add(component);
        }
    }

    public boolean isRowHidden(int i) {
        if(i >= rowHidden.length) {
            return false;
        } else {
            return rowHidden[i];
        }
    }

    public String getCustomMessage(int i) {
        return i >= customMessages.length ? null : customMessages[i];
    }

    public boolean isPlatformNumberHidden() {
        return platformNumberHidden;
    }

    public List<PIDSComponent> getDrawCalls() {
        return drawCalls;
    }
}
