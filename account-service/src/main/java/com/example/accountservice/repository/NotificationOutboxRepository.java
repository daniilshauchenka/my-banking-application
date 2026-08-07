package com.example.accountservice.repository;

import com.example.accountservice.model.NotificationOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEvent, Long> {

    List<NotificationOutboxEvent> findTop50BySentAtIsNullOrderByCreatedAtAsc();
}
