package com.example.notificationservice;

import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.scheduler.NotificationScheduler;
import com.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.notifications.scheduler-delay=600000")
class NotificationSchedulerTests {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationScheduler notificationScheduler;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void schedulerMarksStoredNotificationAsProcessed() {
        notificationService.notify(new NotificationRequest(
                1L,
                "CASH_DEPOSIT",
                new BigDecimal("10.00"),
                "Cash deposit completed"
        ));

        assertThat(notificationRepository.findAll()).singleElement()
                .satisfies(notification -> assertThat(notification.isProcessed()).isFalse());

        notificationScheduler.processNotifications();

        assertThat(notificationRepository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.isProcessed()).isTrue();
            assertThat(notification.getProcessedAt()).isNotNull();
        });
    }
}
