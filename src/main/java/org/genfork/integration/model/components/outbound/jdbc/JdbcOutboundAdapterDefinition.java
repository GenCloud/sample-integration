package org.genfork.integration.model.components.outbound.jdbc;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.annotation.StreamComponent;

import static org.genfork.integration.stream.annotation.StreamComponent.ComponentType.app;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@StreamComponent(componentName = "jdbc-outbound-adapter", componentType = app)
@SettingClass
@Data
@EqualsAndHashCode(callSuper = true)
public class JdbcOutboundAdapterDefinition extends AbstractDefinition<JdbcOutboundAdapterDefinition> {
	@SettingValue(originName = "data-source", description = "Наименование объекта базы данных", required = true)
	private String dataSource;

	@SettingValue(originName = "update-query", description = "Запрос на обновление данных")
	private String updateQuery;

	@SettingValue(originName = "query", description = "Запрос на выборку данных")
	private String subQuery;

	@SettingValue(originName = "jdbc-reply-channel", description = "Наименование диспетчера исходящего канала", required = true)
	private String jdbcReplyChannel;

	@SettingValue(originName = "max-rows", description = "Максимальное количество выгружаемых объектов в запросе")
	private int maxRows;

	@Override
	public boolean isJdbcDefinition() {
		return true;
	}

	@Override
	public boolean isOutbound() {
		return true;
	}
}
