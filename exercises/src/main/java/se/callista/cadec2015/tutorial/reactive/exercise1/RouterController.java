package se.callista.cadec2015.tutorial.reactive.exercise1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.cadec2015.tutorial.reactive.util.callback.AsyncHttpClientCallback;
import se.callista.cadec2015.tutorial.reactive.util.callback.UtilCallback;

@RestController
public class RouterController {

    private static final Logger LOG = LoggerFactory.getLogger(RouterController.class);

    @Value("${serviceProvider.url}")
    private String serviceProviderUrl;

    @Autowired
    private AsyncHttpClientCallback asyncHttpClient;

    @Autowired
    private UtilCallback util;

    /**
     * Sample usage:
     * $ curl "http://localhost:9080/router?qry=whatever"
     *
     * @param qry
     * @return
     */
    @RequestMapping("/router")
    public DeferredResult<ResponseEntity<String>> router(
        @RequestParam(value = "qry", required = false, defaultValue = "")  String qry) {

        String url = serviceProviderUrl + "/service?qry=" + qry;

        LOG.debug("Start route request to {}", url);

        // Setup execution of a blocking request to the service provider
        final DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();

        asyncHttpClient.execute(url,

                throwable -> {
                    util.handleException(throwable, url, deferredResult);
                },

                response -> {
                    LOG.debug("End route request, setting the response on the deferred result: {}", response.getResponseBody());
                    deferredResult.setResult(util.createResponse(response));
                }
        );

        // Return to let go of the precious thread we are holding on to...
        LOG.debug("Asynch non-blocking processing setup, return the request thread to the thread-pool");
        return deferredResult;
    }
}