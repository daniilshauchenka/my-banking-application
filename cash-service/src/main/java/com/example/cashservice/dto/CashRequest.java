package com.example.cashservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashRequest(
        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount
) {
}
