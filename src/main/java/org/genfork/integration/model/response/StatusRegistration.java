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
public class StatusRegistration implements Serializable {
	private Status status;
	private String streamId;
	private String message;

	public StatusRegistration(Status status) {
		this.status = status;
	}

	public StatusRegistration(Status status, String message) {
		this.status = status;
		this.message = message;
	}

	public StatusRegistration(Status status, String streamId, String message) {
		this.status = status;
		this.streamId = streamId;
		this.message = message;
	}

	public enum Status {
		ERROR,
		SUCCESS
	}
}
