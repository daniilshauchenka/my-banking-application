package com.example.cashservice;

import com.example.cashservice.dto.AccountDto;
import com.example.cashservice.model.CashOperationStatus;
import com.example.cashservice.repository.CashOperationRepository;
import com.example.cashservice.service.CashService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CashServiceIntegrationTests {

    private static HttpServer accountServer;
    private static HttpServer notificationServer;
    private static final List<String> accountCalls = new ArrayList<>();
    private static final List<String> notifications = new ArrayList<>();

    @Autowired
    private CashService cashService;

    @Autowired
    private CashOperationRepository cashOperationRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        accountServer = HttpServer.create(new InetSocketAddress(0), 0);
        accountServer.createContext("/accounts/1/deposit", exchange -> respondJson(exchange, """
                {"id":1,"login":"user1","name":"user 1","birthdate":"2001-01-01","balance":105.00}
                """));
        accountServer.createContext("/accounts/1/withdraw", exchange -> respondJson(exchange, """
                {"id":1,"login":"user1","name":"user 1","birthdate":"2001-01-01","balance":95.00}
                """));
        accountServer.start();

        notificationServer = HttpServer.create(new InetSocketAddress(0), 0);
        notificationServer.createContext("/notifications", CashServiceIntegrationTests::recordNotification);
        notificationServer.start();

        registry.add("account-service.url", () -> "http://localhost:" + accountServer.getAddress().getPort());
        registry.add("notification-service.url", () -> "http://localhost:" + notificationServer.getAddress().getPort());
    }

    @BeforeEach
    void setUp() {
        accountCalls.clear();
        notifications.clear();
        cashOperationRepository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        accountServer.stop(0);
        notificationServer.stop(0);
    }

    @Test
    void depositUpdatesAccountAndCreatesNotification() {
        AccountDto account = cashService.deposit(1L, BigDecimal.valueOf(5));

        assertThat(account).isEqualTo(new AccountDto(1L, "user1", "user 1", LocalDate.parse("2001-01-01"), new BigDecimal("105.00")));
        assertThat(accountCalls).containsExactly("/accounts/1/deposit");
        assertThat(notifications).singleElement().satisfies(body -> {
            assertThat(body).contains("\"accountId\":1");
            assertThat(body).contains("\"eventType\":\"CASH_DEPOSIT\"");
        });
        assertThat(cashOperationRepository.findAll()).singleElement().satisfies(operation -> {
            assertThat(operation.getAccountId()).isEqualTo(1L);
            assertThat(operation.getStatus()).isEqualTo(CashOperationStatus.COMPLETED);
        });
    }

    private static void respondJson(HttpExchange exchange, String body) throws IOException {
        accountCalls.add(exchange.getRequestURI().getPath());
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static void recordNotification(HttpExchange exchange) throws IOException {
        notifications.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        exchange.sendResponseHeaders(202, -1);
        exchange.close();
    }
}
