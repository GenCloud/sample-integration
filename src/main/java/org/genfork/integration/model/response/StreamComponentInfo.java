package org.genfork.integration.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.genfork.integration.stream.annotation.StreamComponent;

import java.io.Serializable;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
@Builder
@JsonInclude(NON_NULL)
public class StreamComponentInfo implements Serializable {
	private String componentName;

	private StreamComponent.ComponentType componentType;

	private boolean subComponent;

	private List<ComponentOption> options;
}
