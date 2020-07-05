package org.genfork.integration.stream.converters;

import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.StreamComponentRegistrar;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import static org.genfork.integration.stream.StreamComponentRegistrar.INTEGRATION_COMPONENT_TYPE;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
public abstract class AbstractConverter<T, R> implements IConverter<T, R> {
	@Override
	@SuppressWarnings("unchecked")
	public R convert(Field currentField, T value) {
		final boolean isGeneric = currentField.getGenericType() instanceof ParameterizedType;
		final Type checkedType = isGeneric ? currentField.getGenericType() : currentField.getType();
		final R convertedValue;
		if (isGeneric) {
			convertedValue = convert((ParameterizedType) checkedType, value);
		} else {
			final Class<R> type = (Class<R>) checkedType;
			if (type.isAssignableFrom(AbstractDefinition.class) && value instanceof Map) {
				final Map<Object, Object> map = (Map<Object, Object>) value;
				final Object dslComponent = map.get(INTEGRATION_COMPONENT_TYPE);
				final Class<R> component = (Class<R>) StreamComponentRegistrar.getStreamComponent((String) dslComponent);
				convertedValue = convert(component, value);
			} else {
				convertedValue = convert(type, value);
			}
		}

		return convertedValue;
	}

	public R convert(Class<R> resultType, T value) {
		throw new UnsupportedOperationException();
	}

	public R convert(ParameterizedType resultType, T value) {
		throw new UnsupportedOperationException();
	}

	public T reverseConvert(Class<T> resultType, R value) {
		throw new UnsupportedOperationException();
	}
}
