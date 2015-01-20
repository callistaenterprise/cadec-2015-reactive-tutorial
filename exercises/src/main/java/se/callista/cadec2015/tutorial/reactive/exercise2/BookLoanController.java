package se.callista.cadec2015.tutorial.reactive.exercise2;

import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.cadec2015.tutorial.reactive.util.callback.UtilCallback;

@RestController
public class BookLoanController {

    private static final Logger LOG = LoggerFactory.getLogger(BookLoanController.class);

    private static final String RESULT_AVAILABLE = "AVAILABLE";
    private static final String RESULT_BORROWED = "BORROWED";
    private static final String RESULT_RESERVED = "RESERVED";

    @Autowired
    private UtilCallback util;

    /**
     * Sample usage:
     * $ curl "http://localhost:9080/bookLoan?bookId=1&customerId=2"
     *
     * @param bookId
     * @param customerId
     * @return
     */
    @RequestMapping("/bookLoan")
    public DeferredResult<ResponseEntity<String>> bookLoan(
        @RequestParam (value = "bookId",     required = true) String bookId,
        @RequestParam (value = "customerId", required = true) String customerId) {

        LOG.debug("Start processing bookLoan request, bookId: {}, customerId: {}", bookId, customerId);

        long timestamp = System.currentTimeMillis();

        DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();

        // Send request #1, check customer
        util.execute(deferredResult, "#1, check customer", "/library/checkCustomer?customerId=" + customerId,
            (Response r1) -> {

                // Send request #2, check book
                util.execute(deferredResult, "#2, check book", "/library/checkBook?bookId=" + bookId,
                    (Response r2) -> {

                        // Decide if we can borrow the book or if we have to reserve it for a later borrow
                        boolean isAvailable = util.response2LibraryActionResult(r2).getActionResult().equals(RESULT_AVAILABLE);

                        String requestName =  isAvailable ? "#3.1, borrow book" : "#3.2, reserve book";
                        String action      =  isAvailable ? RESULT_BORROWED     : RESULT_RESERVED;
                        String url3        =  "/library/" + (isAvailable ? "borrowBook" : "reserveBook") + "?bookId=" + bookId + "&customerId=" + customerId;

                        // Send request #3, borrow or reserve book
                        util.execute(deferredResult, requestName, url3,
                            (Response r3) -> {

                                // Send final request #4, confirm book borrow/reserve operation
                                util.execute(deferredResult, "#4, confirm", "/library/confirmBook?bookId=" + bookId + "&customerId=" + customerId,
                                    (Response r4) -> {

                                        // We are done, create a response and send it back to the caller
                                        long processingTimeMs = System.currentTimeMillis() - timestamp;
                                        LOG.debug("Processing complete, time: {} ms", processingTimeMs);
                                        deferredResult.setResult(util.libraryActionResult2Response(action, (int) processingTimeMs));
                                    }
                                );
                            }
                        );
                    }
                );
            }
        );

        // Return to let go of the precious thread we are holding on to...
        LOG.debug("Non-blocking processing setup, return the request thread to the thread-pool");
        return deferredResult;
    }
}