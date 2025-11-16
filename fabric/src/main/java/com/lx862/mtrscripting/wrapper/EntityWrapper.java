package com.lx862.mtrscripting.wrapper;

import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.Entity;

public class EntityWrapper {
    private final Entity entity;

    public EntityWrapper(Entity entity) {
        this.entity = entity;
    }

    public String name() {
        return this.entity.getName().getString();
    }

    public String uuid() {
        return this.entity.getUuidAsString();
    }

    public boolean hasPermissionLevel(int permLevel) {
        return this.entity.hasPermissionLevel(permLevel);
    }

    public ScriptVector3f pos() {
        return new ScriptVector3f(this.entity.getPos());
    }

    public ScriptVector3f blockPos() {
        return new ScriptVector3f(this.entity.getBlockPos());
    }
}
