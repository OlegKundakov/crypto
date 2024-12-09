package org.cryptos.service.domain;

/**
 * Represents a currency in the business(Service) layer of the application.
 * This class holds the symbol of the currency ("BTC", "ETH").
 * This record is used to transfer currency data between layers of the application,
 * typically from the API layer (controllers) to the service layer.
 *
 * @param symbol the symbol of the currency ("BTC", "ETH")
 */
public record CurrencyDomain(String symbol) {
}
