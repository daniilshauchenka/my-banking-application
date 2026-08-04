package com.example.cashservice;

import com.example.cashservice.dto.AccountDto;
import com.example.cashservice.model.CashOperationStatus;
import com.example.cashservice.repository.CashOperationRepository;
import com.example.cashservice.service.CashService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CashServiceIntegrationTests {

    private static final String TOPIC = "cash.notifications.test";

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    private static HttpServer accountServer;
    private static final List<String> accountCalls = new ArrayList<>();

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

        registry.add("account-service.url", () -> "http://localhost:" + accountServer.getAddress().getPort());
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.notifications-topic", () -> TOPIC);
    }

    @BeforeEach
    void setUp() {
        accountCalls.clear();
        cashOperationRepository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        accountServer.stop(0);
    }

    @Test
    void depositUpdatesAccountAndCreatesNotification() {
        try (Consumer<String, String> consumer = createStringConsumer()) {
            consumer.subscribe(List.of(TOPIC));

            AccountDto account = cashService.deposit(1L, BigDecimal.valueOf(5));

            assertThat(account).isEqualTo(new AccountDto(1L, "user1", "user 1", LocalDate.parse("2001-01-01"), new BigDecimal("105.00")));
            assertThat(accountCalls).containsExactly("/accounts/1/deposit");

            String body = KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10)).value();
            assertThat(body).contains("\"accountId\":1");
            assertThat(body).contains("\"eventType\":\"CASH_DEPOSIT\"");

            assertThat(cashOperationRepository.findAll()).singleElement().satisfies(operation -> {
                assertThat(operation.getAccountId()).isEqualTo(1L);
                assertThat(operation.getStatus()).isEqualTo(CashOperationStatus.COMPLETED);
            });
        }
    }

    private static void respondJson(HttpExchange exchange, String body) throws IOException {
        accountCalls.add(exchange.getRequestURI().getPath());
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static Consumer<String, String> createStringConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                KAFKA.getBootstrapServers(),
                "cash-notifications-test-" + UUID.randomUUID(),
                "true"
        );
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer();
    }
}
