package se.callista.cadec2015.tutorial.reactive.serviceprovider.library;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.cadec2015.tutorial.reactive.serviceprovider.setProcTime.SetProcTimeBean;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@RestController
@RequestMapping("/library")
public class LibraryController {
    
    private static final Logger LOG = LoggerFactory.getLogger(LibraryController.class);

    public static final String ID_OK             = "123456";
    public static final String ID_TIMEOUT        = "TIMEOUT";
    public static final String ID_NOT_FOUND      = "FAKE";
    public static final String ID_ALREADY_BOOKED = "STORMEN";

    public static final String RESULT_OK         = "OK";
    public static final String RESULT_BOOKED     = "BOOKED";
    public static final String RESULT_AVAILABLE  = "AVAILABLE";

    @Autowired
    @Qualifier("timerService")
    private ScheduledExecutorService timerService;

    @Autowired
    private SetProcTimeBean setProcTimeBean;

    @Value("${client.requestTimeoutMs}")
    private int clientRequestTimeoutMs;

    /**
     * Sample usage:
     * $ curl -i "http://localhost:8080/library/checkCustomer?customerId=Kalle"
     * $ curl -i "http://localhost:8080/library/checkCustomer?customerId=FAKE"
     *
     * @param customerId
     * @return
     */
    @RequestMapping("/checkCustomer")
    public DeferredResult<LibraryActionResult> serviceCheckCustomer(HttpServletRequest request,
        @RequestParam(value = "customerId", required = true)  String customerId) {

        boolean forceTimeout = customerId.equalsIgnoreCase(ID_TIMEOUT);

        return setupProcessing(request, forceTimeout, (processingTimeMs) -> {

            if (customerId.length() == 0)                  throw new InvalidRequestException("No customerId provided!");
            if (customerId.equalsIgnoreCase(ID_NOT_FOUND)) throw new NotFoundException("Customer", customerId);

            return new LibraryActionResult(RESULT_OK, processingTimeMs);
        });
    }

    /**
     * Sample usage:
     * $ curl -i "http://localhost:8080/library/checkBook?bookId=xyz"
     * $ curl -i "http://localhost:8080/library/checkBook?bookId=STORMEN"
     *
     * @param bookId
     * @return
     */
    @RequestMapping("/checkBook")
    public DeferredResult<LibraryActionResult> serviceCheckBook(HttpServletRequest request,
        @RequestParam(value = "bookId", required = true) String bookId) {

        boolean forceTimeout = bookId.equalsIgnoreCase(ID_TIMEOUT);

        return setupProcessing(request, forceTimeout, (processingTimeMs) -> {

            if (bookId.length() == 0)                  throw new InvalidRequestException("No bookId provided!");
            if (bookId.equalsIgnoreCase(ID_NOT_FOUND)) throw new NotFoundException("Book", bookId);

            String actionResult = bookId.equalsIgnoreCase(ID_ALREADY_BOOKED) ? RESULT_BOOKED : RESULT_AVAILABLE;

            return new LibraryActionResult(actionResult, processingTimeMs);
        });
    }

    /**
     * Sample usage:
     * $ curl -i "http://localhost:8080/library/reserveBook?bookId=STORMEN&customerId=kalle"
     *
     * @param bookId
     * @param customerId
     * @return
     */
    @RequestMapping("/reserveBook")
    public DeferredResult<LibraryActionResult> serviceReserveBook(HttpServletRequest request,
        @RequestParam(value = "bookId",     required = true) String bookId,
        @RequestParam(value = "customerId", required = true) String customerId) {

        return setupProcessing(request, (processingTimeMs) -> {
            return new LibraryActionResult(RESULT_OK, processingTimeMs);
        });
    }

    /**
     * Sample usage:
     * $ curl -i "http://localhost:8080/library/borrowBook?bookid=xyz&customerId=kalle"
     *
     * @param bookId
     * @param customerId
     * @return
     */
    @RequestMapping("/borrowBook")
    public DeferredResult<LibraryActionResult> serviceBorrowBook(HttpServletRequest request,
        @RequestParam(value = "bookId",     required = true) String bookId,
        @RequestParam(value = "customerId", required = true) String customerId) {

        return setupProcessing(request, (processingTimeMs) -> {
            return new LibraryActionResult(RESULT_OK, processingTimeMs);
        });
    }

    /**
     * Sample usage:
     * $ curl -i "http://localhost:8080/library/confirmBook?bookId=xyz&customerId=kalle"
     *
     * @param bookId
     * @param customerId
     * @return
     */
    @RequestMapping("/confirmBook")
    public DeferredResult<LibraryActionResult> serviceConfirmBook(HttpServletRequest request,
            @RequestParam(value = "bookId",     required = true) String bookId,
            @RequestParam(value = "customerId", required = true) String customerId) {

        return setupProcessing(request, (processingTimeMs) -> {
            return new LibraryActionResult(RESULT_OK, processingTimeMs);
        });
    }


    /**
     * Helper function that encapsulate the processing model of a non-blocking test-stub
     *
     * @param request
     * @param f
     * @param <R>
     * @return
     */
    private <R> DeferredResult<R> setupProcessing(HttpServletRequest request, Function<Integer, R> f) {
        return setupProcessing(request, false, f);
    }

    /**
     * Helper function that encapsulate the processing model of a non-blocking test-stub
     *
     * @param request
     * @param forceTimeout
     * @param f
     * @param <R>
     * @return
     */
    private <R> DeferredResult<R> setupProcessing(HttpServletRequest request, boolean forceTimeout, Function<Integer, R> f) {

        // For logging purpose
        String path = request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        // Create the deferredResult
        DeferredResult<R> deferredResult = new DeferredResult<>();

        // Calculate the processing time for this invocation
        int processingTimeMs = forceTimeout ? clientRequestTimeoutMs + 1000 : setProcTimeBean.calculateProcessingTime();

        LOG.debug("New request: {}, processing time: {} ms.", path, processingTimeMs);

        if (forceTimeout) LOG.warn("Forcing timeout...");

        // Schedule the task for asynch completion in the future
        timerService.schedule(
            () -> {
                if (deferredResult.isSetOrExpired()) {
                    LOG.warn("Processing already expired");
                } else {
                    try {
                        // Ok, time to apply the actual business logic supplied by the BiFunction f
                        R result = f.apply(processingTimeMs);
                        deferredResult.setResult(result);
                        LOG.debug("Request done");
                    } catch(Throwable ex) {
                        LOG.error("Request failed: {}", ex.getMessage());
                        deferredResult.setErrorResult(ex);
                    }
                }
            },
            processingTimeMs, TimeUnit.MILLISECONDS);

        LOG.debug("Leave request thread");

        // Processing is now setup, return to let go of the precious thread we are holding on to...
        return deferredResult;
    }
}