package org.cryptos.service.exception;

/**
 * Base exception class for the currency service application.
 * This class extends {@link RuntimeException} and is intended to be used as a base for custom runtime exceptions,
 * that are specific to the currency service layer. It provides constructors to initialize
 * the exception with an error message and an optional cause.
 */
public abstract class CurrencyServiceBaseException extends RuntimeException {
    /**
     * Constructs a new {@link CurrencyServiceBaseException} with the specified error message.
     *
     * @param message the error message
     */
    public CurrencyServiceBaseException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link CurrencyServiceBaseException} with the specified error message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public CurrencyServiceBaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
