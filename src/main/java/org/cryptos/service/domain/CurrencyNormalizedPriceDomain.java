package org.cryptos.service.domain;

import java.math.BigDecimal;

/**
 * Represents the normalized price of the currency in the business layer (service) of the application.
 * This class holds the symbol of the currency and its normalized price.
 * This record is used to transfer currency data between layers of the application,
 * typically from the API layer (controllers) to the service layer.
 *
 * @param symbol the symbol of the currency ("BTC", "ETH")
 * @param normalizedPrice the normalized price for the currency
 */
public record CurrencyNormalizedPriceDomain(
        String symbol,
        BigDecimal normalizedPrice) {
}

