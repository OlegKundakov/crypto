package org.cryptos.service.exception;

/**
 * Exception thrown when there is an error processing a CSV file in the currency stats service.
 * This class extends {@link CurrencyServiceBaseException} and provides specific error handling
 * for issues related to CSV file processing. Includes constructors for
 * passing an error message and an optional cause.
 */
public class CSVFileProcessException extends CurrencyServiceBaseException {

    /**
     * Creates new {@link CSVFileProcessException} with the specified error message.
     *
     * @param message the error message
     */
    public CSVFileProcessException(String message) {
        super(message);
    }

    /**
     * Creates new {@link CSVFileProcessException} with the specified error message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public CSVFileProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
