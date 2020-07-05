package org.genfork.integration.model.components;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.enums.HttpAdapterType;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.converters.impl.ListSetConverter;

import java.util.List;
import java.util.Properties;

/**
 * An abstract model of http integration component with basic parameters (required), http adapter type
 * and auxiliary/common functions used to obtain component settings {@link Properties}.
 *
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractHttpHandlerDefinition<T extends AbstractDefinition<T>> extends AbstractDefinition<T> {
	@SettingValue(originName = "http-adapter-type", required = true)
	private HttpAdapterType httpAdapterType;

	@SettingValue(originName = "http-inbound-supported-methods",
			description = "Поддерживаемые типы запроса",
			required = true,
			innerObject = true,
			converter = ListSetConverter.class)
	private List<String> supportedHttpMethods;

	@SettingValue(originName = "request-channel",
			description = "Наименование диспетчера входящего канала",
			required = true)
	private String requestChannel;

	@SettingValue(originName = "reply-channel",
			description = "Наименование диспетчера исходящего канала",
			required = true)
	private String replyChannel;

	@SettingValue(originName = "message-converters",
			description = "Имена бинов, конверторов сообщений через запятую",
			innerObject = true,
			converter = ListSetConverter.class)
	private List<String> messageConverters;

	@SettingValue(originName = "http-request-timeout",
			description = "Тайм-аут запроса")
	private long requestTimeout = -1L;

	@SettingValue(originName = "http-response-timeout",
			description = "Тайм-аут ответа")
	private long responseTimeout = -1L;

	@Override
	public boolean isHttpDefinition() {
		return true;
	}
}
