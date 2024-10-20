package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;

public class PIDSWrapper {
    private final PIDSBlockEntity be;

    public PIDSWrapper( PIDSBlockEntity be) {
        this.be = be;
    }

    public boolean isRowHidden(int i) {
        boolean[] rowHidden = be.getRowHidden();
        if(i >= rowHidden.length) {
            return false;
        } else {
            return rowHidden[i];
        }
    }

    public String getCustomMessage(int i) {
        String[] customMessages = be.getCustomMessages();
        return i >= customMessages.length ? null : customMessages[i];
    }

    public boolean isPlatformNumberHidden() {
        return be.platformNumberHidden();
    }
}
