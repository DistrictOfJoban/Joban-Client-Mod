package com.lx862.mtrscripting.wrapper;

import com.lx862.jcm.mapping.LoaderImplClient;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.Entity;
import org.mtr.mapping.holder.LivingEntity;
import org.mtr.mapping.holder.MinecraftClient;

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

    public ScriptVector3f smoothPos() {
        ScriptVector3f v3f = new ScriptVector3f(this.entity.getCameraPosVec(MinecraftClient.getInstance().getTickDelta()));
        v3f.sub(new ScriptVector3f(0, entity.getStandingEyeHeight(), 0));
        return v3f;
    }

    public ScriptVector3f blockPos() {
        return new ScriptVector3f(this.entity.getBlockPos());
    }

    public float yaw() {
        return entity.getYaw(MinecraftClient.getInstance().getTickDelta());
    }

    public float pitch() {
        return entity.getPitch(MinecraftClient.getInstance().getTickDelta());
    }

    public float bodyYaw() {
        LivingEntity livingEntity = LivingEntity.cast(entity);
        return livingEntity.getBodyYawMapped();
    }

    public ScriptVector3f velocity() {
        return new ScriptVector3f(LoaderImplClient.getEntityVelocity(entity));
    }
}
