package com.example.transferservice.dto;

import java.math.BigDecimal;

public record TransferResponse(
        String fromLogin,
        String toLogin,
        BigDecimal amount,
        BigDecimal fromBalance,
        BigDecimal toBalance
) {
}
