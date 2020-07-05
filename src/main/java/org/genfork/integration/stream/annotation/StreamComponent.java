package org.genfork.integration.stream.annotation;

import java.lang.annotation.*;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StreamComponent {
	String componentName();

	ComponentType[] componentType();

	boolean subComponent() default false;

	String description() default "#empty";

	enum ComponentType {
		source,
		app,
		sink,
		processor,
		other
	}
}
