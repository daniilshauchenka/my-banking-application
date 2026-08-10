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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CashService {

    private static final Logger log = LoggerFactory.getLogger(CashService.class);

    private final AccountClient accountClient;
    private final NotificationClient notificationClient;
    private final CashOperationRepository cashOperationRepository;

    @Transactional
    public AccountDto deposit(Long accountId, BigDecimal amount) {
        return changeBalance(accountId, amount, CashOperationType.DEPOSIT);
    }

    @Transactional
    public AccountDto deposit(Long accountId, BigDecimal amount, Authentication authentication) {
        assertOwner(accountId, authentication);
        return changeBalance(accountId, amount, CashOperationType.DEPOSIT);
    }

    @Transactional
    public AccountDto withdraw(Long accountId, BigDecimal amount) {
        return changeBalance(accountId, amount, CashOperationType.WITHDRAW);
    }

    @Transactional
    public AccountDto withdraw(Long accountId, BigDecimal amount, Authentication authentication) {
        assertOwner(accountId, authentication);
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

    private void assertOwner(Long accountId, Authentication authentication) {
        AccountDto account = accountClient.getAccount(accountId);
        String login = loginFrom(authentication);
        if (!account.login().equals(login)) {
            throw new AccessDeniedException("Access to another account is forbidden");
        }
    }

    private String loginFrom(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("JWT authentication is required");
        }

        String login = jwt.getClaimAsString("preferred_username");
        if (login == null || login.isBlank()) {
            throw new AccessDeniedException("User login claim is required");
        }
        return login;
    }
}
