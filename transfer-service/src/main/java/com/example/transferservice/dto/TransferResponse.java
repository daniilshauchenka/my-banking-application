package com.example.transferservice.dto;

import java.math.BigDecimal;

public record TransferResponse(
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        BigDecimal fromBalance,
        BigDecimal toBalance
) {
}
