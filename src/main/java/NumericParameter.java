import java.lang.annotation.*;

/**
 * Created by KevinRoj on 26.04.17.
 */
@Target(ElementType.FIELD)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface NumericParameter {
    double min() default  0;
    double max() default Float.MAX_VALUE;
}
