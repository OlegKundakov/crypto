package org.cryptos.api.controller.exception;

import lombok.extern.slf4j.Slf4j;
import org.cryptos.service.exception.CurrencyServiceBaseException;
import org.cryptos.service.exception.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler for the currency controller layer. This class handles exceptions
 * thrown by the currency-related services and returns appropriate HTTP response codes and messages.
 */
@Slf4j
@ControllerAdvice
public class CurrencyExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles {@link EntityNotFoundException}. This exception is thrown when a requested
     * entity is not found.
     *
     * @param ex the exception thrown when an entity is not found
     * @return a {@link ResponseStatusException} with HTTP status 404 (Not Found)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseStatusException handleEntityNotFoundException(EntityNotFoundException ex) {
        log.error("EntityNotFoundException occurs", ex);
        return new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
    }

    /**
     * Handles {@link CurrencyServiceBaseException}. This exception is thrown for general errors
     * related to currency service operations and means invalid user input.
     *
     * @param ex the exception thrown during currency service operations
     * @return a {@link ResponseStatusException} with HTTP status 400 (Bad Request)
     */
    @ExceptionHandler(CurrencyServiceBaseException.class)
    public ResponseStatusException handleCurrencyServiceBaseException(CurrencyServiceBaseException ex) {
        log.error("CurrencyServiceBaseException occurs", ex);
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }

    /**
     * Handles any other general {@link Exception}. This serves as a fallback for unexpected errors
     * that don't fall under specific exceptions.
     *
     * @param ex the general exception
     * @return a {@link ResponseStatusException} with HTTP status 500 (Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseStatusException handleGeneralException(Exception ex) {
        log.error("Exception occurs", ex);
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", ex);
    }
}
