package com.lx862.mtrscripting.core.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Target({ElementType.TYPE,ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
/**
 * Internal Java Scripting API, not designed for use by scripts.
 */
public @interface ApiInternal {
}
