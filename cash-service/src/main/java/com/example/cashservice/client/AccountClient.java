package com.example.cashservice.client;

import com.example.cashservice.dto.AccountDto;
import com.example.cashservice.dto.AmountRequest;
import com.example.cashservice.exception.CashException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(@Qualifier("accountServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public AccountDto deposit(String login, BigDecimal amount) {
        return patchAmount(login, "deposit", amount);
    }

    public AccountDto withdraw(String login, BigDecimal amount) {
        return patchAmount(login, "withdraw", amount);
    }

    private AccountDto patchAmount(String login, String action, BigDecimal amount) {
        return restClient.patch()
                .uri("/accounts/{login}/{action}", login, action)
                .body(new AmountRequest(amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new CashException("Account service rejected %s operation".formatted(action));
                })
                .body(AccountDto.class);
    }
}
