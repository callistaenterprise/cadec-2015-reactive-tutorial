package se.callista.cadec2015.tutorial.reactive.util.rx;

/**
 *
 */
public class AsyncCallException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private String url;

	public AsyncCallException(String url, Throwable cause) {
		super(cause);
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}

