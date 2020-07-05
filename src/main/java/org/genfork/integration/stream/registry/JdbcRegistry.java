package org.genfork.integration.stream.registry;

import org.genfork.integration.exceptions.StreamException;
import org.genfork.integration.model.components.outbound.jdbc.JdbcOutboundAdapterDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.jdbc.JdbcOutboundGateway;

import javax.sql.DataSource;

/**
 * The type Jdbc inbound outbound registry.
 *
 * @author: GenCloud
 * @date: 2020/07
 */
public class JdbcRegistry {
	/**
	 * Build outbound jdbc gateway by jdbc adapter definition.
	 *
	 * @param definition - Jdbc Outbound Adapter Definition
	 * @return Jdbc outbound gateway
	 */
	public JdbcOutboundGateway defineOutboundAdapter(String streamId, GenericApplicationContext applicationContext, JdbcOutboundAdapterDefinition definition) {
		final String query = definition.getSubQuery();
		final String updateQuery = definition.getUpdateQuery();
		final DataSource dataSource = getDataSource(streamId, applicationContext, definition.getDataSource());

		JdbcOutboundGateway gateway;
		if (query != null && updateQuery != null) {
			gateway = new JdbcOutboundGateway(dataSource, updateQuery, query);
		} else if (updateQuery != null) {
			gateway = new JdbcOutboundGateway(dataSource, updateQuery);
		} else {
			throw new StreamException("Can't deploy jdbc adapter - update-query is empty!");
		}

		final int maxRows = definition.getMaxRows();
		gateway.setMaxRows(maxRows);

		return gateway;
	}

	private DataSource getDataSource(String streamId, GenericApplicationContext applicationContext, String propertyValue) {
		final String beanName = String.format("%s_%s", streamId, propertyValue);
		if (!applicationContext.containsBean(beanName)) {
			throw new StreamException("Can't deploy data source bean object into jdbc adapter - not found in context!");
		}

		return applicationContext.getBean(beanName, DataSource.class);
	}
}
