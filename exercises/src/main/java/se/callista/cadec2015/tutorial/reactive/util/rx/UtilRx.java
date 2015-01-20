package se.callista.cadec2015.tutorial.reactive.util.rx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import rx.Observable;
import se.callista.cadec2015.tutorial.reactive.exercise2.LibraryActionResult;
import se.callista.cadec2015.tutorial.reactive.util.CommunicationException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by magnus on 11/12/14.
 */
@Component
public class UtilRx {

    private static final Logger LOG = LoggerFactory.getLogger(UtilRx.class);

    @Value("${serviceProvider.url}")
    private String serviceProviderUrl;

    @Value("${serviceProvider.requestTimeoutMs}")
    private String spRequestTimeoutMs;

    @Autowired
    private AsyncHttpClientRx asyncHttpClient;

    @Autowired
    @Qualifier("libraryActionResultReader")
    private ObjectReader jsonReader;

    @Autowired
    @Qualifier("libraryActionResultWriter")
    private ObjectWriter jsonWriter;

    public Observable<State> execute(Request request) {
        return execute(new State(), request.getDescription(), request.getUri());
    }

    public Observable<State> execute(State state, String requestName, String uri) {
        LOG.debug("Start request {}", requestName);

        String url = serviceProviderUrl + uri;

        return asyncHttpClient
            .observable(url)
            .doOnNext(r -> LOG.debug("Got response {}", requestName))
            .flatMap(r -> isResponseOk(r) ?
                Observable.just(r) :
                Observable.error(new RequestFailureException(r)))
            .map(state::saveResponse)
            .onErrorResumeNext(t -> {
                ResponseEntity<String> errorResponse = handleException(t, requestName);
                return Observable.error(new CommunicationException(t, errorResponse, url, requestName));
            });
    }

    public boolean isResponseOk(Response response) {
        return HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful();
    }

    public LibraryActionResult response2LibraryActionResult(Response response) {
        try {
            return response2LibraryActionResult(response.getResponseBody());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LibraryActionResult response2LibraryActionResult(String response) {
        try {
            return jsonReader.readValue(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public  ResponseEntity<String> libraryActionResult2Response(String action, int processingTimeMs) {
        try {
            String json = jsonWriter.writeValueAsString(new LibraryActionResult(action, (int) processingTimeMs));

            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("Content-Type", "application/json;charset=UTF-8");

            return createResponse(json, headers, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<String> handleException(Throwable t, String requestName) {
        Throwable cause = t;

        if (t instanceof AsyncCallException) {
            AsyncCallException ae = (AsyncCallException) t;
            cause = ae.getCause();
        }

        if(cause instanceof RequestFailureException) {
            Response r = ((RequestFailureException) cause).getResponse();
            LOG.error("Error in request {}: {}", requestName, getResponseBody(r));
            return createResponse(r);

        } else {

            HttpStatus httpStatus;
            String msg;

            if (cause instanceof TimeoutException) {
                httpStatus = HttpStatus.GATEWAY_TIMEOUT;
                msg = "Request timeout (" + spRequestTimeoutMs + " ms)";
            } else {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                msg = "Request failed due to error: " + cause;
            }

            msg += ", HttpStatus: " + httpStatus.value();
            LOG.error("Error in request {}: {}", requestName, msg);
            return new ResponseEntity<>(msg, httpStatus);
        }
    }

    public ResponseEntity<String> createResponse(String responseBody) {
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    public ResponseEntity<String> createResponse(Response response) {
        try {
            return createResponse(response.getResponseBody(), HttpStatus.valueOf(response.getStatusCode()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<String> createResponse(String responseBody, HttpStatus httpStatusCode) {
        return new ResponseEntity<>(responseBody, httpStatusCode);
    }

    public ResponseEntity<String> createResponse(String responseBody, MultiValueMap<String, String> headers, HttpStatus httpStatus) {
        return new ResponseEntity<>(responseBody, headers, httpStatus);
    }

    private String getResponseBody(Response r) {
        try {
            return r.getResponseBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isBookStatus(List<State> results, String bookStatus) {
        return results.stream().anyMatch(state -> {
            return response2LibraryActionResult(state.getLastResponse()).getActionResult().equals(bookStatus);
        });
    }
}
