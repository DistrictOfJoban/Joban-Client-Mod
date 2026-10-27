package com.lx862.mtrscripting.core.util;

import java.lang.reflect.Array;
import java.util.List;

public class JavaUtilJS {
    /**
     * Create a native java array
     * @param classType The class type
     * @param length The total length of the array
     * @return The resulting array
     */
    public static Object newArray(Class<?> classType, int length) {
        return java.lang.reflect.Array.newInstance(classType, length);
    }

    /**
     * Creates a native java multi-dimension array
     * @param classType The array type
     * @param dimensionLength The dimensional length.<br>For example [2, 7] would create an array with length of 2, each element containing an array of length 7.
     * @return The resulting array
     */
    public static Object newArray(Class<?> classType, int[] dimensionLength) {
        return java.lang.reflect.Array.newInstance(classType, dimensionLength);
    }

    /**
     * Create a native java array from a list.
     * @param list The Java List
     * @param classType The class type of the array
     * @return The resulting array
     */
    public static <T> T[] toArray(Class<T> classType, List<T> list) {
        final T[] array = (T[]) Array.newInstance(classType, list.size());
        return list.toArray(array);
    }

    /**
     * ANTE Overload for {@link JavaUtilJS#toArray(Class, List)}
     */
    @Deprecated
    public static <T> T[] toArray(List<T> list, Class<T> classType) {
        return toArray(classType, list);
    }
}
