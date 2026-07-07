package com.example.cashservice.service;

import com.example.cashservice.client.AccountClient;
import com.example.cashservice.client.NotificationClient;
import com.example.cashservice.dto.AccountDto;
import com.example.cashservice.model.CashOperation;
import com.example.cashservice.model.CashOperationStatus;
import com.example.cashservice.model.CashOperationType;
import com.example.cashservice.repository.CashOperationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CashService {

    private static final Logger log = LoggerFactory.getLogger(CashService.class);

    private final AccountClient accountClient;
    private final NotificationClient notificationClient;
    private final CashOperationRepository cashOperationRepository;

    public AccountDto deposit(Long accountId, BigDecimal amount) {
        return changeBalance(accountId, amount, CashOperationType.DEPOSIT);
    }

    public AccountDto withdraw(Long accountId, BigDecimal amount) {
        return changeBalance(accountId, amount, CashOperationType.WITHDRAW);
    }

    private AccountDto changeBalance(Long accountId, BigDecimal amount, CashOperationType type) {
        try {
            AccountDto account = type == CashOperationType.DEPOSIT
                    ? accountClient.deposit(accountId, amount)
                    : accountClient.withdraw(accountId, amount);
            saveOperation(accountId, type, amount, CashOperationStatus.COMPLETED, null);
            notifyCompletedOperation(accountId, amount, type);
            return account;
        } catch (RuntimeException exception) {
            saveOperation(accountId, type, amount, CashOperationStatus.FAILED, exception.getMessage());
            throw exception;
        }
    }

    private void saveOperation(Long accountId, CashOperationType type, BigDecimal amount, CashOperationStatus status, String errorMessage) {
        cashOperationRepository.save(CashOperation.builder()
                .accountId(accountId)
                .type(type)
                .amount(amount)
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }

    private void notifyCompletedOperation(Long accountId, BigDecimal amount, CashOperationType type) {
        try {
            String action = type == CashOperationType.DEPOSIT ? "deposit" : "withdraw";
            notificationClient.notify(
                    accountId,
                    "CASH_%s".formatted(type.name()),
                    amount,
                    "Cash %s completed for account %s".formatted(action, accountId)
            );
        } catch (RuntimeException exception) {
            log.warn("Cash notification failed for accountId={} type={}", accountId, type, exception);
        }
    }
}
