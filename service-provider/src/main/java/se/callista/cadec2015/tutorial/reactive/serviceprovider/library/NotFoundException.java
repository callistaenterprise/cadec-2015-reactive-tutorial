package se.callista.cadec2015.tutorial.reactive.serviceprovider.library;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by magnus on 09/12/14.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Object not found")
public class NotFoundException extends RuntimeException {

	private static final long serialVersionUID = 6383180313377094663L;

	public NotFoundException(String object, String id) {
        super(object + " " + id + " not found");
    }
}