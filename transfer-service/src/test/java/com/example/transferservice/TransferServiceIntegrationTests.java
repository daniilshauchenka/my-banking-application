package com.example.transferservice;

import com.example.transferservice.dto.TransferRequest;
import com.example.transferservice.dto.TransferResponse;
import com.example.transferservice.model.TransferStatus;
import com.example.transferservice.repository.TransferRecordRepository;
import com.example.transferservice.service.TransferService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransferServiceIntegrationTests {

    private static HttpServer accountServer;
    private static final List<String> accountCalls = new ArrayList<>();

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransferRecordRepository transferRecordRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        accountServer = HttpServer.create(new InetSocketAddress(0), 0);
        accountServer.createContext("/accounts/1/withdraw", exchange -> respondJson(exchange, """
                {"id":1,"login":"user1","name":"user 1","birthdate":"2001-01-01","balance":90.00}
                """));
        accountServer.createContext("/accounts/2/deposit", exchange -> respondJson(exchange, """
                {"id":2,"login":"user2","name":"user 2","birthdate":"1998-05-12","balance":260.00}
                """));
        accountServer.start();
        registry.add("account-service.url", () -> "http://localhost:" + accountServer.getAddress().getPort());
    }

    @BeforeEach
    void setUp() {
        accountCalls.clear();
        transferRecordRepository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        accountServer.stop(0);
    }

    @Test
    void transferWithdrawsFromSenderAndDepositsToReceiver() {
        TransferResponse response = transferService.transfer(new TransferRequest(1L, 2L, BigDecimal.TEN));

        assertThat(response.fromAccountId()).isEqualTo(1L);
        assertThat(response.toAccountId()).isEqualTo(2L);
        assertThat(response.fromBalance()).isEqualByComparingTo("90.00");
        assertThat(response.toBalance()).isEqualByComparingTo("260.00");
        assertThat(accountCalls).containsExactly("/accounts/1/withdraw", "/accounts/2/deposit");
        assertThat(transferRecordRepository.findAll()).singleElement().satisfies(record -> {
            assertThat(record.getFromAccountId()).isEqualTo(1L);
            assertThat(record.getToAccountId()).isEqualTo(2L);
            assertThat(record.getStatus()).isEqualTo(TransferStatus.COMPLETED);
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
}
