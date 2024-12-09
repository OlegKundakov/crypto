package org.cryptos.service.exception;

/**
 * Exception thrown when a requested entity is not found in the currency/currency-stats service.
 * This class extends {@link CurrencyServiceBaseException} and provides specific error handling
 * for the cases if entity could not be located. It includes a constructor for passing an error message.
 */
public class EntityNotFoundException extends CurrencyServiceBaseException {
    /**
     * Constructs a new {@link EntityNotFoundException} with the specified error message.
     *
     * @param message the error message
     */
    public EntityNotFoundException(String message) {
        super(message);
    }
}
