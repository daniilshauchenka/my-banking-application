package ru.yandex.practicum.mybankfront.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AmountRequest;
import ru.yandex.practicum.mybankfront.controller.dto.TransferRequest;
import ru.yandex.practicum.mybankfront.controller.dto.UpdateAccountRequest;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BankClient {

    private final RestClient restClient;

    public BankClient(RestClient.Builder restClientBuilder, @Value("${gateway.url}") String gatewayUrl) {
        this.restClient = restClientBuilder.baseUrl(gatewayUrl).build();
    }

    public AccountDto getAccount(Long accountId) {
        return restClient.get()
                .uri("/accounts/{accountId}", accountId)
                .retrieve()
                .body(AccountDto.class);
    }

    public List<AccountDto> getAccounts() {
        return restClient.get()
                .uri("/accounts")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public void updateAccount(Long accountId, UpdateAccountRequest request) {
        restClient.put()
                .uri("/accounts/{accountId}", accountId)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw new IllegalStateException("Cannot update account");
                })
                .toBodilessEntity();
    }

    public void deposit(Long accountId, BigDecimal amount) {
        patchCash(accountId, "deposit", amount);
    }

    public void withdraw(Long accountId, BigDecimal amount) {
        patchCash(accountId, "withdraw", amount);
    }

    public void transfer(TransferRequest request) {
        restClient.post()
                .uri("/transfers")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw new IllegalStateException("Cannot transfer money");
                })
                .toBodilessEntity();
    }

    private void patchCash(Long accountId, String action, BigDecimal amount) {
        restClient.patch()
                .uri("/cash/{accountId}/{action}", accountId, action)
                .body(new AmountRequest(amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw new IllegalStateException("Cannot change cash balance");
                })
                .toBodilessEntity();
    }
}
