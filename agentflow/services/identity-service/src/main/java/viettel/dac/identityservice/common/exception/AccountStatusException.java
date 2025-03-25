package viettel.dac.identityservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when account status prevents an action
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountStatusException extends RuntimeException {
    public AccountStatusException(String message) {
        super(message);
    }
}