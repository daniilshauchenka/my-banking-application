package com.example.notificationservice.scheduler;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationRepository notificationRepository;

    @Scheduled(fixedDelayString = "${app.notifications.scheduler-delay:5000}")
    @Transactional
    public void processNotifications() {
        List<Notification> notifications = notificationRepository.findTop20ByProcessedFalseOrderByCreatedAtAsc();
        for (Notification notification : notifications) {
            log.info(
                    "sent notification id={} eventType={} accountId={} amount={} message={}",
                    notification.getId(),
                    notification.getEventType(),
                    notification.getAccountId(),
                    notification.getAmount(),
                    notification.getMessage()
            );
            notification.markProcessed();
        }
    }
}
