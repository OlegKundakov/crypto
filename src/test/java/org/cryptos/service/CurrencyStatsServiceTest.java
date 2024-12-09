package org.cryptos.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyStatsServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private CurrencyStatsRepository currencyStatsRepository;
    @Mock
    private Resource resource;
    @InjectMocks
    private CurrencyStatsService currencyStatsService;

    @Captor
    private ArgumentCaptor<List<CurrencyStatsEntity>> currencyStatsRepoCaptor;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(currencyStatsService, "batchSize", 2);
        ReflectionTestUtils.setField(currencyStatsService, "defaultPeriodBefore", Period.ofDays(2));
    }

    @Test
    void createStatsSuccessfully() throws IOException {
        // given
        String csvContent = """
                Timestamp,Symbol,Price
                1641308400000,BTC,47111.11
                1641492000000,BTC,43112.12
                1643626800000,BTC,37115.15
                """;
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
        when(currencyRepository.findById("BTC")).thenReturn(Optional.of(new CurrencyEntity("BTC")));

        // when
        currencyStatsService.createStats(resource);

        // then
        verify(currencyRepository, times(1)).findById("BTC");
        verify(currencyStatsRepository, times(2)).saveAll(currencyStatsRepoCaptor.capture());
        List<List<CurrencyStatsEntity>> capturedValues = currencyStatsRepoCaptor.getAllValues();

        validateFirstBatch(capturedValues.getFirst());
        validateLastBatch(capturedValues.getLast());
    }

    @Test
    void throwCSVFileProcessExceptionWhenNotThreeColumns() throws IOException {
        // given
        String csvContent = """
                Timestamp,Symbol,Price
                1641308400000,BTC,47111.11,wrong
                """;
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
        //when(currencyRepository.findById("BTC")).thenReturn(Optional.of(new CurrencyEntity("BTC")));

        // when & then
        CSVFileProcessException exception = assertThrows(CSVFileProcessException.class,
                () -> currencyStatsService.createStats(resource));
        assertEquals("CSV file must contain exactly 3 columns per line", exception.getMessage());
    }

    private void validateFirstBatch(List<CurrencyStatsEntity> firstBatch) {
        assertEquals(2, firstBatch.size());
        CurrencyStatsEntity firstBatchEntity0 = firstBatch.get(0);
        assertAllFieldsEqual("1641308400000", "BTC", "47111.11", firstBatchEntity0);
        CurrencyStatsEntity firstBatchEntity1 = firstBatch.get(1);
        assertAllFieldsEqual("1641492000000", "BTC", "43112.12", firstBatchEntity1);
    }

    private void validateLastBatch(List<CurrencyStatsEntity> lastBatch) {
        assertEquals(1, lastBatch.size());
        CurrencyStatsEntity lastBatchEntity0 = lastBatch.getFirst();
        assertAllFieldsEqual("1643626800000", "BTC", "37115.15", lastBatchEntity0);
    }

    /**
     * Verifies that the exception is thrown when attempting to create stats with unavailable currency.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void throwEntityNotFoundException() throws IOException {
        // given
        String csvContent = """
                Timestamp,Symbol,Price
                1641308400000,BTC,47111.11
                """;
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
        when(currencyRepository.findById("BTC")).thenReturn(Optional.empty());

        // when & then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            currencyStatsService.createStats(resource);
        });
        assertEquals("Currency not found, need to enable the currency first", exception.getMessage());
    }

    /**
     * Verifies that the exception is thrown while trying to read CSV from the resource.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void throwCSVFileProcessExceptionWhenIOExceptionOccurs() throws IOException {
        // given
        when(resource.getInputStream()).thenThrow(IOException.class);

        // when & then
        CSVFileProcessException exception = assertThrows(CSVFileProcessException.class, () -> {
            currencyStatsService.createStats(resource);
        });
        assertEquals("Exception occurs while reading CSV file", exception.getMessage());
    }

    /**
     * Verifies that an exception is thrown when multiple currencies are detected instead of a single currency expected.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void throwExceptionWhenMultipleCurrencies() throws IOException {
        // given
        String csvContent = """
                Timestamp,Symbol,Price
                1641308400000,BTC,47111.11
                "1641823200000","ETH","51000"
                """;
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
        when(currencyRepository.findById("BTC")).thenReturn(Optional.of(new CurrencyEntity("BTC")));

        // when & then
        CSVFileProcessException exception = assertThrows(CSVFileProcessException.class, () -> {
            currencyStatsService.createStats(resource);
        });
        assertEquals("Multiple currencies found, expected only 'BTC' but found 'ETH'", exception.getMessage());
    }

    /**
     * Verifies that the stats are correctly fetched from the repository and mapped to domain object.
     * Optional parameters startDateTime & endDateTime are are used.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void getCurrencyStatsSuccessfully() {
        // given
        String symbol = "BTC";
        LocalDateTime startDateTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = LocalDateTime.now();
        CurrencyStatsMinMaxProjection projection = mockCurrencyStatsMinMaxProjection(symbol, startDateTime, endDateTime);
        when(currencyStatsRepository.findStatsBySymbol(symbol, startDateTime, endDateTime))
                .thenReturn(Optional.of(projection));

        // when
        CurrencyStatsMinMaxDomain result = currencyStatsService.getCurrencyStats(symbol, startDateTime, endDateTime);

        // then
        assertEquals(symbol, result.symbol());
        assertEquals(startDateTime, result.oldestDate());
        assertEquals(endDateTime, result.newestDate());
        assertEquals(new BigDecimal("50000"), result.minPrice());
        assertEquals(new BigDecimal("60000"), result.maxPrice());

        verify(currencyStatsRepository).findStatsBySymbol(symbol, startDateTime, endDateTime);
    }

    /**
     * Verifies that the stats are correctly fetched even when optional parameters startDateTime & endDateTime
     * are not provided. The repository is mocked, so no actual database query occurs.
     */
    @Test
    void getCurrencyStatsSuccessfullyWithNullDateTimes() {
        // given
        String symbol = "BTC";
        CurrencyStatsMinMaxProjection projection = mockCurrencyStatsMinMaxProjection(symbol, LocalDateTime.now(), LocalDateTime.now());
        when(currencyStatsRepository.findStatsBySymbol(eq(symbol), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(projection));

        // when
        CurrencyStatsMinMaxDomain result = currencyStatsService.getCurrencyStats(symbol, null, null);

        // then
        assertEquals(symbol, result.symbol());
        assertEquals(new BigDecimal("50000"), result.minPrice());
        assertEquals(new BigDecimal("60000"), result.maxPrice());
    }

    /**
     * Verifies that an exception is thrown if the repository returns empty data.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void getCurrencyStatsThrowExceptionWhenNoDataFound() {
        // given
        String symbol = "BTC";
        LocalDateTime startDateTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = LocalDateTime.now();
        when(currencyStatsRepository.findStatsBySymbol(symbol, startDateTime, endDateTime))
                .thenReturn(Optional.empty());

        // when & then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> currencyStatsService.getCurrencyStats(symbol, startDateTime, endDateTime));

        assertEquals("Currency 'BTC' not found", exception.getMessage());
    }

    /**
     * Verifies that the normalized prices are correctly loaded from the service.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void getAllCurrenciesNormalizedSuccessfully() {
        // given
        LocalDateTime startDateTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = LocalDateTime.now();
        CurrencyNormalizedPriceProjection btcProjection = mockCurrencyNormalizedPriceProjection("BTC", new BigDecimal("0.8"));
        CurrencyNormalizedPriceProjection ethProjection = mockCurrencyNormalizedPriceProjection("ETH", new BigDecimal("0.7"));

        when(currencyStatsRepository.getNormalizedPricesForAllCurrenciesDesc(startDateTime, endDateTime))
                .thenReturn(List.of(btcProjection, ethProjection));

        // when
        List<CurrencyNormalizedPriceDomain> result = currencyStatsService.getAllCurrenciesNormalized(startDateTime, endDateTime);

        // then
        assertEquals(2, result.size());
        assertEquals("BTC", result.get(0).symbol());
        assertEquals(new BigDecimal("0.8"), result.get(0).normalizedPrice());
        assertEquals("ETH", result.get(1).symbol());
        assertEquals(new BigDecimal("0.7"), result.get(1).normalizedPrice());
    }

    /**
     * Verifies that empty list is returned because normalized prices are not found.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void getAllCurrenciesNormalizedNoDataFound() {
        // given
        LocalDateTime startDateTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = LocalDateTime.now();
        when(currencyStatsRepository.getNormalizedPricesForAllCurrenciesDesc(startDateTime, endDateTime))
                .thenReturn(List.of());

        // when & then
        assertTrue(currencyStatsService.getAllCurrenciesNormalized(startDateTime, endDateTime).isEmpty());
    }

    /**
     * Verifies that the exception is thrown with the correct message when the dates are in the wrong order.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void throwWrongTimePeriodExceptionWhenStartDateAfterEndDate() {
        // given
        LocalDateTime startDateTime = LocalDateTime.of(2000, 2, 2, 1, 1);
        LocalDateTime endDateTime = LocalDateTime.of(2000, 1, 1, 1, 1);

        // when & then
        WrongTimePeriodException exception = assertThrows(WrongTimePeriodException.class,
                () -> currencyStatsService.getAllCurrenciesNormalized(startDateTime, endDateTime));
        assertEquals("The start date '2000-02-02T01:01' must be before the end date '2000-01-01T01:01'",
                exception.getMessage());
    }

    /**
     * Verifies that the highest normalized price for the day is correctly retrieved from the repository.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void getHighestNormalizedPriceForDaySuccessfully() {
        // given
        LocalDate day = LocalDate.of(2024, 1, 1);
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        CurrencyNormalizedPriceProjection projection = mockCurrencyNormalizedPriceProjection("BTC", new BigDecimal("0.8"));
        when(currencyStatsRepository.findHighestNormalizedRangeForDay(startOfDay, startOfNextDay))
                .thenReturn(List.of(projection));

        // when
        CurrencyNormalizedPriceDomain result = currencyStatsService.getHighestNormalizedPriceForDay(day);

        // then
        assertEquals("BTC", result.symbol());
        assertEquals(new BigDecimal("0.8"), result.normalizedPrice());
    }

    /**
     * Tests that EntityNotFoundException is thrown when no data is found for the given day.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void throwEntityNotFoundExceptionWhenNoDataIdDatabase() {
        // given
        LocalDate day = LocalDate.of(2024, 1, 1);
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        when(currencyStatsRepository.findHighestNormalizedRangeForDay(startOfDay, startOfNextDay))
                .thenReturn(List.of());

        // when & then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> currencyStatsService.getHighestNormalizedPriceForDay(day));
        assertEquals("Prices not found for the day '2024-01-01'", exception.getMessage());
    }

    /**
     * Tests that the current day is used as default when the day parameter is null.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void getHighestNormalizedPriceForDayUseCurrentDayWhenDateIsNull() {
        // given
        CurrencyNormalizedPriceProjection projection = mockCurrencyNormalizedPriceProjection("BTC", new BigDecimal("0.8"));

        when(currencyStatsRepository.findHighestNormalizedRangeForDay(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(projection));

        // when
        CurrencyNormalizedPriceDomain result = currencyStatsService.getHighestNormalizedPriceForDay(null);

        // then
        assertEquals("BTC", result.symbol());
        assertEquals(new BigDecimal("0.8"), result.normalizedPrice());
    }

    private long toMillis(LocalDateTime localDateTime) {
        return ZonedDateTime
                .of(localDateTime, ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }


    private void assertAllFieldsEqual(String expectedTimestamp,
                                      String expectedSymbol,
                                      String expectedPrice,
                                      CurrencyStatsEntity actualEntity) {

        assertEquals(Long.parseLong(expectedTimestamp), toMillis(actualEntity.getDateTime()));
        assertEquals(expectedSymbol, actualEntity.getCurrency().getSymbol());
        assertEquals(expectedPrice, actualEntity.getPrice().toString());

    }

    private CurrencyStatsMinMaxProjection mockCurrencyStatsMinMaxProjection(String symbol,
                                                                            LocalDateTime startDateTime,
                                                                            LocalDateTime endDateTime) {
        CurrencyStatsMinMaxProjection projection = mock(CurrencyStatsMinMaxProjection.class);
        when(projection.getSymbol()).thenReturn(symbol);
        when(projection.getOldestDate()).thenReturn(startDateTime);
        when(projection.getNewestDate()).thenReturn(endDateTime);
        when(projection.getMinPrice()).thenReturn(new BigDecimal("50000"));
        when(projection.getMaxPrice()).thenReturn(new BigDecimal("60000"));
        return projection;
    }

    private CurrencyNormalizedPriceProjection mockCurrencyNormalizedPriceProjection(String symbol,
                                                                                    BigDecimal normalizedPrice) {
        CurrencyNormalizedPriceProjection projection = mock(CurrencyNormalizedPriceProjection.class);
        when(projection.getSymbol()).thenReturn(symbol);
        when(projection.getNormalizedPrice()).thenReturn(normalizedPrice);
        return projection;

    }
}