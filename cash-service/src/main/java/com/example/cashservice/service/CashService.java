package com.example.cashservice.service;

import com.example.cashservice.client.AccountClient;
import com.example.cashservice.client.NotificationClient;
import com.example.cashservice.dto.AccountDto;
import com.example.cashservice.model.CashOperation;
import com.example.cashservice.model.CashOperationStatus;
import com.example.cashservice.model.CashOperationType;
import com.example.cashservice.repository.CashOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CashService {

    private static final Logger log = LoggerFactory.getLogger(CashService.class);

    private final AccountClient accountClient;
    private final NotificationClient notificationClient;
    private final CashOperationRepository cashOperationRepository;

    public CashService(
            AccountClient accountClient,
            NotificationClient notificationClient,
            CashOperationRepository cashOperationRepository
    ) {
        this.accountClient = accountClient;
        this.notificationClient = notificationClient;
        this.cashOperationRepository = cashOperationRepository;
    }

    public AccountDto deposit(String login, BigDecimal amount) {
        return changeBalance(login, amount, CashOperationType.DEPOSIT);
    }

    public AccountDto withdraw(String login, BigDecimal amount) {
        return changeBalance(login, amount, CashOperationType.WITHDRAW);
    }

    private AccountDto changeBalance(String login, BigDecimal amount, CashOperationType type) {
        try {
            AccountDto account = type == CashOperationType.DEPOSIT
                    ? accountClient.deposit(login, amount)
                    : accountClient.withdraw(login, amount);
            cashOperationRepository.save(new CashOperation(login, type, amount, CashOperationStatus.COMPLETED, null));
            notifyCompletedOperation(login, amount, type);
            return account;
        } catch (RuntimeException exception) {
            cashOperationRepository.save(new CashOperation(login, type, amount, CashOperationStatus.FAILED, exception.getMessage()));
            throw exception;
        }
    }

    private void notifyCompletedOperation(String login, BigDecimal amount, CashOperationType type) {
        try {
            String action = type == CashOperationType.DEPOSIT ? "deposit" : "withdraw";
            notificationClient.notify(
                    login,
                    "CASH_%s".formatted(type.name()),
                    amount,
                    "Cash %s completed for account %s".formatted(action, login)
            );
        } catch (RuntimeException exception) {
            log.warn("Cash notification failed for login={} type={}", login, type, exception);
        }
    }
}
