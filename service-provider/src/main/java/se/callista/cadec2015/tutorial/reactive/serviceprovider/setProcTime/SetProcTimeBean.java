package se.callista.cadec2015.tutorial.reactive.serviceprovider.setProcTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

@Component
public class SetProcTimeBean {
    
    private static final Logger LOG = LoggerFactory.getLogger(SetProcTimeBean.class);

    @Value("${service.defaultMinMs}")
    private int minMs;

    @Value("${service.defaultMaxMs}")
    private int maxMs;

    public void setDefaultProcessingTime(
        @RequestParam(value = "minMs", required = true) int minMs,
        @RequestParam(value = "maxMs", required = true) int maxMs) {

        if (minMs < 0) {
            minMs = 0;
        }
        if (maxMs < minMs) {
            maxMs = minMs;
        }

        this.minMs = minMs;
        this.maxMs = maxMs;
        LOG.info("Set response time to {} - {} ms.", this.minMs, this.maxMs);
    }

    public int calculateProcessingTime() {
        int processingTimeMs = minMs + (int) (Math.random() * (maxMs - minMs));
        LOG.debug("Return calculated processing time: {} ms", processingTimeMs);
        return processingTimeMs;
    }
}