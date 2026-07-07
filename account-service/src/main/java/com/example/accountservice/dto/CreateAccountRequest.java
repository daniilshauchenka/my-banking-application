package com.example.accountservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateAccountRequest(
        @NotBlank
        String login,

        @NotBlank
        String name,

        @NotNull
        @Past
        LocalDate birthdate,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal balance
) {
}
