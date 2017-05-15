package org.autotune;

import java.lang.annotation.*;

/**
 * Created by KevinRoj on 26.04.17.
 */
@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface TuneableParameters {
    int initRandomSearch() default 5;

    int cacheNextPoints() default 5;

    boolean autoTimeMeasure() default false;
}
