package org.genfork.integration.model.request.http;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
public class SimpleJdbcPayload implements Serializable {
	private Integer accountId;
}
