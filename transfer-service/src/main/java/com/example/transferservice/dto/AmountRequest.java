package com.example.transferservice.dto;

import java.math.BigDecimal;

public record AmountRequest(BigDecimal amount, String operationId) {
}
