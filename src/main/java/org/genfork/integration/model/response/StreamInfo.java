package org.genfork.integration.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@Data
@JsonInclude(NON_NULL)
public class StreamInfo implements Serializable {
	private String streamName;
	private String description;
}
