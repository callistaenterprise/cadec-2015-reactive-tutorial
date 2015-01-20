package se.callista.cadec2015.tutorial.reactive.exercise2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.callista.cadec2015.tutorial.reactive.util.CommunicationException;
import se.callista.cadec2015.tutorial.reactive.util.blocking.UtilBlocking;

@RestController
public class BookLoanController {

    private static final Logger LOG = LoggerFactory.getLogger(BookLoanController.class);

    private static final String RESULT_AVAILABLE = "AVAILABLE";
    private static final String RESULT_BORROWED = "BORROWED";
    private static final String RESULT_RESERVED = "RESERVED";

    @Autowired
    private UtilBlocking util;

    /**
     * Sample usage:
     * $ curl "http://localhost:9080/bookLoan?bookId=1&customerId=2"
     *
     * @param bookId
     * @param customerId
     * @return
     */
    @RequestMapping("/bookLoan")
    public ResponseEntity<String> routingSlip(
        @RequestParam (value = "bookId",     required = true) String bookId,
        @RequestParam (value = "customerId", required = true) String customerId) {

        try {
            LOG.debug("Start processing bookLoan request, bookId: {}, customerId: {}", bookId, customerId);

            String action = null;
            long timestamp = System.currentTimeMillis();

            ResponseEntity<String> response;

            // Send blocking request #1, check customer
            response = util.execute("#1, check customer", "/library/checkCustomer?customerId=" + customerId);

            // Send blocking request #2, check book
            response = util.execute("#2, check book", "/library/checkBook?bookId=" + bookId);

            // Decide if we can borrow the book or if we have to reserve it for a later borrow
            LibraryActionResult lar = util.response2LibraryActionResult(response);
            if (lar.getActionResult().equals(RESULT_AVAILABLE)) {

                // Send blocking request #3.1, borrow book
                action = RESULT_BORROWED;
                response = util.execute("#3.1, borrow book", "/library/borrowBook?bookId=" + bookId + "&customerId=" + customerId);

            } else {

                // Send blocking request #3.2, reserve book
                action = RESULT_RESERVED;
                response = util.execute("#3.2, reserve book", "/library/reserveBook?bookId=" + bookId + "&customerId=" + customerId);
            }

            // Send final blocking request #4, confirm book borrow/reserve operation
            response = util.execute("#4, confirm", "/library/confirmBook?bookId=" + bookId + "&customerId=" + customerId);

            // We are done, create a response and send it back to the caller
            long processingTimeMs = System.currentTimeMillis() - timestamp;
            LOG.debug("Processing complete, time: {} ms", processingTimeMs);
            return util.libraryActionResult2Response(action, (int) processingTimeMs);

        } catch(CommunicationException commEx) {

            // Handle timeout and other communications errors
            LOG.error("Processing aborted with an error in request {}", commEx.getRequestName());
            return commEx.getErrorResponse();
        }
    }
}