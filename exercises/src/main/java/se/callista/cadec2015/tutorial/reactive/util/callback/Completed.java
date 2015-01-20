package se.callista.cadec2015.tutorial.reactive.util.callback;

import com.ning.http.client.Response;

/**
 * Created by magnus on 18/07/14.
 */
public interface Completed {
    public void onCompleted(Response response) throws Exception;
}
