package com.example.accountservice;

import com.example.accountservice.dto.CreateAccountRequest;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.service.AccountService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AccountNotificationIntegrationTests {

    private static final String TOPIC = "account.notifications.test";

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.notifications-topic", () -> TOPIC);
    }

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void createAccountSendsNotificationToKafka() {
        try (Consumer<String, String> consumer = createStringConsumer()) {
            consumer.subscribe(List.of(TOPIC));

            Long accountId = accountService.create(new CreateAccountRequest(
                    "user1",
                    "user 1",
                    LocalDate.parse("2001-01-01"),
                    new BigDecimal("100.00")
            )).id();

            String body = KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10)).value();

            assertThat(body).contains("\"accountId\":%s".formatted(accountId));
            assertThat(body).contains("\"eventType\":\"ACCOUNT_CREATED\"");
            assertThat(body).contains("\"amount\":100.00");
        }
    }

    private static Consumer<String, String> createStringConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                KAFKA.getBootstrapServers(),
                "account-notifications-test-" + UUID.randomUUID(),
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
