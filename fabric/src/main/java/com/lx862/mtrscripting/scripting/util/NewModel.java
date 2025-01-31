package com.lx862.mtrscripting.scripting.util;

import org.mtr.mapping.holder.Identifier;
import org.jetbrains.annotations.Nullable;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.render.DynamicVehicleModel;
import org.mtr.mod.resource.OptimizedModelWrapper;
import org.mtr.mod.resource.StoredModelResourceBase;

public class NewModel implements StoredModelResourceBase {
    private final ObjectObjectImmutablePair<OptimizedModelWrapper, DynamicVehicleModel> models;

    public NewModel(Identifier location, boolean flipV) {
        this.models = load(location.getNamespace() + ":" + location.getPath(), "", flipV, 0, ResourceManagerHelper::readResource);
    }

    @Nullable
    @Override
    public OptimizedModelWrapper getOptimizedModel() {
        return models.left();
    }

    @Nullable
    @Override
    public DynamicVehicleModel getDynamicVehicleModel() {
        return models.right();
    }
}
