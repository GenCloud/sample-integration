package org.genfork.integration.stream.converters.impl;

import org.genfork.integration.stream.converters.AbstractConverter;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumConverter extends AbstractConverter<String, Enum> {
	@Override
	public Enum convert(Class<Enum> resultType, String value) {
		try {
			return (Enum<?>) Enum.valueOf(resultType, value);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	@Override
	public String reverseConvert(Class<String> resultType, Enum value) {
		return value.name();
	}
}
