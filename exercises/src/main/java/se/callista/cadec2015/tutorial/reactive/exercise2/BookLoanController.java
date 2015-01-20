package se.callista.cadec2015.tutorial.reactive.exercise2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import rx.Observable;
import rx.Subscription;
import se.callista.cadec2015.tutorial.reactive.util.CommunicationException;
import se.callista.cadec2015.tutorial.reactive.util.rx.Request;
import se.callista.cadec2015.tutorial.reactive.util.rx.State;
import se.callista.cadec2015.tutorial.reactive.util.rx.UtilRx;

import java.util.Arrays;

@RestController
public class BookLoanController {

    private static final Logger LOG = LoggerFactory.getLogger(BookLoanController.class);

    private static final String RESULT_AVAILABLE = "AVAILABLE";
    private static final String RESULT_BORROWED = "BORROWED";
    private static final String RESULT_RESERVED = "RESERVED";

    @Autowired
    private UtilRx util;

    /**
     * Sample usage:
     * $ curl "http://localhost:9080/bookLoan?bookId=1&customerId=2"
     *
     * @param bookId
     * @param customerId
     * @return
     */
    @RequestMapping("/bookLoan")
    public DeferredResult<ResponseEntity<String>> routingSlip(
        @RequestParam (value = "bookId",     required = true) String bookId,
        @RequestParam (value = "customerId", required = true) String customerId) {

        LOG.debug("Start processing bookLoan request, bookId: {}, customerId: {}", bookId, customerId);

        long timestamp = System.currentTimeMillis();

        final DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();

        // Setup an observable, i.e. declare the processing
        Observable<State> observable =

            // Run Check Customer and Check Book in parallel
            Observable.from(Arrays.asList(
                new Request("#1, check customer", "/library/checkCustomer?customerId=" + customerId),
                new Request("#2, check book",     "/library/checkBook?bookId=" + bookId)))
            .flatMap(request -> util.execute(request))
            .buffer(2)

            // Run either Borrow Book or Reserve Book
            .flatMap(results -> {
                State state = new State();
                if (util.isBookStatus(results, RESULT_AVAILABLE)) {
                    state.setAction(RESULT_BORROWED);
                    return util.execute(state, "#3.1, borrow book", "/library/borrowBook?bookId=" + bookId + "&customerId=" + customerId);
                } else {
                    state.setAction(RESULT_RESERVED);
                    return util.execute(state, "#3.2, reserve book", "/library/reserveBook?bookId=" + bookId + "&customerId=" + customerId);
                }
            })

            // Wrap up with the final Confirm step
            .flatMap(state -> util.execute(state, "#4, confirm", "/library/confirmBook?bookId=" + bookId + "&customerId=" + customerId));

        // Subscribe to the observable, i.e. start the processing
        Subscription subscription = observable.subscribe(
            state -> {
                // We are done, create a response and send it back to the caller
                long processingTimeMs = System.currentTimeMillis() - timestamp;
                LOG.debug("Processing complete, time: {} ms", processingTimeMs);
                deferredResult.setResult(util.libraryActionResult2Response(state.getAction(), (int) processingTimeMs));
            },
            throwable -> {
                CommunicationException commEx = (CommunicationException) throwable;
                LOG.error("Processing aborted with an error in request {}", commEx.getRequestName());
                deferredResult.setErrorResult(commEx.getErrorResponse());
            }
        );

        // Unsubscribe, i.e. tear down any resources setup during the processing
        deferredResult.onCompletion(() -> subscription.unsubscribe());

        // Return to let go of the precious thread we are holding on to...
        LOG.debug("Non-blocking processing setup, return the request thread to the thread-pool");
        return deferredResult;
    }
}