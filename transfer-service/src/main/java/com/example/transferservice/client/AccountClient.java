package com.example.transferservice.client;

import com.example.transferservice.dto.AccountDto;
import com.example.transferservice.dto.AmountRequest;
import com.example.transferservice.exception.TransferException;
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

    public AccountDto getAccount(Long accountId) {
        return restClient.get()
                .uri("/accounts/{accountId}", accountId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new TransferException("Account service rejected account lookup");
                })
                .body(AccountDto.class);
    }

    public AccountDto deposit(Long accountId, BigDecimal amount) {
        return deposit(accountId, amount, null);
    }

    public AccountDto deposit(Long accountId, BigDecimal amount, String operationId) {
        return patchAmount(accountId, "deposit", amount, operationId);
    }

    public AccountDto withdraw(Long accountId, BigDecimal amount) {
        return withdraw(accountId, amount, null);
    }

    public AccountDto withdraw(Long accountId, BigDecimal amount, String operationId) {
        return patchAmount(accountId, "withdraw", amount, operationId);
    }

    private AccountDto patchAmount(Long accountId, String action, BigDecimal amount, String operationId) {
        return restClient.patch()
                .uri("/accounts/{accountId}/{action}", accountId, action)
                .body(new AmountRequest(amount, operationId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new TransferException("Account service rejected %s operation".formatted(action));
                })
                .body(AccountDto.class);
    }
}
