package org.genfork.integration.model.components;

import lombok.Data;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.converters.impl.EnumConverter;
import org.genfork.integration.utils.TypeUtils;
import org.springframework.integration.handler.LoggingHandler.Level;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * An abstract model of an integration component with basic parameters (required)
 * and auxiliary/common functions used to obtain component settings.
 *
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
public abstract class AbstractDefinition<T extends AbstractDefinition<T>> implements Serializable {
	@SettingValue(description = "Идентификатор компонента")
	private String id = UUID.randomUUID().toString();

	@SettingValue(originName = "log-stages", description = "Логирование действий компонента")
	private boolean logStages = false;

	@SettingValue(originName = "log-level", description = "Уровень логирования", converter = EnumConverter.class)
	private Level logLevel = Level.INFO;

	@SettingValue(description = "Ордер (порядок) инициализации компонента в потоке")
	private int order;

	public void pushProps(Map<Object, Object> props) {
		TypeUtils.findAndSetFieldsValues(props, getClass(), this);
	}

	public boolean isInbound() {
		return false;
	}

	public boolean isOutbound() {
		return false;
	}

	public boolean isBeanDefinition() {
		return false;
	}

	public boolean isChannelDefinition() {
		return false;
	}

	public boolean isHttpDefinition() {
		return false;
	}

	public boolean isHandlerDefinition() {
		return false;
	}

	public boolean isTransformerDefinition() {
		return false;
	}

	public boolean isResultDefinition() {
		return false;
	}

	public boolean isJdbcDefinition() {
		return false;
	}
}
