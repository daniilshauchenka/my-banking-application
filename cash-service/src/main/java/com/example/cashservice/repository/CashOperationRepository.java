package com.example.cashservice.repository;

import com.example.cashservice.model.CashOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {
}
