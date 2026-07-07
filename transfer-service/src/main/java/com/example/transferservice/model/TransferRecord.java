package com.example.transferservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfer_records")
public class TransferRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_login", nullable = false, length = 64)
    private String fromLogin;

    @Column(name = "to_login", nullable = false, length = 64)
    private String toLogin;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransferStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TransferRecord() {
    }

    public TransferRecord(String fromLogin, String toLogin, BigDecimal amount, TransferStatus status, String errorMessage) {
        this.fromLogin = fromLogin;
        this.toLogin = toLogin;
        this.amount = amount;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }
}
