package org.genfork.integration.model.components.inbound.http;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.annotation.StreamComponent;
import org.genfork.integration.stream.converters.impl.ListSetConverter;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

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
@StreamComponent(componentName = "cross-origin-definition", componentType = StreamComponent.ComponentType.other, subComponent = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class CrossOrigin extends AbstractDefinition<CrossOrigin> {
	@SettingValue(originName = "cross-http-method",
			description = "Устанавливает проверяемый метод запроса (по умолчанию - 'GET')",
			innerObject = true,
			converter = ListSetConverter.class)
	private List<RequestMethod> httpMethods;

	@SettingValue(originName = "cross-allow-credentials",
			description = "Включает учетные данные передаваемые клиентом")
	private boolean allowCredentials;

	@SettingValue(originName = "cross-max-age",
			description = "Таймаут кеша pre-flight запросов, значение задается в секундах(по умолчанию - 30 минут)")
	private int maxAge;

	@SettingValue(originName = "cross-origin",
			description = "Список разрешенных источников. «*» означает, что все источники разрешены. Эти значения помещаются в заголовок «Access-Control-Allow-Origin» как предварительных, так и фактических ответов (по умолчанию - '*')",
			innerObject = true,
			converter = ListSetConverter.class)
	private List<String> origin;

	@SettingValue(originName = "cross-allowed-headers",
			description = "Указывает, какие заголовки запроса могут быть использованы во время фактического запроса (по умолчанию - '*')",
			innerObject = true,
			converter = ListSetConverter.class)
	private List<String> allowedHeaders;

	@SettingValue(originName = "cross-exposed-headers",
			description = "Указывает, какие заголовки открыты для клиента",
			innerObject = true,
			converter = ListSetConverter.class)
	private List<String> exposedHeaders;
}
