package org.cryptos.service.exception;

/**
 * Exception thrown when an invalid time period is provided in the currency-stats service.
 * This class extends {@link CurrencyServiceBaseException}, includes a constructor for passing an error message.
 */
public class WrongTimePeriodException extends CurrencyServiceBaseException {
    /**
     * Constructs a new {@link WrongTimePeriodException} with the specified error message.
     *
     * @param message the error message
     */
    public WrongTimePeriodException(String message) {
        super(message);
    }
}
