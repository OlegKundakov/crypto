package org.cryptos.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a currency entity in the system, is mapped to the "currency" table in the database
 */
@Entity
@Table(name = "currency")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyEntity {
    /**
     * Unique symbol of the currency ("BTC", "ETH").
     */
    @Id
    @Column(nullable = false, unique = true, updatable = false)
    private String symbol;
}
