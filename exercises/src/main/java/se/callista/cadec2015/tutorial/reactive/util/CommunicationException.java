package se.callista.cadec2015.tutorial.reactive.util;

import org.springframework.http.ResponseEntity;

/**
 *
 */
public class CommunicationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private ResponseEntity<String> errorResponse;
	private String url;
	private String requestName;

	public CommunicationException(Throwable cause, ResponseEntity<String> errorResponse, String url, String requestName) {
		super(cause);
		this.errorResponse = errorResponse;
		this.url = url;
		this.requestName = requestName;
	}

	public ResponseEntity<String> getErrorResponse() {
		return errorResponse;
	}

	public String getUrl() {
		return url;
	}

	public String getRequestName() {
		return requestName;
	}
}

