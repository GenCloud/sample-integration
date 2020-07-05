package org.genfork.integration.model.components.inbound.http;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.components.AbstractHttpHandlerDefinition;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.converters.impl.ClassConverter;
import org.genfork.integration.stream.converters.impl.ListSetConverter;
import org.genfork.integration.stream.converters.impl.ObjectConverter;

import java.util.List;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractHttpInboundDefinition<T extends AbstractHttpInboundDefinition<T>> extends AbstractHttpHandlerDefinition<T> {
	@SettingValue(originName = "http-inbound-path",
			description = "Путь по которому должны приходить запросы",
			required = true)
	private String localPath;

	@SettingValue(originName = "http-inbound-payload-expression",
			description = "Экспрессия (строковое выражения) манипуляции с приходящим телом сообщения. Доступные параметры для вызова #pathVariables, #requestParams")
	private String payloadExpression;

	@SettingValue(originName = "http-inbound-mapped-request-headers",
			description = "Разделенный запятыми список заголовков разрешенных в HTTP-запросе",
			innerObject = true,
			converter = ListSetConverter.class)
	private List<String> mappedRequestHeaders;

	@SettingValue(originName = "payload-type",
			description = "Тип запрашиваемого сообщения в запросе",
			required = true,
			converter = ClassConverter.class)
	private Class<?> payloadType;

	@SettingValue(originName = "merge-with-default-converters",
			description = "Флаг, отвечающий за включение стандарных конверторов. По-умолчанию, false")
	private boolean mergeWithDefaultConverters;

	@SettingValue(originName = "cross-origin",
			description = "Блок конфигурации CORS для проверки фактического источника запроса, HTTP-методов и заголовков",
			converter = ObjectConverter.class)
	private CrossOrigin crossOrigin;

	@SettingValue(originName = "request-mapping",
			description = "Определяет атрибуты RESTful конфигурации для входящих эндпоинтов",
			converter = ObjectConverter.class)
	private RequestMapping requestMapping;
}
