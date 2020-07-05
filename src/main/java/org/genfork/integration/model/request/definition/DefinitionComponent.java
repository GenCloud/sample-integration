package org.genfork.integration.model.request.definition;

import lombok.Data;
import org.genfork.integration.stream.annotation.StreamComponent;

import java.io.Serializable;
import java.util.Map;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
public class DefinitionComponent implements Serializable {
	private String componentName;
	private StreamComponent.ComponentType componentType;
	private Map<Object, Object> componentParameters;
}
