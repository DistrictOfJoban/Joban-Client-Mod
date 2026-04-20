package com.lx862.mtrscripting.core.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.PARAMETER, ElementType.TYPE_USE})
/**
 * This parameter is designed to be nullable.
 */
public @interface ValueNullable {
}
