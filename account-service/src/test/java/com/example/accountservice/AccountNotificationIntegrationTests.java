package com.example.accountservice;

import com.example.accountservice.dto.CreateAccountRequest;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.service.AccountService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccountNotificationIntegrationTests {

    private static HttpServer notificationServer;
    private static final List<String> notificationBodies = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> notificationPaths = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        startNotificationServer();
        registry.add("notification-service.url", () -> "http://localhost:" + notificationServer.getAddress().getPort());
    }

    @BeforeAll
    static void startNotificationServer() throws IOException {
        if (notificationServer != null) {
            return;
        }
        notificationServer = HttpServer.create(new InetSocketAddress(0), 0);
        notificationServer.createContext("/notifications", AccountNotificationIntegrationTests::handleNotification);
        notificationServer.start();
    }

    @AfterAll
    static void stopNotificationServer() {
        if (notificationServer != null) {
            notificationServer.stop(0);
        }
    }

    @BeforeEach
    void setUp() {
        notificationBodies.clear();
        notificationPaths.clear();
        accountRepository.deleteAll();
    }

    @Test
    void createAccountSendsNotification() {
        Long accountId = accountService.create(new CreateAccountRequest(
                "user1",
                "user 1",
                LocalDate.parse("2001-01-01"),
                new BigDecimal("100.00")
        )).id();

        assertThat(notificationPaths).containsExactly("/notifications");
        assertThat(notificationBodies).singleElement()
                .satisfies(body -> {
                    assertThat(body).contains("\"accountId\":%s".formatted(accountId));
                    assertThat(body).contains("\"eventType\":\"ACCOUNT_CREATED\"");
                    assertThat(body).contains("\"amount\":100.00");
                });
    }

    private static void handleNotification(HttpExchange exchange) throws IOException {
        notificationPaths.add(exchange.getRequestURI().getPath());
        notificationBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        exchange.sendResponseHeaders(202, -1);
        exchange.close();
    }
}
