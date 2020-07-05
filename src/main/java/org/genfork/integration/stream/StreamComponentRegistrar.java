package org.genfork.integration.stream;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.model.response.ComponentOption;
import org.genfork.integration.model.response.StreamComponentInfo;
import org.genfork.integration.stream.annotation.StreamComponent;
import org.genfork.integration.stream.annotation.StreamComponent.ComponentType;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Slf4j
@UtilityClass
public class StreamComponentRegistrar {
	public static final String INTEGRATION_COMPONENT_TYPE = "integration-component";
	private static final Map<String, Class<AbstractDefinition<?>>> streamComponents;
	private static final Map<String, Map<ComponentType, StreamComponentInfo>> streamComponentInfos = new HashMap<>();

	static {
		final Reflections reflections = new Reflections("org.genfork.integration", new TypeAnnotationsScanner(), new SubTypesScanner());
		final Set<Class<?>> types = reflections.getTypesAnnotatedWith(StreamComponent.class);

		streamComponents = types
				.stream()
				.collect(Collectors.toMap(k -> k.getDeclaredAnnotation(StreamComponent.class).componentName(), v -> (Class<AbstractDefinition<?>>) v));

		streamComponents.forEach((name, clazz) -> {
			final StreamComponent streamComponent = clazz.getAnnotation(StreamComponent.class);
			final String componentName = streamComponent.componentName();
			final ComponentType[] componentTypes = streamComponent.componentType();

			Arrays.stream(componentTypes)
					.map(componentType -> {
						final List<ComponentOption> componentOptions = StreamComponentOptionsParser.getComponentOptions(clazz);
						return StreamComponentInfo.builder().componentName(componentName).componentType(componentType)
								.subComponent(streamComponent.subComponent())
								.options(componentOptions).build();
					})
					.forEach(info -> {
						if (streamComponentInfos.containsKey(name)) {
							streamComponentInfos.computeIfPresent(name, (key, map) -> {
								map.put(info.getComponentType(), info);
								return map;
							});
						} else {
							final Map<ComponentType, StreamComponentInfo> map = new EnumMap<>(ComponentType.class);
							map.put(info.getComponentType(), info);
							streamComponentInfos.put(name, map);
						}
					});
		});

		log.info("Loaded integration components: {}", streamComponentInfos.size());
	}

	public Map<String, Map<ComponentType, StreamComponentInfo>> getStreamComponentInfos() {
		return streamComponentInfos;
	}

	public StreamComponentInfo getStreamComponentInfo(String name, ComponentType componentType) {
		return streamComponentInfos.get(name).values()
				.stream()
				.filter(i -> i.getComponentType() == componentType)
				.findFirst().orElse(null);
	}

	public Class<AbstractDefinition<?>> getStreamComponent(String componentName) {
		return streamComponents.get(componentName);
	}
}
