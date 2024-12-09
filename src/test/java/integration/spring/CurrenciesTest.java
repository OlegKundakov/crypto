package integration.spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cryptos.CryptosApplication;
import org.cryptos.api.dto.CurrencyDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("h2")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = CryptosApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CurrenciesTest {

    private static final String BASE_URL = "/currencies";
    private static final String GET_ONE_URL = BASE_URL + "/{symbol}";

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @LocalServerPort
    private int port;

    /**
     * Test creates a single currency and verifies it is retrieved correctly.
     */
    @Test
    void postOneCurrencyAndCheck() throws JsonProcessingException {
        String currencySymbol = "BTC";
        postValidOneCurrencyEntityAndVerify(currencySymbol);
        getValidOneCurrencyByNameAndVerify(currencySymbol);
    }

    /**
     * Test tries retrieve the currency that does not exist. Http code 404 is returned.
     */
    @Test
    void failToGetCurrencyThatNotExists() throws JsonProcessingException {
        String currencySymbol = "BTC";
        getNonExistingCurrency(currencySymbol);
    }

    /**
     * Test creates two currencies and verifies they are retrieved in a batch as array
     */
    @Test
    void postTwoCurrenciesAndCheckBatch() throws JsonProcessingException {
        String btcSymbol = "BTC";
        String ethSymbol = "ETH";
        postValidOneCurrencyEntityAndVerify(btcSymbol);
        postValidOneCurrencyEntityAndVerify(ethSymbol);
        getValidCurrencyArrayAndVerify(btcSymbol, ethSymbol);
    }

    /**
     * Test retrieves an empty array of currencies because no currencies exist.
     */
    @Test
    void getEmptyCurrencyArray() {
        getValidCurrencyArrayAndVerify();
    }

    private void postValidOneCurrencyEntityAndVerify(String currencySymbol) throws JsonProcessingException {
        //given
        String jsonRequestBody = objectMapper.writeValueAsString(new CurrencyDTO(currencySymbol));
        HttpEntity<String> requestEntity = createJsonHttpEntityWithBody(jsonRequestBody);
        //when
        ResponseEntity<Void> response = testRestTemplate.postForEntity(BASE_URL, requestEntity, Void.class);
        //then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    private void getValidOneCurrencyByNameAndVerify(String currencySymbol) {
        //when
        ResponseEntity<CurrencyDTO> responseEntity = testRestTemplate.getForEntity(GET_ONE_URL, CurrencyDTO.class, currencySymbol);
        //then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(currencySymbol, responseEntity.getBody().symbol());

    }

    private void getValidCurrencyArrayAndVerify(String... expectedSymbols) {
        //when
        var type = new ParameterizedTypeReference<List<CurrencyDTO>>() {
        };
        ResponseEntity<List<CurrencyDTO>> responseEntity =
                testRestTemplate.exchange(BASE_URL, HttpMethod.GET, null, type);
        //then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(expectedSymbols.length, responseEntity.getBody().size());
        List<String> actualSymbols = responseEntity.getBody().stream().map(CurrencyDTO::symbol).toList();
        assertEquals(List.of(expectedSymbols), actualSymbols);
    }

    private void getNonExistingCurrency(String currencySymbol) throws JsonProcessingException {
        //when
        ResponseEntity<ProblemDetail> responseEntity = testRestTemplate.getForEntity(GET_ONE_URL, ProblemDetail.class, currencySymbol);
        //then
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("Currency 'BTC' not found", responseEntity.getBody().getDetail());
    }

    private HttpEntity<String> createJsonHttpEntityWithBody(String jsonRequestBody) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new HttpEntity<>(jsonRequestBody, headers);
    }
}
