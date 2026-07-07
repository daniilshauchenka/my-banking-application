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

    public AccountDto getAccount(String login) {
        return restClient.get()
                .uri("/accounts/{login}", login)
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

    public void updateAccount(String login, UpdateAccountRequest request) {
        restClient.put()
                .uri("/accounts/{login}", login)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw new IllegalStateException("Cannot update account");
                })
                .toBodilessEntity();
    }

    public void deposit(String login, BigDecimal amount) {
        patchCash(login, "deposit", amount);
    }

    public void withdraw(String login, BigDecimal amount) {
        patchCash(login, "withdraw", amount);
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

    private void patchCash(String login, String action, BigDecimal amount) {
        restClient.patch()
                .uri("/cash/{login}/{action}", login, action)
                .body(new AmountRequest(amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw new IllegalStateException("Cannot change cash balance");
                })
                .toBodilessEntity();
    }
}
