package com.example.cashservice.client;

import com.example.cashservice.dto.NotificationRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class NotificationClient {

    private final RestClient restClient;

    public NotificationClient(@Qualifier("notificationServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void notify(String login, String eventType, BigDecimal amount, String message) {
        restClient.post()
                .uri("/notifications")
                .body(new NotificationRequest(login, eventType, amount, message))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Notification service rejected cash notification");
                })
                .toBodilessEntity();
    }
}
