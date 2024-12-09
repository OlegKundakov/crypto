package org.cryptos.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a currency statistics entity in the system, is mapped to the "currency_stats" table in the database.
 * It holds information about the price at the specific date and time.
 * Default btree indexes are created on the "date_time", "currency_id", and "price" columns for improved query performance.
 * It's required for rapid filtering/search in persistence layer.
 *
 */
@Entity
@Table(
        name = "currency_stats",
        indexes = {
                @Index(name = "idx_date_time", columnList = "date_time"),
                @Index(name = "idx_currency_id", columnList = "currency_id"),
                @Index(name = "idx_price", columnList = "price")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyStatsEntity {

    /**
     * Unique identifier for the currency statistics entry.
     */
    @Id
    @Column(nullable = false, unique = true, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Not nullable date and time when the currency price was actual.
     */
    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    /**
     * The currency associated with this statistic record.
     * The relation is many-to-one with the {@link CurrencyEntity}.
     * The foreign key constraint is defined by "fk_currency_id" and refers to "currency" table.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_id", nullable = false, foreignKey = @ForeignKey(name = "fk_currency_id"))
    private CurrencyEntity currency;

    /**
     * Not nullable price of the currency at the specified date and time.
     */
    @Column(name = "price", nullable = false, precision = 25, scale = 7)
    private BigDecimal price;

}
