package com.example.cashservice.repository;

import com.example.cashservice.model.NotificationOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEvent, Long> {

    List<NotificationOutboxEvent> findTop50BySentAtIsNullOrderByCreatedAtAsc();
}
