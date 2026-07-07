package com.example.cashservice.model;

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
@Table(name = "cash_operations")
public class CashOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login", nullable = false, length = 64)
    private String login;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private CashOperationType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CashOperationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CashOperation() {
    }

    public CashOperation(String login, CashOperationType type, BigDecimal amount, CashOperationStatus status, String errorMessage) {
        this.login = login;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = Instant.now();
    }
}
