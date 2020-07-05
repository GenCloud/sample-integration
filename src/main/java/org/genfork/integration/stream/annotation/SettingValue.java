package org.genfork.integration.stream.annotation;

import org.genfork.integration.stream.converters.AbstractConverter;

import java.lang.annotation.*;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SettingValue {
	String originName() default "";

	String description() default "";

	Class<? extends AbstractConverter> converter() default AbstractConverter.class;

	String[] prefixes() default {};

	boolean innerObject() default false;

	boolean required() default false;
}
