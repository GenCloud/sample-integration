package org.genfork.integration.stream.converters.impl;

import lombok.extern.slf4j.Slf4j;
import org.genfork.integration.stream.converters.AbstractConverter;

import java.lang.reflect.ParameterizedType;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Slf4j
public class ClassConverter extends AbstractConverter<String, Class<?>> {
	@Override
	public Class<?> convert(ParameterizedType resultType, String value) {
		try {
			return Class.forName(value);
		} catch (IllegalArgumentException | ClassNotFoundException ex) {
			try {
				return Class.forName(value, false, ClassLoader.getSystemClassLoader());
			} catch (ClassNotFoundException e) {
				log.error("Conversation failed - cant find class {}", value);
				return null;
			}
		}
	}

	@Override
	public String reverseConvert(Class<String> resultType, Class<?> value) {
		return value.getName();
	}
}
