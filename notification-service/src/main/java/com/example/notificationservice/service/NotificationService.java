package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notify(NotificationRequest request) {
        notificationRepository.save(Notification.builder()
                .accountId(request.accountId())
                .eventType(request.eventType())
                .amount(request.amount())
                .message(request.message())
                .build());
    }
}
