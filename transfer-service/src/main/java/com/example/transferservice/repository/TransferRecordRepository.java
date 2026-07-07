package com.example.transferservice.repository;

import com.example.transferservice.model.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {
}
