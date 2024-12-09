package org.cryptos.persistence.repository;

import org.cryptos.persistence.entity.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for run CRUD operations on the {@link CurrencyEntity}.
 * This interface extends {@link JpaRepository} to provide methods for interacting with the
 * "currency" table in the database.
 * Custom queries can be added to this interface if needed.
 */
public interface CurrencyRepository extends JpaRepository<CurrencyEntity, String> {
}
