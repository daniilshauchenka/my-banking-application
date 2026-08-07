package com.example.transferservice.repository;

import com.example.transferservice.model.NotificationOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEvent, Long> {

    List<NotificationOutboxEvent> findTop50BySentAtIsNullOrderByCreatedAtAsc();
}
