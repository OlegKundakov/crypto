package org.cryptos.service;

import org.cryptos.persistence.entity.CurrencyEntity;
import org.cryptos.persistence.repository.CurrencyRepository;
import org.cryptos.service.domain.CurrencyDomain;
import org.cryptos.service.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;
    @InjectMocks
    private CurrencyService currencyService;

    /**
     * Verifies that the repository's save method is called with the correct entity.
     * Since the repository is mocked, no actual database interaction occurs.
     * Ensures that the save method is called.
     */
    @Test
    void shouldCreateCurrency() {
        // given
        var domain = new CurrencyDomain("BTC");
        var entity = new CurrencyEntity("BTC");
        when(currencyRepository.save(any(CurrencyEntity.class))).thenReturn(entity);
        // when
        currencyService.create(domain);
        // then
        verify(currencyRepository).save(any(CurrencyEntity.class));
    }

    /**
     * Verifies that the correct currency domain object is returned.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void shouldReturnCurrencyWhenExists() {
        // given
        var entity = new CurrencyEntity("BTC");
        var domain = new CurrencyDomain("BTC");
        when(currencyRepository.findById("BTC")).thenReturn(Optional.of(entity));
        // when
        CurrencyDomain result = currencyService.getOne("BTC");
        // then
        assertEquals(domain, result);
    }

    /**
     * Verifies that an EntityNotFoundException is thrown with the expected message.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void shouldThrowWhenCurrencyNotFound() {
        // given
        when(currencyRepository.findById("BTC")).thenReturn(Optional.empty());
        // when & then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            currencyService.getOne("BTC");
        });
        assertEquals("Currency 'BTC' not found", exception.getMessage());
    }

    /**
     * Verifies that a list of all currencies is returned correctly.
     * The repository is mocked, so no actual database query occurs.
     */
    @Test
    void shouldReturnAllCurrencies() {
        // given
        var entity1 = new CurrencyEntity("BTC");
        var entity2 = new CurrencyEntity("ETH");
        when(currencyRepository.findAll()).thenReturn(List.of(entity1, entity2));
        // when
        List<CurrencyDomain> result = currencyService.getAll();
        // then
        assertEquals(2, result.size());
        assertEquals("BTC", result.get(0).symbol());
        assertEquals("ETH", result.get(1).symbol());
    }
}