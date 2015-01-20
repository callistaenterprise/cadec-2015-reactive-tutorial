package se.callista.cadec2015.tutorial.reactive.util.blocking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import se.callista.cadec2015.tutorial.reactive.exercise2.LibraryActionResult;
import se.callista.cadec2015.tutorial.reactive.util.CommunicationException;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Component
public class UtilBlocking {

    private static final Logger LOG = LoggerFactory.getLogger(UtilBlocking.class);

    @Autowired
    private RestTemplate restTemplate;

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


    public ResponseEntity<String> execute(String requestName, String uri) throws CommunicationException {

        String url = serviceProviderUrl + uri;

        try {
            LOG.debug("Start request {}", requestName);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            LOG.debug("Got response {}", requestName);

            return response;

        } catch (RuntimeException ex) {

            // Log the error and create a error-response
            ResponseEntity<String> errorResponse = handleException(ex, requestName, url);

            // Throw a communication specific error
            throw new CommunicationException(ex, errorResponse, url, requestName);
        }
    }

    public ResponseEntity<String> handleException(RuntimeException ex , String url) {
        return handleException(ex, "", url);
    }

    public ResponseEntity<String> handleException(RuntimeException ex, String requestName, String url) {

        HttpStatus httpStatus = null;
        String msg = null;

        // Handle timeout errors, return 504 (Gateway Timeout)
        if (ex.getCause() instanceof SocketTimeoutException) {
            httpStatus = HttpStatus.GATEWAY_TIMEOUT;
            msg = "Request timeout (" + serviceProviderRequestTimeoutMs + " ms)";

            // Handle client side errors, pass them on...
        } else if (ex instanceof HttpStatusCodeException) {
            HttpStatusCodeException httpEx = (HttpStatusCodeException)ex;
            httpStatus = httpEx.getStatusCode();
            msg = httpEx.getResponseBodyAsString();

            // Whatever remaining errors...
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            msg = ex.getMessage();
        }

        // Log relevant error information and create a response with the same information
        msg += ", HttpStatus: " + httpStatus.value() + ", Url: " + url;
        LOG.error("Error in request {}: {}", requestName, msg);
        return createResponse(msg, httpStatus);
    }

    public LibraryActionResult response2LibraryActionResult(ResponseEntity<String> response) {
        try {
            return jsonReader.readValue(response.getBody());
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

    /*
     * PRIVATE METHODS
     */

    private ResponseEntity<String> createResponse(String msg, HttpStatus httpStatus) {
        return new ResponseEntity<>(msg, httpStatus);
    }

    private ResponseEntity<String> createResponse(String msg, MultiValueMap<String, String> headers, HttpStatus httpStatus) {
        return new ResponseEntity<>(msg, headers, httpStatus);
    }
}