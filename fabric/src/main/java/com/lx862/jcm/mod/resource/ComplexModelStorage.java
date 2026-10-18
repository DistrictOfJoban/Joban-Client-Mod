package com.lx862.jcm.mod.resource;

import com.lx862.jcm.mixin.mtrscripting.OptimizedModelWrapperAccessor;
import com.lx862.mtrscripting.core.util.model.ModelJS;
import com.lx862.mtrscripting.core.util.model.ModelManagerJS;
import com.lx862.mtrscripting.core.util.model.RawModelJS;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.resource.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplexModelStorage {
    private static final Map<Identifier, OptimizedModelWrapper> models = new HashMap<>();
    private static final List<Identifier> modelToLoad = new ArrayList<>();

    public static void requestModelLoad(Identifier identifier) {
        /* Two renderers sharing a model id used to queue it twice; the first
           uploaded GL model was then replaced by put() without close() and
           leaked its buffers on every reload. */
        if (!modelToLoad.contains(identifier)) modelToLoad.add(identifier);
    }

    public static OptimizedModelWrapper getModel(Identifier identifier) {
        return models.get(identifier);
    }

    public static void beginReload() {
        modelToLoad.forEach(id -> {
            OptimizedModelWrapper previous = models.get(id);
            if (previous != null) {
                ((OptimizedModelWrapperAccessor)(Object)previous).getOptimizedModel().close();
            }
            RawModelJS rawModelJS = ModelManagerJS.loadModel(id);
            ModelJS modelJS = ModelJS.uploadRawModel(rawModelJS);
            models.put(id, modelJS.getUnderlyingModel());
        });
        /* The request list survives reset() otherwise — stale ids were
           re-uploaded forever. beginReload consumed them. */
        modelToLoad.clear();
    }

    public static void reset() {
        models.values().forEach(e -> {
            ((OptimizedModelWrapperAccessor)(Object)e).getOptimizedModel().close();
        });
        models.clear();
        modelToLoad.clear();
    }
}
