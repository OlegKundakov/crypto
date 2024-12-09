package org.cryptos.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import org.cryptos.persistence.entity.CurrencyEntity;
import org.cryptos.persistence.entity.CurrencyNormalizedPriceProjection;
import org.cryptos.persistence.entity.CurrencyStatsEntity;
import org.cryptos.persistence.entity.CurrencyStatsMinMaxProjection;
import org.cryptos.persistence.repository.CurrencyRepository;
import org.cryptos.persistence.repository.CurrencyStatsRepository;
import org.cryptos.service.domain.CurrencyNormalizedPriceDomain;
import org.cryptos.service.domain.CurrencyStatsMinMaxDomain;
import org.cryptos.service.exception.CSVFileProcessException;
import org.cryptos.service.exception.EntityNotFoundException;
import org.cryptos.service.exception.WrongTimePeriodException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling currency statistics. Contains methods for uploading, retrieving and normalizing currency statistics.
 * The service is annotated with {@link Service} to notice it as a Spring service,
 * and {@link Transactional} to ensure that the operations are executed within a transaction context.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CurrencyStatsService {

    @Value("${currency.create-stats.batch-size:30}")
    private int batchSize;
    @Value("#{T(java.time.Period).parse('${currency.get-stats.default-before-period:P30D}')}")
    private Period defaultPeriodBefore;

    private final CurrencyRepository currencyRepository;
    private final CurrencyStatsRepository currencyStatsRepository;

    /**
     * Reads a CSV file containing currency statistics and creates {@link CurrencyStatsEntity} records in DB.
     * The method processes the file in batches, validating the data and ensuring that the file contains only one currency.
     * The currency statistics data is associated with an existing {@link CurrencyEntity}.
     *
     * @param resource the resource containing the CSV file to process
     * @throws CSVFileProcessException if an error occurs while reading or processing the CSV file
     * @throws EntityNotFoundException if the currency entity is not found in the repository,
     * @see #validateCurrencyMatch for extra cases when exception thrown
     * @see #validateCsvHeader for extra cases when exception thrown
     */
    public void createStats(Resource resource) {
        try (var reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
            String[] line;
            List<CurrencyStatsEntity> batch = new ArrayList<>(batchSize);
            //skip first as header
            reader.readNext();
            //read currency from second row and keep it to ensure that file contains only one(this) currency
            line = reader.readNext();
            validateCsvHeader(line);
            CurrencyEntity currencyEntity = currencyRepository.findById(line[1])
                    .orElseThrow(() -> new EntityNotFoundException("Currency not found, need to enable the currency first"));
            batch.add(parseAndCreateEntity(line, currencyEntity));

            while ((line = reader.readNext()) != null) {
                validateCsvHeader(line);
                validateCurrencyMatch(currencyEntity.getSymbol(), line[1]);
                batch.add(parseAndCreateEntity(line, currencyEntity));

                if (batch.size() == batchSize) {
                    currencyStatsRepository.saveAll(batch);
                    batch = new ArrayList<>(batchSize);
                }
            }

            if (!batch.isEmpty()) {
                currencyStatsRepository.saveAll(batch);
            }
        } catch (CsvValidationException | IOException e) {
            throw new CSVFileProcessException("Exception occurs while reading CSV file", e);
        }
    }

    /**
     * Retrieves the statistics (min/max price, oldest/newest date) for a specific currency within the specified time period.
     *
     * @param symbol        the symbol of the currency (BTC", "ETH")
     * @param startDateTime the start of the period (inclusive)
     * @param endDateTime   the end of the period (exclusive)
     * @return the {@link CurrencyStatsMinMaxDomain} containing the min/max price and oldest/newest date for the currency
     * @throws EntityNotFoundException  if no statistics are found for the specified currency and time period
     * @throws WrongTimePeriodException if the start date is after the end date
     */
    public CurrencyStatsMinMaxDomain getCurrencyStats(String symbol, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LocalDateTime startDateTimeOrDefault = dateTimeOrDefaultPeriodBefore(startDateTime);
        LocalDateTime endDateTimeOrDefault = dateTimeOrNow(endDateTime);
        validateLocalDateTimes(startDateTimeOrDefault, endDateTimeOrDefault);

        return currencyStatsRepository.findStatsBySymbol(symbol, startDateTimeOrDefault, endDateTimeOrDefault)
                .map(this::convertToDomain)
                .orElseThrow(() -> new EntityNotFoundException("Currency '%s' not found".formatted(symbol)));
    }

    /**
     * Retrieves a list of all currencies with their normalized price for the specified time period.
     * The normalized price is calculated as (max price - min price) / min price. Empty list is returned if no stats found.
     *
     * @param startDateTime optional start date of the period (inclusive). The (current day - 30 days) is used by default.
     * @param endDateTime   optional end date of the period (exclusive). The current day is used by default.
     * @return a list of {@link CurrencyNormalizedPriceDomain} representing each currency and its normalized price
     * @throws WrongTimePeriodException if the start date is after the end date
     */
    public List<CurrencyNormalizedPriceDomain> getAllCurrenciesNormalized(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LocalDateTime startDateTimeOrDefault = dateTimeOrDefaultPeriodBefore(startDateTime);
        LocalDateTime endDateTimeOrDefault = dateTimeOrNow(endDateTime);
        validateLocalDateTimes(startDateTimeOrDefault, endDateTimeOrDefault);

        return currencyStatsRepository.getNormalizedPricesForAllCurrenciesDesc(startDateTimeOrDefault, endDateTimeOrDefault).stream()
                .map(this::convertNormalizedPriceToDomain)
                .toList();
    }

    /**
     * Retrieves the currency with the highest normalized price for the specified day.
     * The normalized price is calculated as (max price - min price) / min price.
     *
     * @param day optional day to retrieve the highest normalized price. The current day is used by default.
     * @return the {@link CurrencyNormalizedPriceDomain} for the currency with the highest normalized price on that day
     * @throws EntityNotFoundException if no price data is found for the specified day
     */
    public CurrencyNormalizedPriceDomain getHighestNormalizedPriceForDay(LocalDate day) {
        LocalDateTime startOfThisDayOrDefault = dateTimeOrNow(day);
        LocalDateTime startOfNextDayOrDefault = dateTimeOrNow(day).plusDays(1);

        List<CurrencyNormalizedPriceProjection> highestNormalizedRangeForDay =
                currencyStatsRepository.findHighestNormalizedRangeForDay(startOfThisDayOrDefault, startOfNextDayOrDefault);
        if (highestNormalizedRangeForDay.isEmpty()) {
            throw new EntityNotFoundException("Prices not found for the day '%s'".formatted(day));
        }
        return convertNormalizedPriceToDomain(highestNormalizedRangeForDay.getFirst());
    }

    /**
     * Parses a line from a CSV file and creates {@link CurrencyStatsEntity} object.
     * The method assumes the CSV line contains the timestamp in milliseconds, the currency symbol, the price.
     *
     * @param line           the CSV line containing data in the format [timestamp, currencySymbol, price]
     * @param currencyEntity the {@link CurrencyEntity} associated with the parsed data
     * @return a new {@link CurrencyStatsEntity} populated with the parsed data
     */
    private CurrencyStatsEntity parseAndCreateEntity(String[] line, CurrencyEntity currencyEntity) {
        var entity = new CurrencyStatsEntity();
        entity.setDateTime(parseMillisToDateTime(line[0]));
        entity.setCurrency(currencyEntity);
        entity.setPrice(new BigDecimal(line[2]));
        return entity;
    }

    /**
     * Validates that the expected currency matches the currency found in the file.
     * This function checks if the given currency matches the expected one.
     * If there is a mismatch, it throws {@link CSVFileProcessException}.
     * If the file is guaranteed to contain only one currency, the function can be disabled.
     *
     * @param expectedCurrency the currency symbol that is expected ("BTC", "ETH")
     * @param readCurrency     the currency symbol found in the file
     * @throws CSVFileProcessException if the expected and read currencies do not match
     */
    private void validateCurrencyMatch(String expectedCurrency, String readCurrency) {
        if (!expectedCurrency.equalsIgnoreCase(readCurrency)) {
            throw new CSVFileProcessException(
                    "Multiple currencies found, expected only '%s' but found '%s'"
                            .formatted(expectedCurrency, readCurrency));
        }
    }

    /**
     * Validates that the start date is before the end date.
     *
     * @param start the start date and time
     * @param end   the end date and time
     * @throws WrongTimePeriodException if the start date is after the end date
     */
    private void validateLocalDateTimes(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new WrongTimePeriodException("The start date '%s' must be before the end date '%s'".formatted(start, end));
        }
    }

    /**
     * Parses a string representing milliseconds since epoch to a {@link LocalDateTime}.
     *
     * @param millisString the string representing milliseconds since epoch
     * @return the corresponding {@link LocalDateTime}
     */
    private LocalDateTime parseMillisToDateTime(String millisString) {
        return Instant.ofEpochMilli(Long.parseLong(millisString))
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * Converts DB entity {@link CurrencyNormalizedPriceProjection}
     * to business entity {@link CurrencyNormalizedPriceDomain}.
     *
     * @param projection the {@link CurrencyNormalizedPriceProjection} to be converted
     * @return the corresponding {@link CurrencyNormalizedPriceDomain}
     */
    private CurrencyNormalizedPriceDomain convertNormalizedPriceToDomain(CurrencyNormalizedPriceProjection projection) {
        return new CurrencyNormalizedPriceDomain(projection.getSymbol(), projection.getNormalizedPrice());
    }

    /**
     * Converts DB entity {@link CurrencyStatsMinMaxProjection} to business entity {@link CurrencyStatsMinMaxDomain}.
     *
     * @param projection the {@link CurrencyStatsMinMaxProjection} to be converted
     * @return the corresponding {@link CurrencyStatsMinMaxDomain}
     */
    private CurrencyStatsMinMaxDomain convertToDomain(CurrencyStatsMinMaxProjection projection) {
        return new CurrencyStatsMinMaxDomain(
                projection.getSymbol(),
                projection.getOldestDate(),
                projection.getNewestDate(),
                projection.getMinPrice(),
                projection.getMaxPrice()
        );
    }

    /**
     * Returns the given date time or, if null, a default period before the current time.
     *
     * @param dateTime the {@link LocalDateTime} to be returned if non-null
     * @return the provided {@link LocalDateTime}, or the default period before the current time
     */
    private LocalDateTime dateTimeOrDefaultPeriodBefore(LocalDateTime dateTime) {
        return dateTime != null ? dateTime : LocalDateTime.now().minus(defaultPeriodBefore);
    }

    /**
     * Returns the given date time or if null, the current date and time.
     *
     * @param dateTime the {@link LocalDateTime} to be returned if non-null
     * @return the provided {@link LocalDateTime}, or the current date and time
     */
    private LocalDateTime dateTimeOrNow(LocalDateTime dateTime) {
        return dateTime != null ? dateTime : LocalDateTime.now();
    }

    /**
     * Converts {@link LocalDate} to {@link LocalDateTime} representing the start of the day,
     * or if the date is null, uses the current date.
     *
     * @param date the {@link LocalDate} to be converted
     * @return the corresponding {@link LocalDateTime} at the start of the day
     */
    private LocalDateTime dateTimeOrNow(LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return effectiveDate.atStartOfDay();
    }

    /**
     * Validates that the array contains exactly 3 elements.
     *
     * @param line the line of the CSV file
     * @throws CSVFileProcessException if the CSV file does not contain exactly 3 columns
     */
    private void validateCsvHeader(String[] line) {
        if (line.length != 3) {
            throw new CSVFileProcessException("CSV file must contain exactly 3 columns per line");
        }
    }
}