package se.callista.cadec2015.tutorial.reactive.serviceprovider.library;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LibraryActionResult {

    @XmlElement
    private final String actionResult;

    @XmlElement
    private final int processingTimeMs;

    public LibraryActionResult() {
    	actionResult = "UNKNOWN";
        processingTimeMs = -1;
    }
    
    public LibraryActionResult(String actionResult, int processingTimeMs) {
        this.actionResult = actionResult;
        this.processingTimeMs = processingTimeMs;
    }

    public String getActionResult() {
        return actionResult;
    }

    public int getProcessingTimeMs() {
        return processingTimeMs;
    }
}