package com.example.cashservice.client;

import com.example.cashservice.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class NotificationClient {

    @Qualifier("notificationServiceRestClient")
    private final RestClient restClient;

    public void notify(Long accountId, String eventType, BigDecimal amount, String message) {
        restClient.post()
                .uri("/notifications")
                .body(new NotificationRequest(accountId, eventType, amount, message))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Notification service rejected cash notification");
                })
                .toBodilessEntity();
    }
}
