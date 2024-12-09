package org.cryptos.service;

import lombok.RequiredArgsConstructor;
import org.cryptos.persistence.entity.CurrencyEntity;
import org.cryptos.persistence.repository.CurrencyRepository;
import org.cryptos.service.domain.CurrencyDomain;
import org.cryptos.service.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class responsible for handling operations related to currencies.
 * This class provides methods to create, get one, and get all currencies.
 * The service is annotated with {@link Service} to notice it as a Spring service,
 * and {@link Transactional} to ensure that the operations are executed within a transaction context.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    /**
     * Creates a new currency in the system.
     *
     * @param cryptoCurrency the {@link CurrencyDomain} representing the currency to be created
     */
    public void create(CurrencyDomain cryptoCurrency) {
        currencyRepository.save(new CurrencyEntity(cryptoCurrency.symbol()));
    }
    /**
     * Retrieves a currency by its symbol.
     *
     * @param name the symbol of the currency ("BTC", "ETH")
     * @return {@link CurrencyDomain} representing the currency
     * @throws EntityNotFoundException if no currency is found by the specified symbol
     */
    public CurrencyDomain getOne(String name) {
        return currencyRepository.findById(name)
                .map(this::convertToDomain)
                .orElseThrow(() -> new EntityNotFoundException("Currency '%s' not found".formatted(name)));
    }

    /**
     * Retrieves all currencies in the system.
     *
     * @return a list of {@link CurrencyDomain} representing all currencies. Returns empty array if no currency is found
     */
    public List<CurrencyDomain> getAll() {
        return currencyRepository.findAll()
                .stream()
                .map(this::convertToDomain)
                .toList();
    }

    /**
     * Converts DB entity {@link CurrencyEntity} to business entity {@link CurrencyDomain} using one field
     *
     * @param entity the {@link CurrencyEntity} to be converted
     * @return the corresponding {@link CurrencyDomain}
     */
    private CurrencyDomain convertToDomain(CurrencyEntity entity) {
        return new CurrencyDomain(entity.getSymbol());
    }

}