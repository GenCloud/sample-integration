package org.genfork.integration.model.components.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.annotation.StreamComponent;
import org.genfork.integration.stream.converters.impl.ClassConverter;
import org.genfork.integration.stream.converters.impl.ListSetConverter;

import java.util.List;

import static org.genfork.integration.stream.annotation.StreamComponent.ComponentType.other;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@SettingClass(excludedProperties = {})
@StreamComponent(componentName = "bean", componentType = other)
@Data
@EqualsAndHashCode(callSuper = true)
public class BeanDefinition extends AbstractDefinition<BeanDefinition> {
	@SettingValue(originName = "bean-type", description = "Класс-тип бина доступного в приложении", converter = ClassConverter.class, required = true)
	private Class<?> beanType;

	@SettingValue(originName = "init-method", description = "Метод инцициализации компонента")
	private String initMethod;

	@SettingValue(originName = "constructor-args",
			description = "Аргументы конструктора бина",
			innerObject = true,
			converter = ListSetConverter.class)
	private List<PropertyDefinition> constructorArgs;

	@SettingValue(originName = "property-args",
			description = "Аргументы полей и методов бина",
			innerObject = true,
			converter = ListSetConverter.class)
	private List<PropertyDefinition> propertyArgs;

	@Override
	public boolean isBeanDefinition() {
		return true;
	}
}
