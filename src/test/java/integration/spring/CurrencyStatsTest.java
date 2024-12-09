package integration.spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cryptos.CryptosApplication;
import org.cryptos.api.dto.CurrencyDTO;
import org.cryptos.api.dto.CurrencyNormalizedPriceDTO;
import org.cryptos.api.dto.CurrencyStatsMinMaxDTO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
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
import org.springframework.util.LinkedMultiValueMap;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("h2")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = CryptosApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CurrencyStatsTest {

    private static final String BASE_CURRENCY_URL = "/currencies";
    private static final String BASE_STATS_URL = BASE_CURRENCY_URL + "/stats";
    private static final String GET_ONE_URL = BASE_STATS_URL + "/{symbol}";

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @LocalServerPort
    private int port;

    /**
     * Test creates stats for a currency successfully by posting valid stats and verifying the result.
     */
    @Test
    void createStatsForCurrencySuccessfully() throws IOException {
        String currencySymbol = "BTC";
        postValidOneCurrencyEntityAndVerify(currencySymbol);
        postValidStatsAndVerify("csv/btc_valid.csv");
        getOneCurrencyStatsAndVerify(currencySymbol);
    }

    /**
     * Test fails to create stats for a currency that does not exist in the database.
     */
    @Test
    void failToCreateStatsForCurrencyThatNotExistsInDB() {
        failToPostStatsBecauseCurrencyNotFound();
    }

    /**
     * Test fails to create stats when the provided CSV is invalid.
     */
    @Test
    void failToCreateStatsCSVInvalid() throws JsonProcessingException {
        String currencySymbol = "BTC";
        postValidOneCurrencyEntityAndVerify(currencySymbol);
        failToPostStatsBecauseInvalidCSV();
    }

    /**
     * Test retrieves all currencies with normalized stats by posting valid stats for multiple currencies
     * and verifying the result.
     */
    @Test
    void getAllCurrenciesNormalized() throws JsonProcessingException {
        String btc = "BTC";
        String eth = "ETH";
        postValidOneCurrencyEntityAndVerify(btc);
        postValidOneCurrencyEntityAndVerify(eth);
        postValidStatsAndVerify("csv/btc_valid.csv");
        postValidStatsAndVerify("csv/eth_valid.csv");
        getAllCurrenciesNormalizedAndVerify();

    }

    @Disabled
    @Test
    void getHighestNormalizedPriceForDay() throws JsonProcessingException {
        String btc = "BTC";
        String eth = "ETH";
        postValidOneCurrencyEntityAndVerify(btc);
        postValidOneCurrencyEntityAndVerify(eth);
        postValidStatsAndVerify("csv/btc_valid.csv");
        postValidStatsAndVerify("csv/eth_valid.csv");
        getHighestNormalizedPriceForDayAndVerify();
    }


    private void postValidOneCurrencyEntityAndVerify(String currencySymbol) throws JsonProcessingException {
        //given
        String jsonRequestBody = objectMapper.writeValueAsString(new CurrencyDTO(currencySymbol));
        HttpEntity<String> requestEntity = createJsonHttpEntityWithBody(jsonRequestBody);
        //when
        ResponseEntity<Void> response = testRestTemplate.postForEntity(BASE_CURRENCY_URL, requestEntity, Void.class);
        //then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    private void postValidStatsAndVerify(String fileName) {
        //given
        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("file", new ClassPathResource(fileName));
        var headers = createMultipartFormDataHeaders();
        HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);
        //when
        ResponseEntity<Void> response = testRestTemplate.exchange(BASE_STATS_URL, HttpMethod.POST, entity, Void.class);
        //then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    private void failToPostStatsBecauseCurrencyNotFound() {
        //given
        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("file", new ClassPathResource("csv/btc_valid.csv"));
        var headers = createMultipartFormDataHeaders();
        HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);
        //when
        ResponseEntity<ProblemDetail> response = testRestTemplate.exchange(BASE_STATS_URL, HttpMethod.POST, entity, ProblemDetail.class);
        //then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Currency not found, need to enable the currency first", response.getBody().getDetail());
    }

    private void failToPostStatsBecauseInvalidCSV() {
        //given
        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("file", new ClassPathResource("csv/invalid_csv.csv"));
        var headers = createMultipartFormDataHeaders();
        HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);
        //when
        ResponseEntity<ProblemDetail> response = testRestTemplate.exchange(BASE_STATS_URL, HttpMethod.POST, entity, ProblemDetail.class);
        //then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CSV file must contain exactly 3 columns per line", response.getBody().getDetail());
    }

    private void getOneCurrencyStatsAndVerify(String currencySymbol) {
        var startDateTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        var endDateTime = LocalDateTime.of(2030, 1, 1, 0, 0);
        String url = BASE_STATS_URL + "/" + currencySymbol + "?startDateTime=" + startDateTime + "&endDateTime=" + endDateTime;
        //when
        ResponseEntity<CurrencyStatsMinMaxDTO> responseEntity = testRestTemplate.getForEntity(url, CurrencyStatsMinMaxDTO.class, currencySymbol);
        //then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        CurrencyStatsMinMaxDTO body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(currencySymbol, body.symbol());
        assertEquals(0, new BigDecimal("20").compareTo(body.minPrice()));
        assertEquals(0, new BigDecimal("50").compareTo(body.maxPrice()));
    }

    private void getAllCurrenciesNormalizedAndVerify() {
        var startDateTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        var endDateTime = LocalDateTime.of(2030, 1, 1, 0, 0);
        String url = BASE_STATS_URL + "?startDateTime=" + startDateTime + "&endDateTime=" + endDateTime;
        //when
        var type = new ParameterizedTypeReference<List<CurrencyNormalizedPriceDTO>>() {
        };
        ResponseEntity<List<CurrencyNormalizedPriceDTO>> responseEntity = testRestTemplate.exchange(url, HttpMethod.GET, null, type);
        //then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<CurrencyNormalizedPriceDTO> body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        CurrencyNormalizedPriceDTO ethNormalizedDto = body.getFirst();
        assertEquals("ETH", ethNormalizedDto.symbol());
        assertEquals(0, new BigDecimal("4.0").compareTo(ethNormalizedDto.normalizedPrice()));
        CurrencyNormalizedPriceDTO btcNormalizedDto = body.getLast();
        assertEquals("BTC", btcNormalizedDto.symbol());
        assertEquals(0, new BigDecimal("1.5").compareTo(btcNormalizedDto.normalizedPrice()));
    }

    private void getHighestNormalizedPriceForDayAndVerify() {
        var requiredDay = LocalDate.of(2024, 1, 3);
        String url = BASE_STATS_URL + "/highest?day=" + requiredDay;
        //when
        ResponseEntity<CurrencyNormalizedPriceDTO> responseEntity = testRestTemplate.exchange(url, HttpMethod.GET, null, CurrencyNormalizedPriceDTO.class);
        //then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        CurrencyNormalizedPriceDTO body = responseEntity.getBody();
        assertNotNull(body);
    }

    private HttpEntity<String> createJsonHttpEntityWithBody(String jsonRequestBody) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new HttpEntity<>(jsonRequestBody, headers);
    }

    private HttpHeaders createMultipartFormDataHeaders() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }
}
