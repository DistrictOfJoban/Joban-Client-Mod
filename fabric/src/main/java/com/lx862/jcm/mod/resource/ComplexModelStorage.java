package com.lx862.jcm.mod.resource;

import com.lx862.jcm.mixin.mtrscripting.OptimizedModelWrapperAccessor;
import com.lx862.mtrscripting.core.util.model.ModelJS;
import com.lx862.mtrscripting.core.util.model.ModelManagerJS;
import com.lx862.mtrscripting.core.util.model.RawModelJS;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.resource.*;

import java.util.*;

public class ComplexModelStorage {
    private static final Map<Identifier, OptimizedModelWrapper> models = new HashMap<>();
    private static final Set<Identifier> modelToLoad = new HashSet<>();

    /**
     * Only call once for each BER constructor.
     */
    public static void requireModel(Identifier identifier) {
        modelToLoad.add(identifier);
    }

    public static OptimizedModelWrapper getModel(Identifier identifier) {
        return models.get(identifier);
    }

    public static void beginReload() {
        modelToLoad.forEach(id -> {
            /* Yanyang: close the previous GL model before replacing it —
               put() alone leaked its buffers on every reload. */
            OptimizedModelWrapper previous = models.get(id);
            if (previous != null) {
                ((OptimizedModelWrapperAccessor)(Object)previous).getOptimizedModel().close();
            }
            RawModelJS rawModelJS = ModelManagerJS.loadModel(id);
            ModelJS modelJS = ModelJS.uploadRawModel(rawModelJS);
            models.put(id, modelJS.getUnderlyingModel());
        });
        /* Yanyang: the request list survives reset() otherwise — stale ids
           were re-uploaded forever. beginReload consumed them. */
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
