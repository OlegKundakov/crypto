package org.cryptos.persistence.entity;

import java.math.BigDecimal;

/**
 * Projection interface to get the normalized price of the currency with its symbol.
 * This interface is used for queries that return a normalized price value for a particular currency.
 */
public interface CurrencyNormalizedPriceProjection {
    /**
     * Gets the symbol of the currency ("BTC", "ETH").
     */
    String getSymbol();

    /**
     * Gets the normalized price for this currency.
     */
    BigDecimal getNormalizedPrice();
}