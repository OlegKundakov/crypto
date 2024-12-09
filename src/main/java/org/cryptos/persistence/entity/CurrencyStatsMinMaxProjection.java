package org.cryptos.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projection interface for fetching the minimum/maximum price and oldest/newest date of the currency.
 * This interface is used to use custom queries to retrieve data for the spcific currency.
 */
public interface CurrencyStatsMinMaxProjection {
    /**
     * Gets the symbol of the currency ("BTC", "ETH").
     */
    String getSymbol();

    /**
     * Gets the oldest date of the statistics
     */
    LocalDateTime getOldestDate();

    /**
     * Gets the newest date of the statistics
     */
    LocalDateTime getNewestDate();

    /**
     * Gets the minimum price recorded for this currency.
     */
    BigDecimal getMinPrice();

    /**
     * Gets the maximum price recorded for this currency.
     */
    BigDecimal getMaxPrice();
}
