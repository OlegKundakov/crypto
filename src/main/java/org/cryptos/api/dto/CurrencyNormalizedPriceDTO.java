package org.cryptos.api.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for representing normalized price information of a currency.
 * Contains the currency symbol and its corresponding normalized price.
 *
 * @param symbol the symbol of the currency,
 * @param normalizedPrice
 */
public record CurrencyNormalizedPriceDTO(
        String symbol,
        BigDecimal normalizedPrice) {
}

