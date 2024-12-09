package org.cryptos.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cryptos.api.dto.CurrencyDTO;
import org.cryptos.service.CurrencyService;
import org.cryptos.service.domain.CurrencyDomain;
import org.cryptos.service.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {

    @MockitoBean
    private CurrencyService currencyService;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends POST request to create new currency and verifies the response status 201.
     * Also verifies that the service's create method is called.
     */
    @Test
    void createCurrencySuccessfully() throws Exception {
        // given
        var currencyDTO = new CurrencyDTO("BTC");

        // when & then
        mockMvc.perform(post("/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(currencyDTO)))
                .andExpect(status().isCreated());
        verify(currencyService).create(new CurrencyDomain("BTC"));
    }

    /**
     * Test for handling invalid JSON input when creating a currency.
     * Expects a BadRequest code response 400 with a specific error message.
     */
    @Test
    void currencyNotCreateWhenWrongJson() throws Exception {
        // when & then
        mockMvc.perform(post("/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Failed to read request"))
                .andDo(print());
    }

    /**
     * Sends GET request to get a currency and verifies the response status is 200
     * and that the returned symbol is correct.
     */
    @Test
    void getOneCurrencySuccessfully() throws Exception {
        // given
        when(currencyService.getOne("BTC")).thenReturn(new CurrencyDomain("BTC"));

        // when & then
        mockMvc.perform(get("/currencies/BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"));
    }

    /**
     * Sends GET request for a currency that does not exist and expects a NotFound response (404)
     * with a specific error message.
     */
    @Test
    void currencyNotFound() throws Exception {
        // given
        when(currencyService.getOne("BTC")).thenThrow(new EntityNotFoundException("BTC not found"));

        // when & then
        mockMvc.perform(get("/currencies/BTC"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("BTC not found"));
    }

    /**
     * Sends GET request to retrieve all currencies and verifies the response status (200)
     * and that the response contains the expected currencies.
     */
    @Test
    void getAllCurrenciesSuccessfully() throws Exception {
        // given
        when(currencyService.getAll()).thenReturn(List.of(new CurrencyDomain("BTC"), new CurrencyDomain("ETH")));

        // when & then
        mockMvc.perform(get("/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[1].symbol").value("ETH"));
    }

    /**
     * Sends GET request to retrieve all currencies and expects a successful response (200) list is empty '[]'
     */
    @Test
    void getAllCurrenciesButResultIsEmptyList() throws Exception {
        // given
        when(currencyService.getAll()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}