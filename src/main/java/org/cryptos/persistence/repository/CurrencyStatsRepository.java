package org.cryptos.persistence.repository;

import org.cryptos.persistence.entity.CurrencyNormalizedPriceProjection;
import org.cryptos.persistence.entity.CurrencyStatsEntity;
import org.cryptos.persistence.entity.CurrencyStatsMinMaxProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for run CRUD operations and custom queries on the {@link CurrencyStatsEntity}.
 * This interface extends {@link JpaRepository} to provide methods for interacting with the "currency_stats" table in the database.
 * It also includes custom queries to calculate and retrieve statistics about currency prices for given date range.
 */
public interface CurrencyStatsRepository extends JpaRepository<CurrencyStatsEntity, String> {

    /**
     * Finds the statistics for a given currency symbol for the specified date range.
     * The statistics includes the oldest and newest price record dates and minimum and maximum prices.
     *
     * @param currencySymbol the symbol of the currency ("BTC", "ETH")
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (exclusive)
     * @return an {@link Optional} containing the {@link CurrencyStatsMinMaxProjection} with the statistical data, if found
     */
    @Query("""
            SELECT c.symbol AS symbol,
                   MIN(cstats.dateTime) AS oldestDate,
                   MAX(cstats.dateTime) AS newestDate,
                   MIN(cstats.price) AS minPrice,
                   MAX(cstats.price) AS maxPrice
            FROM CurrencyStatsEntity cstats
            JOIN cstats.currency c
            WHERE c.symbol = :currencySymbol
            AND cstats.dateTime >= :startDate
            AND cstats.dateTime < :endDate
            GROUP BY c.symbol
            """)
    Optional<CurrencyStatsMinMaxProjection> findStatsBySymbol(@Param("currencySymbol") String currencySymbol,
                                                              @Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);


    /**
     * Retrieves the normalized price for all currencies within a given date range.
     * The normalized price is calculated as (max price - min price) / min price.
     * The results are ordered in descending order by normalized price.
     *
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (exclusive)
     * @return a list of {@link CurrencyNormalizedPriceProjection} representing the normalized price for each currency
     */
    @Query("""
            SELECT
                c.symbol AS symbol,
                (MAX(cstats.price) - MIN(cstats.price)) / MIN(cstats.price) AS normalizedPrice
            FROM CurrencyStatsEntity cstats
            JOIN cstats.currency c
            WHERE cstats.dateTime >= :startDate
            AND cstats.dateTime < :endDate
            GROUP BY c.symbol
            ORDER BY normalizedPrice DESC
            """)
    List<CurrencyNormalizedPriceProjection> getNormalizedPricesForAllCurrenciesDesc(@Param("startDate") LocalDateTime startDate,
                                                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Finds the currencies with the highest normalized price range for a given date range.
     * The normalized price is calculated as (max price - min price) / min price.
     * The results are ordered by normalized price in descending order.
     *
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (exclusive)
     * @return a list of {@link CurrencyNormalizedPriceProjection} with the highest normalized price ranges
     */
    @Query("""
            SELECT
                c.symbol AS symbol,
                (MAX(cstats.price) - MIN(cstats.price)) / MIN(cstats.price) AS normalizedPrice
            FROM CurrencyStatsEntity cstats
            JOIN cstats.currency c
            WHERE cstats.dateTime >= :startDate
            AND cstats.dateTime < :endDate
            GROUP BY c.symbol
            ORDER BY normalizedPrice DESC
            """)
    List<CurrencyNormalizedPriceProjection> findHighestNormalizedRangeForDay(@Param("startDate") LocalDateTime startDate,
                                                                             @Param("endDate") LocalDateTime endDate);

}
