package org.genfork.integration.stream.converters.impl;

import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.StreamComponentRegistrar;
import org.genfork.integration.stream.converters.AbstractConverter;
import org.genfork.integration.stream.converters.ConvertersRegistrar;
import org.genfork.integration.utils.TypeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static org.genfork.integration.stream.StreamComponentRegistrar.INTEGRATION_COMPONENT_TYPE;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@SuppressWarnings("unchecked")
public class ListSetConverter extends AbstractConverter<Collection<Object>, Collection<Object>> {
	@Override
	public Collection<Object> convert(ParameterizedType resultType, Collection<Object> list) {
		;
		if (list.isEmpty()) {
			return Collections.emptyList();
		}

		final Collection<Object> result = resultType.getRawType() == Set.class ? new HashSet<>() : new ArrayList<>();

		final Type actualTypeArgument = resultType.getActualTypeArguments()[0];
		if (actualTypeArgument instanceof ParameterizedType) {
			if (Map.class.isAssignableFrom((Class<?>) ((ParameterizedType) actualTypeArgument).getRawType())) {
				list.forEach(val -> {
					final AbstractConverter<Object, Object> converter = ConvertersRegistrar.getConverter(MapConverter.class);
					final Object converted = converter.convert((ParameterizedType) actualTypeArgument, val);
					result.add(converted);
				});
			} else if (Set.class.isAssignableFrom((Class<?>) ((ParameterizedType) actualTypeArgument).getRawType())
					|| List.class.isAssignableFrom((Class<?>) ((ParameterizedType) actualTypeArgument).getRawType())) {
				list.forEach(val -> {
					final Object converted = convert((ParameterizedType) actualTypeArgument, (Collection<Object>) val);
					result.add(converted);
				});
			}
		} else {
			final Class<?> ouActualTypeArgument = (Class<?>) actualTypeArgument;

			list.forEach(val -> {
				if (val instanceof Map) {
					final Object actualObject;

					if (ouActualTypeArgument.isAssignableFrom(AbstractDefinition.class)) {
						final Map<Object, Object> map = (Map<Object, Object>) val;
						final Object dslComponent = map.get(INTEGRATION_COMPONENT_TYPE);
						final Class<?> component = StreamComponentRegistrar.getStreamComponent((String) dslComponent);

						try {
							actualObject = component.getConstructors()[0].newInstance();
						} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
							throw new RuntimeException("Can't convert values to object [" + component.getSimpleName() + "]", e);
						}
					} else {
						try {
							actualObject = ouActualTypeArgument.getConstructors()[0].newInstance();
						} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
							throw new RuntimeException("Can't convert values to object [" + ouActualTypeArgument.getSimpleName() + "]", e);
						}
					}

					if (ouActualTypeArgument != actualObject.getClass()) {
						TypeUtils.findAndSetFieldsValues((Map<Object, Object>) val, actualObject.getClass(), actualObject);
					} else {
						TypeUtils.findAndSetFieldsValues((Map<Object, Object>) val, ouActualTypeArgument, actualObject);
					}

					result.add(actualObject);
				} else if (val instanceof String || val instanceof Number) {
					result.add(val);
				}
			});
		}

		return result;
	}
}
