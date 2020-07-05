package org.genfork.integration.model.response;

import lombok.Data;
import org.genfork.integration.model.components.AbstractDefinition;

import java.io.Serializable;
import java.util.List;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
public class StreamDefinition implements Serializable {
	private String streamName;
	private String streamId;
	private StreamStatus streamStatus;

	private List<StreamComponentInfo> components;

	private List<AbstractDefinition<?>> definitions;

	public enum StreamStatus {
		UNDEPLOYED,
		DEPLOYED,
		STARTED,
		INCOMPLETE,
		FAILED
	}
}
