package com.lx862.mtrscripting.api;

import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

public abstract class ScriptResultCall {
    public abstract void run(World world, ScriptVector3f basePos, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light);
}
