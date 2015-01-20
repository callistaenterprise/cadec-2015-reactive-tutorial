package se.callista.cadec2015.tutorial.reactive.exercise1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import se.callista.cadec2015.tutorial.reactive.util.blocking.UtilBlocking;

@RestController
public class RouterController {

    private static final Logger LOG = LoggerFactory.getLogger(RouterController.class);

    @Value("${serviceProvider.url}")
    private String serviceProviderUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UtilBlocking util;

    /**
     * Sample usage:
     * $ curl "http://localhost:9080/router?qry=whatever"
     *
     * @param qry
     * @return
     */
    @RequestMapping("/router")
    public ResponseEntity<String> router(
        @RequestParam(value = "qry", required = false, defaultValue = "")  String qry) {

        String url = serviceProviderUrl + "/service?qry=" + qry;

        try {
            LOG.debug("Start route request to: {}", url);

            // Execute a blocking request to the service provider
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            LOG.debug("End route request, response: {}", response.getBody());
            return response;

        } catch (RuntimeException ex) {
            return util.handleException(ex, url);
        }
    }
}