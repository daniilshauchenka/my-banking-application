package com.example.transferservice.client;

import com.example.transferservice.dto.AccountDto;
import com.example.transferservice.dto.AmountRequest;
import com.example.transferservice.exception.TransferException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(RestClient.Builder restClientBuilder, @Value("${account-service.url}") String accountServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(accountServiceUrl).build();
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
                    throw new TransferException("Account service rejected %s operation".formatted(action));
                })
                .body(AccountDto.class);
    }
}
