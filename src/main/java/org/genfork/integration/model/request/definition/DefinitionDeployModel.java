package org.genfork.integration.model.request.definition;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
public class DefinitionDeployModel implements Serializable {
	private String flowName;
	private String description;
	private List<DefinitionComponent> components;
}
