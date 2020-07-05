package org.genfork.integration.stream.converters.impl;

import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.StreamComponentRegistrar;
import org.genfork.integration.stream.converters.AbstractConverter;
import org.genfork.integration.stream.converters.ConvertersRegistrar;
import org.genfork.integration.utils.TypeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static org.genfork.integration.stream.StreamComponentRegistrar.INTEGRATION_COMPONENT_TYPE;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@SuppressWarnings({"unchecked"})
public class MapConverter extends AbstractConverter<Map<Object, Object>, Map<Object, Object>> {
	@Override
	public Map<Object, Object> convert(ParameterizedType resultType, Map<Object, Object> map) {
		final Object object = map.values().stream().findFirst().orElse(null);
		if (object == null) {
			return Collections.emptyMap();
		}

		final Map<Object, Object> result = new LinkedHashMap<>();

		final Set<Object> keys = map.keySet();

		if (resultType.getActualTypeArguments()[1] instanceof ParameterizedType
				&& Map.class.isAssignableFrom((Class<?>) ((ParameterizedType) resultType.getActualTypeArguments()[1]).getRawType())) {
			keys.forEach(key -> {
				final Map<Object, Object> value = (Map<Object, Object>) map.get(key);
				final Map<Object, Object> converted = convert((ParameterizedType) resultType.getActualTypeArguments()[1], value);
				result.put(key, converted);
			});
		} else if (resultType.getActualTypeArguments()[1] instanceof ParameterizedType
				&& (Set.class.isAssignableFrom((Class<?>) ((ParameterizedType) resultType.getActualTypeArguments()[1]).getRawType())
				|| List.class.isAssignableFrom((Class<?>) ((ParameterizedType) resultType.getActualTypeArguments()[1]).getRawType()))) {
			final AbstractConverter<Object, Object> converter = ConvertersRegistrar.getConverter(ListSetConverter.class);
			keys.forEach(key -> {
				final Collection<Object> value = (Collection<Object>) map.get(key);
				final Object converted = converter.convert((ParameterizedType) resultType.getActualTypeArguments()[1], value);
				result.put(key, converted);
			});
		} else {
			final Class<?> ouActualTypeArgument = (Class<?>) resultType.getActualTypeArguments()[1];

			keys.forEach(key -> {
				final Object actualObject;

				final Object originObject = map.get(key);
				if (originObject instanceof Map) {
					final Map<Object, Object> value = (Map<Object, Object>) originObject;

					if (ouActualTypeArgument.isAssignableFrom(AbstractDefinition.class)) {
						final Object dslComponent = value.get(INTEGRATION_COMPONENT_TYPE);
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
						TypeUtils.findAndSetFieldsValues(value, actualObject.getClass(), actualObject);
					} else {
						TypeUtils.findAndSetFieldsValues(value, ouActualTypeArgument, actualObject);
					}

					result.put(key, actualObject);
				} else if (originObject instanceof String || originObject instanceof Number) {
					result.put(key, originObject);
				}
			});
		}

		return result;
	}
}
