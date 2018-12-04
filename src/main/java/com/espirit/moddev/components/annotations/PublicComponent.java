package com.espirit.moddev.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PublicComponent {
    String name();
    String displayName() default "";
}