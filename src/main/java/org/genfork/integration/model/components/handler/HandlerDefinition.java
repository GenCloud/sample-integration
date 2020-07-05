package org.genfork.integration.model.components.handler;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.annotation.StreamComponent;
import org.genfork.integration.stream.converters.impl.ObjectConverter;

/**
 * Integration component model (handler) for storing settings intended for initialization.
 *
 * @author: GenCloud
 * @date: 2020/07
 */
@SettingClass(scanSuper = false)
@StreamComponent(componentName = "handler", componentType = StreamComponent.ComponentType.processor)
@Data
@EqualsAndHashCode(callSuper = true)
public class HandlerDefinition extends AbstractDefinition<HandlerDefinition> {
	@SettingValue(originName = "handler-definition", description = "Встроенный компонент-обработчик", converter = ObjectConverter.class, required = true)
	private AbstractDefinition<?> handlerDefinition;

	@Override
	public boolean isHandlerDefinition() {
		return true;
	}
}
