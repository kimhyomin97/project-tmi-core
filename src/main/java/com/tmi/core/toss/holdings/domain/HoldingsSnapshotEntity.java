package com.tmi.core.toss.holdings.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "toss_holdings_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_toss_holdings_snapshot__account_time",
                columnNames = {"account_seq", "snapshot_at"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoldingsSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_seq", nullable = false)
    private Long accountSeq;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_purchase_krw", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalPurchaseKrw;

    @Column(name = "total_purchase_usd", precision = 20, scale = 4)
    private BigDecimal totalPurchaseUsd;

    @Column(name = "market_value_krw", nullable = false, precision = 20, scale = 4)
    private BigDecimal marketValueKrw;

    @Column(name = "market_value_usd", precision = 20, scale = 4)
    private BigDecimal marketValueUsd;

    @Column(name = "profit_loss_krw", nullable = false, precision = 20, scale = 4)
    private BigDecimal profitLossKrw;

    @Column(name = "profit_loss_usd", precision = 20, scale = 4)
    private BigDecimal profitLossUsd;

    @Column(name = "daily_pl_krw", nullable = false, precision = 20, scale = 4)
    private BigDecimal dailyPlKrw;

    @Column(name = "daily_pl_usd", precision = 20, scale = 4)
    private BigDecimal dailyPlUsd;

    @Column(name = "profit_loss_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal profitLossRate;

    @Column(name = "daily_pl_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal dailyPlRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", nullable = false, columnDefinition = "jsonb")
    private String rawResponse;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;
}
