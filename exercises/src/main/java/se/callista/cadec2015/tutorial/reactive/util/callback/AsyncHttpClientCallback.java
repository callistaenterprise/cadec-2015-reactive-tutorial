package se.callista.cadec2015.tutorial.reactive.util.callback;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * Created by magnus on 18/07/14.
 */
public class AsyncHttpClientCallback {

    @Autowired
    private AsyncHttpClient asyncHttpClient;

    public ListenableFuture<Response> execute(String url, final Error e, final Completed c) {

        try {
            return asyncHttpClient
                .prepareGet(url)
                .execute(new AsyncCompletionHandler<Response>() {

                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        c.onCompleted(response);
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        e.onThrowable(t);
                    }

                });

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}