package org.genfork.integration.model.components.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.annotation.StreamComponent;

import static org.genfork.integration.stream.annotation.StreamComponent.ComponentType.other;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@SettingClass(excludedProperties = {
		"id",
		"order",
		"log-stages",
		"log-level"
})
@StreamComponent(componentName = "property-definition", componentType = other, subComponent = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class PropertyDefinition extends AbstractDefinition<PropertyDefinition> {
	@SettingValue(originName = "property-name", description = "Название параметра")
	private String propertyName;

	@SettingValue(originName = "property-bean", description = "Значение параметра является компонентом")
	private boolean namedBean;

	@SettingValue(originName = "property-value", description = "Значение параметра")
	private Object value;

	public PropertyDefinition() {
	}

	private PropertyDefinition(Object value, boolean namedBean) {
		this.namedBean = namedBean;
		this.value = value;
	}

	public static PropertyDefinition of(Object value, boolean namedBean) {
		return new PropertyDefinition(value, namedBean);
	}
}
