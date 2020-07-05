package org.genfork.integration.model.components.messaging;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.model.enums.ChannelType;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.annotation.StreamComponent;
import org.genfork.integration.stream.converters.impl.EnumConverter;
import org.springframework.messaging.MessageChannel;

import static org.genfork.integration.stream.annotation.StreamComponent.ComponentType.app;
import static org.genfork.integration.stream.annotation.StreamComponent.ComponentType.source;

/**
 * Model storage parameters for {@link MessageChannel} implementations providing common
 * properties such as the channel name.
 *
 * @author: GenCloud
 * @date: 2020/07
 */
@StreamComponent(componentName = "message-channel", componentType = {source, app})
@SettingClass(excludedProperties = {})
@Data
@EqualsAndHashCode(callSuper = true)
public class ChannelDefinition extends AbstractDefinition<ChannelDefinition> {
	@SettingValue(originName = "channel-type", description = "Тип диспетчера канала", required = true, converter = EnumConverter.class)
	private ChannelType channelType;

	@SettingValue(description = "Оператор, указывающий на то что должен ли диспетчер канала включать аварийное переключение")
	private boolean failover;

	@SettingValue(originName = "max-subscribers", description = "Максимальное количество подключений, поддерживаемое диспетчером канала")
	private int maxSubscribers = Integer.MAX_VALUE;

	@Override
	public boolean isChannelDefinition() {
		return true;
	}
}
