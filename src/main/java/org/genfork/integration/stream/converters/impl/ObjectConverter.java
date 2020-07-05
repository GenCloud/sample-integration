package org.genfork.integration.stream.converters.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.genfork.integration.exceptions.StreamException;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.StreamComponentRegistrar;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.converters.AbstractConverter;
import org.genfork.integration.utils.TypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Slf4j
public class ObjectConverter extends AbstractConverter<Map<Object, Object>, Object> {
	private static final String COMPONENT_NAME_PARAM = "componentName";
	private static final String COMPONENT_PARAMETERS_PARAM = "componentParameters";

	@Override
	public Object convert(Class<Object> resultType, Map<Object, Object> props) {
		final Object actualObject;
		try {
			actualObject = TypeUtils.instantiateObject(resultType);
		} catch (Exception e) {
			throw new StreamException("Can't convert values to object [" + resultType.getSimpleName() + "]", e);
		}

		TypeUtils.findAndSetFieldsValues(props, resultType, actualObject);
		return actualObject;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object convert(ParameterizedType resultType, Map<Object, Object> props) {
		if (resultType.getRawType() == AbstractDefinition.class) {
			final String componentName = (String) props.get(COMPONENT_NAME_PARAM);
			final Class<AbstractDefinition<?>> streamComponentType = StreamComponentRegistrar.getStreamComponent(componentName);
			if (streamComponentType == null) {
				throw new StreamException("Can't find stream component with the same name: " + componentName);
			}

			final Map<Object, Object> componentParameters = (Map<Object, Object>) props.get(COMPONENT_PARAMETERS_PARAM);
			final Object actualObject = TypeUtils.instantiateObject(streamComponentType);
			TypeUtils.findAndSetFieldsValues(componentParameters, actualObject.getClass(), actualObject);
			return actualObject;
		}

		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Object, Object> reverseConvert(Class<Map<Object, Object>> resultType, Object value) {
		final Set<Field> fields = Arrays
				.stream(value.getClass().getDeclaredFields())
				.filter(f ->
						f != null && f.isAnnotationPresent(SettingValue.class))
				.collect(Collectors.toSet());

		final Map<Object, Object> map = new LinkedHashMap<>();
		fields.forEach(f -> {
			final Object fieldValue = TypeUtils.getFieldValue(f, value);
			final SettingValue settingValue = f.getAnnotation(SettingValue.class);
			final String key = StringUtils.isEmpty(settingValue.originName()) ? f.getName() : settingValue.originName();
			map.put(key, fieldValue);
		});

		return map;
	}
}
