package se.callista.cadec2015.tutorial.reactive.serviceprovider.library;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by magnus on 09/12/14.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid Request")
public class InvalidRequestException extends RuntimeException {

	private static final long serialVersionUID = -1169117496239481466L;

	public InvalidRequestException(String errorMsg) {
        super(errorMsg);
    }
}