package org.genfork.integration.stream.converters;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@UtilityClass
@Slf4j
public class ConvertersRegistrar {
	private final Map<Class<? extends AbstractConverter<Object, Object>>, AbstractConverter<Object, Object>> converters;

	static {
		final Reflections reflections = new Reflections("org.genfork.integration.stream.converters.impl", new SubTypesScanner());

		converters = reflections.getSubTypesOf(AbstractConverter.class)
				.stream()
				.collect(Collectors.toMap(k -> (Class<? extends AbstractConverter<Object, Object>>) k, v -> {
					try {
						return (AbstractConverter<Object, Object>) v.getConstructors()[0].newInstance();
					} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
						e.printStackTrace();
						return new AbstractConverter<Object, Object>() {
							@Override
							public Object convert(Field currentField, Object value) {
								throw new RuntimeException("Converter does not correctly initialized. Check - " + v.getSimpleName());
							}
						};
					}
				}));
	}

	@SuppressWarnings("rawtypes")
	public AbstractConverter<Object, Object> getConverter(Class<? extends AbstractConverter> type) {
		return converters.get(type);
	}
}
