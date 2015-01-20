package se.callista.cadec2015.tutorial.reactive.serviceprovider.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import se.callista.cadec2015.tutorial.reactive.serviceprovider.setProcTime.SetProcTimeBean;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
public class ServiceController {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceController.class);

    @Autowired
    @Qualifier("timerService")
    private ScheduledExecutorService timerService;

    @Autowired
    private SetProcTimeBean setProcTimeBean;

    /**
     * Sample usage:
     * $ curl -i "http://localhost:8080/service?qry=3"
     * $ curl -i "http://localhost:8080/service?qry=error"
     * $ curl -i "http://localhost:8080/service?qry=timeout"
     *
     * @param qry
     * @return
     */
    @RequestMapping("/service")
    public DeferredResult<ProcessingStatus> service(
        @RequestParam(value = "qry", required = false, defaultValue = "")  String qry) {

        // Create the deferredResult and initiate a callback object, task, with it
        DeferredResult<ProcessingStatus> deferredResult = new DeferredResult<>();

        // Calculate the processing time, cause a timeout if qry = timeout
        int processingTimeMs = qry.equals("timeout") ? 20000 : setProcTimeBean.calculateProcessingTime();

        LOG.debug("1. Start processing request, expected processing time: {} ms.", processingTimeMs);

        // Return a 500 error if qry = error
        if (qry.equals("error")) {
            deferredResult.setErrorResult(new RuntimeException("Error: Invalid query parameter, qry=" + qry));
            return deferredResult;
        }

        // Schedule the task for asynch completion in the future
        timerService.schedule(
            () -> completeProcessing(deferredResult, qry, processingTimeMs),
            processingTimeMs, TimeUnit.MILLISECONDS);

        LOG.debug("2. Processing of request leave the request thread");

        // Return to let go of the precious thread we are holding on to...
        return deferredResult;
    }

    private void completeProcessing(DeferredResult<ProcessingStatus> deferredResult, String qry, int processingTimeMs) {

        if (deferredResult.isSetOrExpired()) {
            LOG.warn("3. Processing of request already expired");
        } else {

            String status = "Ok";
            if (qry.equals("3")) status += "(3)";

            boolean deferredStatus = deferredResult.setResult(new ProcessingStatus(status, processingTimeMs));
            LOG.debug("3. Processing of request done, deferredStatus = {}", deferredStatus);
        }
    }
}