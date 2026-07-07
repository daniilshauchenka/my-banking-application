package ru.yandex.practicum.mybankfront.controller.dto;

import java.math.BigDecimal;

public record TransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount) {
}
