package org.genfork.integration.stream.annotation;

import java.lang.annotation.*;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SettingClass {
	boolean scanSuper() default true;

	String[] excludedProperties() default {"id", "order"};
}
