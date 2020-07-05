package org.genfork.integration.stream.converters;

import java.lang.reflect.Field;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
public interface IConverter<T, R> {
	R convert(Field currentField, T value);
}
