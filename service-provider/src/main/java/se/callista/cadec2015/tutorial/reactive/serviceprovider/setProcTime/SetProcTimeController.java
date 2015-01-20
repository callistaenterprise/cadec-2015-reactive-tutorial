package se.callista.cadec2015.tutorial.reactive.serviceprovider.setProcTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SetProcTimeController {
    
    private static final Logger LOG = LoggerFactory.getLogger(SetProcTimeController.class);

    @Autowired
    private SetProcTimeBean setProcTimeBean;

    /**
     * Sample usage:
     *
     *  curl "http://localhost:8080/set-default-processing-time?minMs=1000&maxMs=2000"
     *
     * @param minMs
     * @param maxMs
     */
    @RequestMapping("/set-default-processing-time")
    public void setDefaultProcessingTime(
        @RequestParam(value = "minMs", required = true) int minMs,
        @RequestParam(value = "maxMs", required = true) int maxMs) {

        setProcTimeBean.setDefaultProcessingTime(minMs, maxMs);
    }
}