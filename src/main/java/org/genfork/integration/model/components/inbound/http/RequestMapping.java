package org.genfork.integration.model.components.inbound.http;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.annotation.StreamComponent;

import java.util.List;

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
@StreamComponent(componentName = "request-mapping-definition", componentType = other, subComponent = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class RequestMapping extends AbstractDefinition<RequestMapping> {
	@SettingValue(originName = "request-mapping-params",
			description = "Параметры запроса, служащие для сопоставления. Последовательность определяется как \"myParam=myValue\" выражений. Параметры могут быть отменены операторами \"!=\", как \"myParam!=myValue\", также разрешена запись параметра \"myParam\", который должен присутствовать в запросе. Выражение вида \"!myParam\" показывает, что указанный параметр запрещен в запросе",
			innerObject = true)
	private List<String> requestMappingParams;

	@SettingValue(originName = "request-mapping-consumes",
			description = "Ожидаемый тип заголовка контента. Задается как експрессия по аналогии с params",
			innerObject = true)
	private List<String> requestMappingConsumes;

	@SettingValue(originName = "request-mapping-produces",
			description = "Возвращаемый тип заголовка контента. Задается как експрессия по аналогии с params",
			innerObject = true)
	private List<String> requestMappingProduces;
}
