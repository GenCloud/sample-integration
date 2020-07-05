package org.genfork.integration.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.genfork.integration.exceptions.StreamException;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.converters.AbstractConverter;
import org.genfork.integration.stream.converters.ConvertersRegistrar;
import org.genfork.integration.stream.converters.IConverter;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@UtilityClass
@Slf4j
public class TypeUtils {
	@SuppressWarnings("unchecked")
	public <O> O instantiateObject(Class<O> type) {
		try {
			return (O) type.getConstructors()[0].newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new StreamException("Can't initialize class: " + type, e);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void findAndSetFieldsValues(Map<Object, Object> props, Class<?> actualTypeArgument, Object actualObject) {
		final SettingClass settingClass = actualTypeArgument.getAnnotation(SettingClass.class);
		final Set<Field> allFields = settingClass != null && settingClass.scanSuper() ?
				ReflectionUtils.getAllFields(actualTypeArgument, f -> f != null && f.isAnnotationPresent(SettingValue.class))
				: Arrays.stream(actualTypeArgument.getDeclaredFields()).filter(f -> f != null && f.isAnnotationPresent(SettingValue.class)).collect(Collectors.toSet());

		final String[] excludedProperties = settingClass != null ? settingClass.excludedProperties() : new String[0];
		final Set<Object> keys = props.keySet();
		keys.stream()
				.filter(key ->
						!ArrayUtils.contains(excludedProperties, key))
				.forEach(key -> {
					final Object value = props.get(key);
					final Field field = allFields
							.stream()
							.filter(f ->
									StringUtils.isEmpty(f.getAnnotation(SettingValue.class).originName())
											? f.getName().equals(key)
											: f.getAnnotation(SettingValue.class).originName().equals(key))
							.findFirst()
							.orElse(null);
					if (field != null) {
						final SettingValue annotation = field.getAnnotation(SettingValue.class);
						final Class<? extends AbstractConverter> type = annotation.converter();
						if (type != AbstractConverter.class) {
							final IConverter<Object, Object> converter = ConvertersRegistrar.getConverter(type);
							try {
								final Object convertedValue = converter.convert(field, value);
								setFiledValue(actualObject, convertedValue, field);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							setFiledValue(actualObject, value, field);
						}
					}
				});
	}

	private void setFiledValue(Object actualObject, Object value, Field field) {
		final Class<?> fieldType = field.getType();
		final Class<?> valueType = value.getClass();
		final boolean primitiveCheck = isPrimitive(fieldType, valueType);

		if (fieldType != valueType && (fieldType.isPrimitive() && !primitiveCheck)) {
			log.error("Field type does not equal prop argument. Assign class: {}, required type: {}, actual type: {}",
					actualObject.getClass().getSimpleName(), fieldType.getSimpleName(), value.getClass().getSimpleName());
			return;
		}

		final boolean accessible = field.isAccessible();
		if (!accessible) {
			field.setAccessible(true);
		}

		try {
			if (int.class.equals(fieldType) && Integer.class.equals(valueType)) {
				field.setInt(actualObject, (Integer) value);
			} else if (long.class.equals(fieldType) && Long.class.equals(valueType)) {
				field.setLong(actualObject, (Long) value);
			} else if (char.class.equals(fieldType) && Character.class.equals(valueType)) {
				field.setChar(actualObject, (Character) value);
			} else if (short.class.equals(fieldType) && Short.class.equals(valueType)) {
				field.setShort(actualObject, (Short) value);
			} else if (boolean.class.equals(fieldType) && Boolean.class.equals(valueType)) {
				field.setBoolean(actualObject, (Boolean) value);
			} else if (byte.class.equals(fieldType) && Byte.class.equals(valueType)) {
				field.setByte(actualObject, (Byte) value);
			} else {
				field.set(actualObject, value);
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		if (!accessible) {
			field.setAccessible(false);
		}
	}

	public Object getFieldValue(Field field, Object object) {
		final boolean accessible = field.isAccessible();
		if (!accessible) {
			field.setAccessible(true);
		}

		Object value = null;
		try {
			value = field.get(object);
		} catch (IllegalAccessException e) {
			log.error("Error accessing object field value. Type: {}, field: {}", object.getClass().getSimpleName(), field.getName(), e);
		}

		if (!accessible) {
			field.setAccessible(false);
		}

		return value;
	}

	public boolean isPrimitive(Class<?> fieldType, Class<?> valueType) {
		return (int.class.equals(fieldType) && Integer.class.equals(valueType))
				|| (long.class.equals(fieldType) && Long.class.equals(valueType))
				|| (char.class.equals(fieldType) && Character.class.equals(valueType))
				|| (short.class.equals(fieldType) && Short.class.equals(valueType))
				|| (boolean.class.equals(fieldType) && Boolean.class.equals(valueType))
				|| (byte.class.equals(fieldType) && Byte.class.equals(valueType));
	}
}
