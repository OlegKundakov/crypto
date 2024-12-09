package org.cryptos.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cryptos.api.dto.CurrencyDTO;
import org.cryptos.service.CurrencyService;
import org.cryptos.service.domain.CurrencyDomain;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing currency operations.
 */
@RestController
@RequestMapping("/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    /**
     * Creates a new currency record.
     *
     * @param currencyDTO the DTO representing the currency to create
     */
    @Operation(summary = "Create a new currency", description = "Creates a new currency record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New currency successfully created"),
            @ApiResponse(responseCode = "400", description = "Failed to read request"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),})
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createCurrency(@Valid @RequestBody CurrencyDTO currencyDTO) {
        currencyService.create(convertToDomain(currencyDTO));
    }

    /**
     * Retrieves a currency by its name.
     *
     * @param name the name of the currency to retrieve
     * @return the DTO representing the retrieved currency
     */
    @Operation(summary = "Get a currency by name", description = "Get a currency by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The currency successfully retrieved"),
            @ApiResponse(responseCode = "404", description = "currency 'X' not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),})
    @Parameter(name = "name", description = "The name of the currency to get")
    @GetMapping("/{name}")
    @ResponseStatus(HttpStatus.OK)
    public CurrencyDTO getOneCurrency(@PathVariable String name) {
        return convertToDTO(currencyService.getOne(name));
    }

    /**
     * Retrieves a list of all currencies.
     *
     * @return a list of DTOs representing all currencies
     */
    @Operation(summary = "Get all currencies", description = "Gets a list of all currencies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The currencies successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),})
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CurrencyDTO> getAllCurrencies() {
        return currencyService.getAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Converts DTO {@link CurrencyDTO} to business entity {@link CurrencyDomain} using one field
     *
     * @param dto the {@link CurrencyDTO} to be converted
     * @return the corresponding {@link CurrencyDomain}
     */
    private CurrencyDomain convertToDomain(CurrencyDTO dto) {
        return new CurrencyDomain(dto.symbol());
    }

    /**
     * Converts business entity {@link CurrencyDomain} to DTO {@link CurrencyDTO} using one field
     *
     * @param domain the {@link CurrencyDomain} to be converted
     * @return the corresponding {@link CurrencyDTO}
     */
    private CurrencyDTO convertToDTO(CurrencyDomain domain) {
        return new CurrencyDTO(domain.symbol());
    }
}
