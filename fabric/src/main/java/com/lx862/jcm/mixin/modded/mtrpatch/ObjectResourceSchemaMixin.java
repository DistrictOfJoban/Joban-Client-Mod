package com.lx862.jcm.mixin.modded.mtrpatch;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.mtr.core.serializer.ReaderBase;
import org.mtr.core.serializer.WriterBase;
import org.mtr.legacy.generated.resource.ObjectResourceSchema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ObjectResourceSchema.class, remap = false)
public class ObjectResourceSchemaMixin {
    @Unique
    private DoubleArrayList jsblock$translationArray = null;
    @Unique
    private DoubleArrayList jsblock$rotationArray = null;
    @Unique
    private DoubleArrayList jsblock$scaleArray = null;
    @Unique
    private BooleanArrayList jsblock$mirrorArray = null;

    @Inject(method = "updateData", at = @At("TAIL"))
    private void captureNTEProperties(ReaderBase readerBase, CallbackInfo ci) {
        jsblock$translationArray = new DoubleArrayList();
        jsblock$rotationArray = new DoubleArrayList();
        jsblock$scaleArray = new DoubleArrayList();
        jsblock$mirrorArray = new BooleanArrayList();

        int[] axis = {0};
        readerBase.iterateDoubleArray("translation", () -> {}, (d) -> {
            // MTR 4 model rotated by 180 degrees
            // x => x
            // y => -y
            // z => -z
            if(axis[0] != 0) jsblock$translationArray.add(-d);
            else jsblock$translationArray.add(d);
            axis[0]++;
        });

        axis[0] = 0;
        readerBase.iterateDoubleArray("rotation", () -> {}, (d) -> {
            // MTR 4 model rotated by 180 degrees
            // x => x
            // y => -y
            // z => -z
            if(axis[0] != 0) jsblock$rotationArray.add(-d);
            else jsblock$rotationArray.add(d);
            axis[0]++;
        });
        readerBase.iterateDoubleArray("scale", () -> {}, (d) -> {
            jsblock$scaleArray.add(d);
        });
        readerBase.iterateBooleanArray("mirror", () -> {}, (bl) -> {
            jsblock$mirrorArray.add(bl);
        });
    }

    @Inject(method = "serializeData", at = @At("TAIL"))
    private void serializeExtraData(WriterBase writerBase, CallbackInfo ci) {
        if(jsblock$translationArray != null) {
            WriterBase.Array arrWriter = writerBase.writeArray("translation");
            for(double d : jsblock$translationArray) {
                arrWriter.writeDouble(d);
            }
        }
        if(jsblock$rotationArray != null) {
            WriterBase.Array arrWriter = writerBase.writeArray("rotation");
            for(double d : jsblock$rotationArray) {
                arrWriter.writeDouble(d);
            }
        }
        if(jsblock$scaleArray != null) {
            WriterBase.Array arrWriter = writerBase.writeArray("scale");
            for(double d : jsblock$scaleArray) {
                arrWriter.writeDouble(d);
            }
        }
        if(jsblock$mirrorArray != null) {
            WriterBase.Array arrWriter = writerBase.writeArray("mirror");
            for(boolean bl : jsblock$mirrorArray) {
                arrWriter.writeBoolean(bl);
            }
        }
    }
}
