package org.cryptos.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object (DTO) for representing the currency in the API(controller) layer.
 * This DTO contains only the symbol of the currency.
 *
 * @param symbol the symbol of the currency, must not be null.
 */
public record CurrencyDTO(@NotNull String symbol) {
}
