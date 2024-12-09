package org.cryptos.service.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents the statistical data of a currency in the business layer of the application.
 * This class holds the symbol of the currency, the oldest/newest price record dates,
 * and minimum/maximum prices for the currency for a specified time range.
 * This record is used to transfer currency data between layers of the application,
 * typically from the API layer (controllers) to the service layer.
 *
 * @param symbol the symbol of the currency ("BTC", "ETH")
 * @param oldestDate the oldest date when the price for this currency was actual
 * @param newestDate the newest date when the price for this currency was actual
 * @param minPrice the minimum price recorded for this currency
 * @param maxPrice the maximum price recorded for this currency
 *
 * @author YourName
 */
public record CurrencyStatsMinMaxDomain(
        String symbol,
        LocalDateTime oldestDate,
        LocalDateTime newestDate,
        BigDecimal minPrice,
        BigDecimal maxPrice) {
}

