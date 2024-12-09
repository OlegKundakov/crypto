package org.cryptos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for representing the minimum and maximum statistics of a currency.
 * Contains the currency symbol, the oldest and newest date for the statistics, and the minimum and maximum prices during the period.
 *
 * @param symbol     the symbol of the currency ("BTC", "ETH")
 * @param oldestDate the earliest date when the statistics were actual
 * @param newestDate the latest date when the statistics were actual
 * @param minPrice   the minimum price of the currency during the specified period
 * @param maxPrice   the maximum price of the currency during the specified period
 */
public record CurrencyStatsMinMaxDTO(
        String symbol,
        LocalDateTime oldestDate,
        LocalDateTime newestDate,
        BigDecimal minPrice,
        BigDecimal maxPrice) {
}

