package org.genfork.integration.model.components.inbound.http;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.enums.HttpAdapterType;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.StreamComponent;

import static org.genfork.integration.stream.annotation.StreamComponent.ComponentType.source;

/**
 * Model storage parameters http duplex adapter.
 *
 * @author: GenCloud
 * @date: 2020/07
 */
@StreamComponent(componentName = "http-inbound-gateway", componentType = source)
@SettingClass
@Data
@EqualsAndHashCode(callSuper = true)
public class HttpInGatewayAdapterDefinition extends AbstractHttpInboundDefinition<HttpInGatewayAdapterDefinition> {
	public HttpInGatewayAdapterDefinition() {
		setHttpAdapterType(HttpAdapterType.INBOUND_GATEWAY);
	}

	@Override
	public boolean isInbound() {
		return true;
	}
}
