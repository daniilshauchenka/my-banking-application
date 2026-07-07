package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notify(NotificationRequest request) {
        log.info(
                "notification eventType={} login={} amount={} message={}",
                request.eventType(),
                request.login(),
                request.amount(),
                request.message()
        );
    }
}
