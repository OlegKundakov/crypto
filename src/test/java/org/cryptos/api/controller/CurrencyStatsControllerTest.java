package org.cryptos.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cryptos.service.CurrencyStatsService;
import org.cryptos.service.domain.CurrencyNormalizedPriceDomain;
import org.cryptos.service.domain.CurrencyStatsMinMaxDomain;
import org.cryptos.service.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(CurrencyStatsController.class)
class CurrencyStatsControllerTest {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @MockitoBean
    private CurrencyStatsService currencyStatsService;

    @Autowired
    private MockMvc mockMvc;

    /**
     * Sends multipart POST request with a CSV file and verifies the response status (201).
     * Also verifies that the service's createStats method is called.
     */
    @Test
    void createStatsSuccessfully() throws Exception {
        //given
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", MediaType.MULTIPART_FORM_DATA_VALUE, "content".getBytes());
        //when & then
        mockMvc.perform(multipart("/currencies/stats")
                        .file(file))
                .andExpect(status().isCreated());
        verify(currencyStatsService).createStats(any(Resource.class));
    }

    /**
     * Sends GET request with currency symbol and start/end dates and verifies the response (200) with correct json body.
     */
    @Test
    void getOneCurrencyStatsWithBothDatesSuccessfully() throws Exception {
        //given
        String name = "BTC";
        LocalDateTime startDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2024, 1, 2, 0, 0, 0);
        var domain = new CurrencyStatsMinMaxDomain(
                "BTC", startDateTime, endDateTime, BigDecimal.valueOf(50), BigDecimal.valueOf(51));
        when(currencyStatsService.getCurrencyStats(name, startDateTime, endDateTime))
                .thenReturn(domain);
        //when & then
        mockMvc.perform(get("/currencies/stats/{name}", name)
                        .param("startDateTime", startDateTime.toString())
                        .param("endDateTime", endDateTime.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value(domain.symbol()))
                .andExpect(jsonPath("$.oldestDate").value(formatter.format(domain.oldestDate())))
                .andExpect(jsonPath("$.newestDate").value(formatter.format(domain.newestDate())))
                .andExpect(jsonPath("$.minPrice").value(domain.minPrice().toString()))
                .andExpect(jsonPath("$.maxPrice").value(domain.maxPrice().toString()));
    }

    /**
     * Sends a GET request for currency statistics without dates parameters and verifies the response (200)
     * with correct json body.
     */
    @Test
    void getOneCurrencyStatsWithNoDatesSuccessfully() throws Exception {
        //given
        String name = "BTC";
        LocalDateTime oldestDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0, 0);
        LocalDateTime newestDate = LocalDateTime.of(2024, 1, 2, 0, 0, 0, 0);
        var domain = new CurrencyStatsMinMaxDomain(
                "BTC", oldestDate, newestDate, BigDecimal.valueOf(50), BigDecimal.valueOf(51));
        when(currencyStatsService.getCurrencyStats(name, null, null))
                .thenReturn(domain);
        //when & then
        mockMvc.perform(get("/currencies/stats/{name}", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value(domain.symbol()))
                .andExpect(jsonPath("$.oldestDate").value(formatter.format(domain.oldestDate())))
                .andExpect(jsonPath("$.newestDate").value(formatter.format(domain.newestDate())))
                .andExpect(jsonPath("$.minPrice").value(domain.minPrice().toString()))
                .andExpect(jsonPath("$.maxPrice").value(domain.maxPrice().toString()));
    }

    /**
     * Sends GET request with invalid parameters and verifies that an EntityNotFoundException is thrown,
     * HTTP 404 status is returned with reason message.
     */
    @Test
    void throwEntityNotFoundExceptionWhenServiceThrows() throws Exception {
        //given
        String name = "BTC";
        LocalDateTime startDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2024, 1, 2, 0, 0, 0, 0);
        when(currencyStatsService.getCurrencyStats(name, startDateTime, endDateTime))
                .thenThrow(new EntityNotFoundException("BTC not found"));
        //when & then
        mockMvc.perform(get("/currencies/stats/{name}", name)
                        .param("startDateTime", startDateTime.toString())
                        .param("endDateTime", endDateTime.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("BTC not found"));
    }

    /**
     * Sends GET request to retrieve normalized statistics for all currencies and verifies the response (200)
     * with correctc Json array
     */
    @Test
    void getAllCurrenciesNormalizedSuccessfully() throws Exception {
        //given
        LocalDateTime startDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2024, 1, 2, 0, 0, 0);
        when(currencyStatsService.getAllCurrenciesNormalized(startDateTime, endDateTime))
                .thenReturn(List.of(
                        new CurrencyNormalizedPriceDomain("BTC", BigDecimal.valueOf(0.9)),
                        new CurrencyNormalizedPriceDomain("ETH", BigDecimal.valueOf(0.5))
                ));
        //when & then
        mockMvc.perform(get("/currencies/stats")
                        .param("startDateTime", startDateTime.toString())
                        .param("endDateTime", endDateTime.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[0].normalizedPrice").value(BigDecimal.valueOf(0.9)))
                .andExpect(jsonPath("$[1].symbol").value("ETH"))
                .andExpect(jsonPath("$[1].normalizedPrice").value(BigDecimal.valueOf(0.5)));
    }

    /**
     * Sends GET request with a specific day and verifies the response (200) with the highest normalized price.
     */
    @Test
    void getHighestNormalizedPriceForDay_ShouldReturnHighestNormalizedPrice() throws Exception {
        //given
        LocalDate day = LocalDate.of(2024, 1, 1);
        when(currencyStatsService.getHighestNormalizedPriceForDay(day))
                .thenReturn(new CurrencyNormalizedPriceDomain("BTC", BigDecimal.valueOf(0.1)));
        //when & then
        mockMvc.perform(get("/currencies/stats/highest")
                        .param("day", day.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.normalizedPrice").value(BigDecimal.valueOf(0.1)));
    }
}