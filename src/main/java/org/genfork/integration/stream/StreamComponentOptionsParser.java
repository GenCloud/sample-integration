package org.genfork.integration.stream;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ArrayUtils;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.model.response.ComponentOption;
import org.genfork.integration.model.response.StreamComponentInfo;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.annotation.StreamComponent;
import org.reflections.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@UtilityClass
public class StreamComponentOptionsParser {
	@SuppressWarnings("unchecked")
	public LinkedList<StreamComponentInfo> getComponentsFromInstances(LinkedList<AbstractDefinition<?>> definitions) {
		return definitions.stream().map(def -> {
			final Class<AbstractDefinition<?>> clazz = (Class<AbstractDefinition<?>>) def.getClass();
			final StreamComponent streamComponent = clazz.getAnnotation(StreamComponent.class);
			final String componentName = streamComponent.componentName();

			final List<ComponentOption> componentOptions = StreamComponentOptionsParser.getComponentOptions(clazz);
			return StreamComponentInfo.builder()
					.componentName(componentName)
					.subComponent(streamComponent.subComponent())
					.options(componentOptions).build();
		})
				.collect(Collectors.toCollection(LinkedList::new));

	}

	@SuppressWarnings("unchecked")
	public List<ComponentOption> getComponentOptions(Class<? extends AbstractDefinition<?>> type) {
		final SettingClass settingClass = type.getAnnotation(SettingClass.class);
		final StreamComponent streamComponent = type.getAnnotation(StreamComponent.class);
		final String[] excludedProperties = settingClass != null ? settingClass.excludedProperties() : new String[0];
		final Set<Field> allFields = settingClass != null && settingClass.scanSuper() ?
				ReflectionUtils.getAllFields(type, f -> f != null && f.isAnnotationPresent(SettingValue.class))
				: Arrays.stream(type.getDeclaredFields()).filter(f -> f != null && f.isAnnotationPresent(SettingValue.class))
				.collect(Collectors.toSet());

		return allFields
				.stream()
				.filter(f ->
						!ArrayUtils.contains(excludedProperties, StringUtils.hasText(f.getDeclaredAnnotation(SettingValue.class).originName())
								? f.getDeclaredAnnotation(SettingValue.class).originName() : f.getName()))
				.map(f -> {
					final SettingValue settingValue = f.getDeclaredAnnotation(SettingValue.class);
					final String paramName = StringUtils.hasText(settingValue.originName()) ? settingValue.originName() : f.getName();

					final ComponentOption option = new ComponentOption();
					option.setId(streamComponent != null ? streamComponent.componentName() + "." + paramName : paramName);
					option.setName(paramName);
					option.setDeprecated(f.isAnnotationPresent(Deprecated.class));
					option.setDescription(settingValue.description());
					option.setShortDescription(settingValue.description());

					final Type fieldType = f.getGenericType();

					tryDefineInnerTypes(option, fieldType);

					final String rawName = fieldType instanceof ParameterizedType ? ((ParameterizedType) fieldType).getRawType().getTypeName() : fieldType.getTypeName();
					option.setType(rawName);
					option.setSourceType(type.getName());
					return option;
				})
				.collect(Collectors.toList());
	}

	private void tryDefineInnerTypes(ComponentOption option, Type type) {
		if (type instanceof ParameterizedType) {
			final ParameterizedType parameterizedType = (ParameterizedType) type;
			final Type rawType = parameterizedType.getRawType();
			if (rawType == List.class || rawType == Set.class) {
				final Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
				tryDefineParameterizedObjects(option, actualTypeArgument);
			} else if (rawType == Map.class) {
				final Type actualTypeArgument = parameterizedType.getActualTypeArguments()[1];
				tryDefineParameterizedObjects(option, actualTypeArgument);
			}
		} else {
			defineSimpleObject(option, (Class<?>) type);
		}
	}

	@SuppressWarnings("unchecked")
	private void defineSimpleObject(ComponentOption option, Class<?> type) {
		if (type.isAnnotationPresent(SettingClass.class) && type.isAnnotationPresent(StreamComponent.class)) {
			final StreamComponent streamComponent = type.getDeclaredAnnotation(StreamComponent.class);
			final StreamComponentInfo innerObject = StreamComponentInfo.builder()
					.componentName(streamComponent.componentName())
					.componentType(streamComponent.componentType()[0])
					.build();

			final List<ComponentOption> componentOptions = getComponentOptions((Class<AbstractDefinition<?>>) type);
			innerObject.setOptions(componentOptions);

			option.setInner(true);
			option.setInnerObject(innerObject);
		}
	}

	private void tryDefineParameterizedObjects(ComponentOption option, Type actualTypeArgument) {
		if (actualTypeArgument instanceof ParameterizedType) {
			final Class<?> innerRawType = (Class<?>) ((ParameterizedType) actualTypeArgument).getRawType();
			if (Map.class.isAssignableFrom(innerRawType)
					|| Set.class.isAssignableFrom(innerRawType)
					|| List.class.isAssignableFrom(innerRawType)) {
				final StreamComponentInfo innerObject = StreamComponentInfo.builder()
						.componentName(innerRawType.getSimpleName().toLowerCase())
						.build();

				final ComponentOption innerOption = new ComponentOption();

				innerOption.setId(option.getId() + "." + innerRawType.getSimpleName().toLowerCase());
				innerOption.setName(innerRawType.getSimpleName().toLowerCase());
				innerOption.setSourceType(innerRawType.getName());

				tryDefineInnerTypes(innerOption, actualTypeArgument);

				innerObject.setOptions(Collections.singletonList(innerOption));

				option.setInner(true);
				option.setInnerObject(innerObject);
			}
		} else {
			defineSimpleObject(option, (Class<?>) actualTypeArgument);
		}
	}
}
