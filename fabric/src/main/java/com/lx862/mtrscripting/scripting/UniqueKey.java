package com.lx862.mtrscripting.scripting;

import java.util.Arrays;
import java.util.Objects;

/* A key that can uniquely identify a set of objects. */
public class UniqueKey {
    private final Object[] objs;

    public UniqueKey(Object... objs) {
        this.objs = objs;
    }

    @Override
    public boolean equals(Object other) {
        if(other == this) return true;
        if(other instanceof UniqueKey) {
            UniqueKey otherKey = (UniqueKey) other;
            if(otherKey.objs.length != objs.length) return false;

            for(int i = 0; i < objs.length; i++) {
                if(!objs[i].equals(otherKey.objs[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(objs);
    }

    @Override
    public String toString() {
        return String.join(", ", Arrays.stream(objs).map(Object::toString).toList());
    }
}
