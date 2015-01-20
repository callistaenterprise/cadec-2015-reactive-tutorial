package se.callista.cadec2015.tutorial.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import se.callista.cadec2015.tutorial.reactive.exercise2.LibraryActionResult;
import se.callista.cadec2015.tutorial.reactive.util.callback.AsyncHttpClientCallback;
import se.callista.cadec2015.tutorial.reactive.util.rx.AsyncHttpClientRx;

@ComponentScan()
@EnableAutoConfiguration
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    @Value("${serviceProvider.connectionTimeoutMs}")
    private int serviceProviderConnectionTimeoutMs;

    @Value("${serviceProvider.requestTimeoutMs}")
    private int serviceProviderRequestTimeoutMs;

    @Value("${serviceProvider.maxRequestRetry}")
    private int serviceProviderMaxRequestRetry;

    @Bean(name="libraryActionResultReader")
    private ObjectReader getLibraryActionResultReader() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.reader(LibraryActionResult.class);
    }

    @Bean(name="libraryActionResultWriter")
    private ObjectWriter getLibraryActionResultWriter() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writer();
    }

    /**
     * Create a blocking http client
     *
     * @return
     */
    @Bean
    public RestTemplate getRestTemplate() {
        LOG.debug("Creates a new RestTemplate-object with: connection-timeout: {} ms, read-timeout: {} ms", serviceProviderConnectionTimeoutMs, serviceProviderRequestTimeoutMs);
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(serviceProviderConnectionTimeoutMs);
        rf.setReadTimeout(serviceProviderRequestTimeoutMs);
        return new RestTemplate(rf);
    }

    /**
     * Create a non-blocking http client
     * @return
     */
    @Bean
    public AsyncHttpClient getAsyncHttpClient() {
        LOG.debug(
            "Creates a new AsyncHttpClient-object with: connection-timeout: {} ms, " +
            "request-timeout: {} ms and max-retries: {}",
            serviceProviderConnectionTimeoutMs,
            serviceProviderRequestTimeoutMs,
            serviceProviderMaxRequestRetry);

        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().
            setConnectionTimeoutInMs(serviceProviderConnectionTimeoutMs).
            setRequestTimeoutInMs(serviceProviderRequestTimeoutMs).
            setMaxRequestRetry(serviceProviderMaxRequestRetry).
            build();

        return new AsyncHttpClient(config);
    }

    /**
     * Create a labmda aware non-blocking http client
     * @return
     */
    @Bean
    public AsyncHttpClientCallback getAsyncHttpClientLambdaAware() {
        LOG.debug("Creates a new AsyncHttpClientLambdaAware-object");
        return new AsyncHttpClientCallback();
    }

    /**
     * Create a RX Java aware non-blocking http client
     * @return
     */
    @Bean
    public AsyncHttpClientRx getHttpClientRx() {
        LOG.debug("Creates a new AsyncHttpClientRx-object");
        return new AsyncHttpClientRx();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}