package se.callista.cadec2015.tutorial.reactive.util.rx;

import com.ning.http.client.Response;

/**
 *
 */
public class RequestFailureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Response response;

    public RequestFailureException(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
}
