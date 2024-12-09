package org.cryptos.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.cryptos.api.dto.CurrencyNormalizedPriceDTO;
import org.cryptos.api.dto.CurrencyStatsMinMaxDTO;
import org.cryptos.service.CurrencyStatsService;
import org.cryptos.service.domain.CurrencyNormalizedPriceDomain;
import org.cryptos.service.domain.CurrencyStatsMinMaxDomain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for managing currency statistics operations.
 */
@RestController
@RequestMapping("/currencies/stats")
@RequiredArgsConstructor
public class CurrencyStatsController {

    private final CurrencyStatsService currencyStatsService;

    /**
     * Uploads a file containing currency statistics.
     * The file must be in multipart form data format, and the currency must be pre-created.
     *
     * @param file the file containing currency statistics in CSV format
     */
    @Operation(summary = "Upload a file with currency statistics", description = "Uploads a file containing currency statistics. " +
            "The file must be in multipart form data format. The currency must be pre-created."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New currency statistics successfully uploaded"),
            @ApiResponse(responseCode = "400", description = "CSV file must contain exactly 3 columns per line"),
            @ApiResponse(responseCode = "404", description = "currency not found, need to enable the currency first"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),})
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void createStats(@RequestParam("file") MultipartFile file) {
        currencyStatsService.createStats(file.getResource());
    }

    /**
     * Get the statistics for the specific currency for an optional date range.
     *
     * @param name          the name of the currency
     * @param startDateTime optional start date for filtering the stats
     * @param endDateTime   optional end date for filtering the stats
     * @return the statistics of the specified currency
     */
    @Operation(summary = "Get currency stats by name",
            description = "Fetches the statistics of a specific currency. Optional filters for start and end date are available.")
    @Parameter(name = "name", description = "The name of the currency to get")
    @Parameter(name = "startDateTime", description = "Optional start date for filtering the stats (Using example: 2020-12-31T23:50:51)." +
            "Default value is current date.")
    @Parameter(name = "endDateTime", description = "Optional end date for filtering the stats (Example: 2020-12-31T23:50:51)." +
            "Default values is 'current date - 30 days'")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The currency statistics successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "The start date '2022-05-02T10:00' must be before the end date '2022-02-02T10:00'"),
            @ApiResponse(responseCode = "400", description = "Failed to convert 'startDateTime' with value: '2022-05-02T25:00:01'"),
            @ApiResponse(responseCode = "404", description = "currency 'X' not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),})
    @GetMapping("/{name}")
    @ResponseStatus(HttpStatus.OK)
    public CurrencyStatsMinMaxDTO getOneCurrencyStats(@PathVariable String name,
                                                      @RequestParam(value = "startDateTime", required = false) LocalDateTime startDateTime,
                                                      @RequestParam(value = "endDateTime", required = false) LocalDateTime endDateTime) {
        return convertStatsToDTO(currencyStatsService.getCurrencyStats(name, startDateTime, endDateTime));
    }

    /**
     * Get the list of all currencies with their normalized prices in descending order.
     * Optional filters for start and end dates are available.
     *
     * @param startDateTime optional start date for filtering the stats
     * @param endDateTime   optional end date for filtering the stats
     * @return a list of currencies with their normalized prices
     */
    @Operation(summary = "Get all currencies with normalized prices",
            description = "Fetches the list of all currencies with their normalized prices in descending order. " +
                    "Optional filters for start and end date are available.")
    @Parameter(name = "startDateTime", description = "Optional start date for filtering the stats (Using example: 2020-12-31T23:50:51)." +
            "Default value is current date.")
    @Parameter(name = "endDateTime", description = "Optional end date for filtering the stats (Example: 2020-12-31T23:50:51)." +
            "Default values is 'current date - 30 days'")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The currency statistics successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "The start date '2022-05-02T10:00' must be before the end date '2022-02-02T10:00'"),
            @ApiResponse(responseCode = "400", description = "Failed to convert 'startDateTime' with value: '2022-05-02T25:00:01'"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),})
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CurrencyNormalizedPriceDTO> getAllCurrenciesNormalized(
            @RequestParam(value = "startDateTime", required = false) LocalDateTime startDateTime,
            @RequestParam(value = "endDateTime", required = false) LocalDateTime endDateTime) {
        return currencyStatsService.getAllCurrenciesNormalized(startDateTime, endDateTime).stream()
                .map(this::convertCurrenciesNormalizedToDTO)
                .toList();
    }

    /**
     * Get the highest normalized price for a specific day of all currencies.
     *
     * @param day optional day to retrieve the highest normalized price
     * @return the highest normalized price for the specified day
     */
    @Operation(summary = "Get the highest normalized price for a specific day",
            description = "Fetches the highest normalized price for a specific day of all currencies. " +
                    "If no day is provided, the current day will be used."
    )
    @Parameter(name = "day", description = "Optional day to retrieve the highest normalized price (example 2022-12-31). Default is today")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The statistics successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Failed to convert 'day' with value: '2022-01-32"),
            @ApiResponse(responseCode = "404", description = "Prices not found for the day '2025-12-31'"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),})
    @GetMapping("/highest")
    @ResponseStatus(HttpStatus.OK)
    public CurrencyNormalizedPriceDTO getHighestNormalizedPriceForDay(
            @RequestParam(value = "day", required = false) LocalDate day) {
        return convertCurrenciesNormalizedToDTO(currencyStatsService.getHighestNormalizedPriceForDay(day));
    }

    /**
     * Converts business entity {@link CurrencyStatsMinMaxDomain} to DTO {@link CurrencyStatsMinMaxDTO}.
     *
     * @param domain the {@link CurrencyStatsMinMaxDomain} to be converted
     * @return the corresponding {@link CurrencyStatsMinMaxDTO}
     */
    private CurrencyStatsMinMaxDTO convertStatsToDTO(CurrencyStatsMinMaxDomain domain) {
        return new CurrencyStatsMinMaxDTO(
                domain.symbol(),
                domain.oldestDate(),
                domain.newestDate(),
                domain.minPrice(),
                domain.maxPrice()
        );
    }

    /**
     * Converts business entity {@link CurrencyNormalizedPriceDomain} to DTO {@link CurrencyNormalizedPriceDTO}.
     *
     * @param domain the {@link CurrencyNormalizedPriceDomain} to be converted
     * @return the corresponding {@link CurrencyNormalizedPriceDTO}
     */
    private CurrencyNormalizedPriceDTO convertCurrenciesNormalizedToDTO(CurrencyNormalizedPriceDomain domain) {
        return new CurrencyNormalizedPriceDTO(
                domain.symbol(),
                domain.normalizedPrice()
        );
    }
}
