package se.callista.cadec2015.tutorial.reactive.util.rx;

import com.ning.http.client.Response;

import java.io.IOException;

/**
 * Created by magnus on 11/12/14.
 */
public class State {

    private Response lastResponse;

    private String action = null;

    public State() {
    }

    public State saveResponse(Response response) {
        lastResponse = response;
        return this;
    }

    public Response getLastResponse() {
        return lastResponse;
    }

    public String getLastResponseBody() {
        try {
            return lastResponse.getResponseBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}

