package com.example.notificationservice;

import com.example.notificationservice.repository.NotificationRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class NotificationKafkaIntegrationTests {

    private static final String TOPIC = "notification.consumer.test";

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @Autowired
    private NotificationRepository notificationRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.listener.auto-startup", () -> "true");
        registry.add("app.kafka.notifications-topic", () -> TOPIC);
    }

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void consumesKafkaNotificationAndStoresIt() {
        send("""
                {"accountId":1,"eventType":"CASH_DEPOSIT","amount":50.00,"message":"Cash deposit completed"}
                """);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(notificationRepository.findAll()).singleElement().satisfies(notification -> {
                    assertThat(notification.getAccountId()).isEqualTo(1L);
                    assertThat(notification.getEventType()).isEqualTo("CASH_DEPOSIT");
                    assertThat(notification.getAmount()).isEqualByComparingTo("50.00");
                    assertThat(notification.getMessage()).isEqualTo("Cash deposit completed");
                })
        );
    }

    private static void send(String body) {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.CLIENT_ID_CONFIG, "notification-consumer-test-" + UUID.randomUUID()
        );
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(TOPIC, "1", body)).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka test producer was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka test producer failed", exception);
        }
    }
}
