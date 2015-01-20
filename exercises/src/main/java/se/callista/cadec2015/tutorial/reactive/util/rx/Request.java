package se.callista.cadec2015.tutorial.reactive.util.rx;

/**
 * Created by magnus on 19/01/15.
 */
public class Request {
    private String description;
    private String uri;

    public Request(String description, String uri) {
        this.description = description;
        this.uri = uri;
    }

    public String getDescription() {
        return description;
    }

    public String getUri() {
        return uri;
    }
}
