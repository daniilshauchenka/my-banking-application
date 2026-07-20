package com.example.accountservice.repository;

import com.example.accountservice.model.AccountOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountOperationRepository extends JpaRepository<AccountOperation, Long> {

    Optional<AccountOperation> findByOperationId(String operationId);
}
