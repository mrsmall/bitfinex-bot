package com.klein.ta.funcs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by mresc on 23.02.16.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FunctionInput {
    /**
     * Description of the input field
     */
    String value() default "";

    /**
     * Is the field required or optional
     */
    boolean required() default true;
}
