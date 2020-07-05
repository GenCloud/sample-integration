package org.genfork.integration.exceptions;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
public class StreamException extends IntegrationException {
	public StreamException(String message) {
		super(message);
	}

	public StreamException(String message, Throwable cause) {
		super(message, cause);
	}
}
