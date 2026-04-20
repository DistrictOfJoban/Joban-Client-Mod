package com.lx862.mtrscripting.core.util;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import org.mtr.core.tool.Vector;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.holder.Vector3f;

public class ScriptVector3f {
    public static final ScriptVector3f ZERO = new ScriptVector3f(0, 0, 0);
    public static final ScriptVector3f XP = new ScriptVector3f(1, 0, 0);
    public static final ScriptVector3f YP = new ScriptVector3f(0, 1, 0);
    public static final ScriptVector3f ZP = new ScriptVector3f(0, 0, 1);

    private Vector3d impl;

    @ApiInternal
    public ScriptVector3f(Vector3d impl) {
        this.impl = impl;
    }

    public ScriptVector3f(float x, float y, float z) {
        this(new Vector3d(x, y, z));
    }

    public ScriptVector3f(Vector3f impl) {
        this(new Vector3d(impl));
    }

    public ScriptVector3f(Vector tscVector) {
        this(new Vector3d(tscVector.x(), tscVector.y(), tscVector.z()));
    }

    public ScriptVector3f(BlockPos blockPos) {
        this(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public float x() { return (float)impl.getXMapped(); }
    public float y() { return (float)impl.getYMapped(); }
    public float z() { return (float)impl.getZMapped(); }

    public ScriptVector3f copy() {
        Vector3d newImpl = new Vector3d(x(), y(), z());
        return new ScriptVector3f(newImpl);
    }

    public ScriptVector3f normalize() {
        impl = impl.normalize();
        return this;
    }

    public ScriptVector3f add(float x, float y, float z) {
        impl = impl.add(x, y, z);
        return this;
    }

    public ScriptVector3f add(ScriptVector3f other) {
        impl = impl.add(other.impl);
        return this;
    }

    public ScriptVector3f sub(ScriptVector3f other) {
        impl = impl.subtract(other.impl);
        return this;
    }

    public ScriptVector3f mul(float x, float y, float z) {
        impl = impl.multiply(x, y, z);
        return this;
    }

    public ScriptVector3f mul(float n) {
        impl = impl.multiply(n);
        return this;
    }

    public ScriptVector3f rotX(float rad) {
        impl = impl.rotateX(rad);
        return this;
    }

    public ScriptVector3f rotY(float rad) {
        impl = impl.rotateY(rad);
        return this;
    }

    public ScriptVector3f rotZ(float rad) {
        impl = impl.rotateZ(rad);
        return this;
    }

    public ScriptVector3f cross(ScriptVector3f other) {
        impl = impl.crossProduct(other.impl);
        return this;
    }

    public double distance(ScriptVector3f other) {
        double dx = x() - other.x();
        double dy = y() - other.y();
        double dz = z() - other.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double distanceSq(ScriptVector3f other) {
        double dx = x() - other.x();
        double dy = y() - other.y();
        double dz = z() - other.z();
        return (dx * dx + dy * dy + dz * dz);
    }

    /**
     * Compatibility for NTE, equivalent to {@link ScriptVector3f#rawBlockPos()}
     */
    @Deprecated
    public BlockPos toBlockPos() {
        return rawBlockPos();
    }

    public BlockPos rawBlockPos() {
        return new BlockPos((int)x(), (int)y(), (int)z());
    }

    public Vector3d rawVector3d() {
        return copy().impl;
    }

    public Vector3f rawVector3f() {
        return new Vector3f((float)x(), (float)y(), (float)z());
    }

    @Override
    public int hashCode() {
        return impl.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ScriptVector3f scriptVector3F = (ScriptVector3f) o;
        return impl.equals(scriptVector3F.impl);
    }

    @Override
    public String toString() {
        return "ScriptVector3f[" + "x=" + x() + ", y=" + y() + ", z=" + z() + "]";
    }
}
