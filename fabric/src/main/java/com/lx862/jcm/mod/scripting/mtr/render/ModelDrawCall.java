package com.lx862.jcm.mod.scripting.mtr.render;

import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.jcm.mod.scripting.mtr.util.ScriptedModel;
import com.lx862.mtrscripting.util.Vector3dWrapper;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

public class ModelDrawCall extends ScriptResultCall {
    private final ScriptedModel model;
    private final StoredMatrixTransformations storedMatrixTransformations;

    public ModelDrawCall(ScriptedModel model, StoredMatrixTransformations storedMatrixTransformations) {
        this.model = model;
        this.storedMatrixTransformations = storedMatrixTransformations;
    }

    @Override
    public void run(World world, Vector3dWrapper basePos, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        if(this.storedMatrixTransformations != null) {
            storedMatrixTransformations.add(this.storedMatrixTransformations);
            storedMatrixTransformations.add(gh -> gh.translate(basePos.x(), basePos.y(), basePos.z()));
        }

        this.model.draw(storedMatrixTransformations, light);
    }
}
