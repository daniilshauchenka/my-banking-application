package com.example.cashservice.client;

import com.example.cashservice.dto.AccountDto;
import com.example.cashservice.dto.AmountRequest;
import com.example.cashservice.exception.CashException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class AccountClient {

    @Qualifier("accountServiceRestClient")
    private final RestClient restClient;

    public AccountDto deposit(Long accountId, BigDecimal amount) {
        return patchAmount(accountId, "deposit", amount);
    }

    public AccountDto withdraw(Long accountId, BigDecimal amount) {
        return patchAmount(accountId, "withdraw", amount);
    }

    private AccountDto patchAmount(Long accountId, String action, BigDecimal amount) {
        return restClient.patch()
                .uri("/accounts/{accountId}/{action}", accountId, action)
                .body(new AmountRequest(amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new CashException("Account service rejected %s operation".formatted(action));
                })
                .body(AccountDto.class);
    }
}
