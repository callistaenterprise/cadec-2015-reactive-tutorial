package se.callista.cadec2015.tutorial.reactive.util.callback;

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
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.cadec2015.tutorial.reactive.exercise2.LibraryActionResult;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Component
public class UtilCallback {

    private static final Logger LOG = LoggerFactory.getLogger(UtilCallback.class);

    @Autowired
    private AsyncHttpClientCallback asyncHttpClient;

    @Value("${serviceProvider.url}")
    private String serviceProviderUrl;

    @Value("${serviceProvider.requestTimeoutMs}")
    private String serviceProviderRequestTimeoutMs;

    @Autowired
    @Qualifier("libraryActionResultReader")
    private ObjectReader jsonReader;

    @Autowired
    @Qualifier("libraryActionResultWriter")
    private ObjectWriter jsonWriter;

    public void execute(DeferredResult<ResponseEntity<String>> deferredResult, String requestName, String uri, Consumer<Response> f) {

        // Send non-blocking request #1, check customer
        String url = serviceProviderUrl + uri;
        LOG.debug("Start request {}", requestName);

        asyncHttpClient.execute(url,

            throwable -> {
                // Some communication error, like timeout has happened
                // Log and set an error response and abort further processing
                handleException(throwable, url, deferredResult);
            },

            response -> {
                LOG.debug("Got response {}", requestName);

                // Check response, if not ok simply pass back the error and abort further processing
                if (!isResponseOk(response)) {
                    logAndSetErrorResponse(deferredResult, requestName, url, response);
                    return;
                }

                // Call the actual business logic that is supposed to act on the response
                f.accept(response);
            }
        );
    }

    public boolean isResponseOk(Response response) {
        return response.getStatusCode() / 100 == 2; // Any status code 2xx is ok
    }

    public String getResponseBody(Response r) {
        try {
            return r.getResponseBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LibraryActionResult response2LibraryActionResult(Response response) {
        try {
            return jsonReader.readValue(response.getResponseBody());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<String> libraryActionResult2Response(String action, int processingTimeMs) {
        try {
            String json = jsonWriter.writeValueAsString(new LibraryActionResult(action, (int) processingTimeMs));

            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("Content-Type", "application/json;charset=UTF-8");

            return createResponse(json, headers, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleException(Throwable throwable, String url, DeferredResult<ResponseEntity<String>> deferredResult) {

        HttpStatus httpStatus;
        String msg;

        if (throwable instanceof TimeoutException) {
            httpStatus = HttpStatus.GATEWAY_TIMEOUT;
            msg = "Request timeout (" + serviceProviderRequestTimeoutMs + " ms)";
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            msg = "Request failed due to error: " + throwable;
        }

        msg += ", HttpStatus: " + httpStatus.value() + ", Url: " + url;
        LOG.error(msg);
        deferredResult.setResult(new ResponseEntity<String>(msg, httpStatus));
    }

    private void logAndSetErrorResponse(DeferredResult<ResponseEntity<String>> deferredResult, String requestName, String url, Response response) {
        ResponseEntity<String> errorResponse = createResponse(response);
        String msg = errorResponse.getBody();
        HttpStatus httpStatus = errorResponse.getStatusCode();
        msg += ", HttpStatus: " + httpStatus.value() + ", Url: " + url;

        LOG.error("Error in request {}: {}", requestName, msg);
        deferredResult.setResult(errorResponse);
    }

    public ResponseEntity<String> createResponse(Response response) {
        try {
            return new ResponseEntity<String>(response.getResponseBody(), HttpStatus.valueOf(response.getStatusCode()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<String> createResponse(String responseBody) {
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    public ResponseEntity<String> createResponse(String responseBody, HttpStatus httpStatusCode) {
        return new ResponseEntity<>(responseBody, httpStatusCode);
    }

    /*
     * PRIVATE METHODS
     */

    private ResponseEntity<String> createResponse(String responseBody, MultiValueMap<String, String> headers, HttpStatus httpStatus) {
        return new ResponseEntity<>(responseBody, headers, httpStatus);
    }

}