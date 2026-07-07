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
                    throw new TransferException("Account service rejected %s operation".formatted(action));
                })
                .body(AccountDto.class);
    }
}
