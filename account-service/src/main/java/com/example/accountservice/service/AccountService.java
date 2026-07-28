package com.example.accountservice.service;

import com.example.accountservice.client.NotificationClient;
import com.example.accountservice.dto.AccountDto;
import com.example.accountservice.dto.CreateAccountRequest;
import com.example.accountservice.dto.UpdateAccountRequest;
import com.example.accountservice.exception.AccountAlreadyExistsException;
import com.example.accountservice.exception.AccountNotFoundException;
import com.example.accountservice.exception.InsufficientFundsException;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.AccountOperation;
import com.example.accountservice.model.AccountOperationType;
import com.example.accountservice.repository.AccountOperationRepository;
import com.example.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountOperationRepository accountOperationRepository;
    private final NotificationClient notificationClient;

    @Transactional(readOnly = true)
    public List<AccountDto> findAll() {
        return accountRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountDto findById(Long id) {
        return toDto(getAccount(id));
    }

    @Transactional
    public AccountDto create(CreateAccountRequest request) {
        if (accountRepository.existsByLogin(request.login())) {
            throw new AccountAlreadyExistsException("Account with login '%s' already exists".formatted(request.login()));
        }

        Account account = Account.builder()
                .login(request.login())
                .name(request.name())
                .birthdate(request.birthdate())
                .balance(request.balance())
                .build();
        Account savedAccount = accountRepository.save(account);
        notifyAccount(savedAccount.getId(), "ACCOUNT_CREATED", savedAccount.getBalance(),
                "Account %s created".formatted(savedAccount.getId()));
        return toDto(savedAccount);
    }

    @Transactional
    public AccountDto update(Long id, UpdateAccountRequest request) {
        Account account = getAccount(id);
        account.setName(request.name());
        account.setBirthdate(request.birthdate());
        notifyAccount(account.getId(), "ACCOUNT_UPDATED", account.getBalance(),
                "Account %s updated".formatted(account.getId()));
        return toDto(account);
    }

    @Transactional
    public AccountDto deposit(Long id, BigDecimal amount) {
        return depositInternal(id, amount, null);
    }

    @Transactional
    public AccountDto deposit(Long id, BigDecimal amount, String operationId) {
        return depositInternal(id, amount, operationId);
    }

    private AccountDto depositInternal(Long id, BigDecimal amount, String operationId) {
        Account account = getAccountForUpdate(id);
        AccountDto duplicate = findDuplicateOperation(account, amount, operationId, AccountOperationType.DEPOSIT);
        if (duplicate != null) {
            return duplicate;
        }

        account.setBalance(account.getBalance().add(amount));
        saveOperation(account.getId(), amount, operationId, AccountOperationType.DEPOSIT);
        notifyAccount(account.getId(), "ACCOUNT_DEPOSIT", amount,
                "Account %s balance increased by %s".formatted(account.getId(), amount));
        return toDto(account);
    }

    @Transactional
    public AccountDto withdraw(Long id, BigDecimal amount) {
        return withdrawInternal(id, amount, null);
    }

    @Transactional
    public AccountDto withdraw(Long id, BigDecimal amount, String operationId) {
        return withdrawInternal(id, amount, operationId);
    }

    private AccountDto withdrawInternal(Long id, BigDecimal amount, String operationId) {
        Account account = getAccountForUpdate(id);
        AccountDto duplicate = findDuplicateOperation(account, amount, operationId, AccountOperationType.WITHDRAW);
        if (duplicate != null) {
            return duplicate;
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough funds on account '%s'".formatted(id));
        }
        account.setBalance(account.getBalance().subtract(amount));
        saveOperation(account.getId(), amount, operationId, AccountOperationType.WITHDRAW);
        notifyAccount(account.getId(), "ACCOUNT_WITHDRAW", amount,
                "Account %s balance decreased by %s".formatted(account.getId(), amount));
        return toDto(account);
    }

    @Transactional
    public void delete(Long id) {
        Account account = getAccount(id);
        accountRepository.delete(account);
        notifyAccount(account.getId(), "ACCOUNT_DELETED", account.getBalance(),
                "Account %s deleted".formatted(account.getId()));
    }

    private Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account with id '%s' was not found".formatted(id)));
    }

    private Account getAccountForUpdate(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException("Account with id '%s' was not found".formatted(id)));
    }

    private AccountDto toDto(Account account) {
        return new AccountDto(
                account.getId(),
                account.getLogin(),
                account.getName(),
                account.getBirthdate(),
                account.getBalance()
        );
    }

    private AccountDto findDuplicateOperation(Account account, BigDecimal amount, String operationId, AccountOperationType type) {
        if (operationId == null || operationId.isBlank()) {
            return null;
        }

        return accountOperationRepository.findByOperationId(operationId)
                .map(operation -> {
                    if (!operation.getAccountId().equals(account.getId())
                            || operation.getType() != type
                            || operation.getAmount().compareTo(amount) != 0) {
                        throw new IllegalArgumentException("Operation id '%s' was already used for another operation".formatted(operationId));
                    }
                    return toDto(account);
                })
                .orElse(null);
    }

    private void saveOperation(Long accountId, BigDecimal amount, String operationId, AccountOperationType type) {
        if (operationId == null || operationId.isBlank()) {
            return;
        }

        accountOperationRepository.save(AccountOperation.builder()
                .operationId(operationId)
                .accountId(accountId)
                .type(type)
                .amount(amount)
                .build());
    }

    private void notifyAccount(Long accountId, String eventType, BigDecimal amount, String message) {
        try {
            notificationClient.notify(accountId, eventType, amount, message);
        } catch (RuntimeException exception) {
            log.warn("Account notification failed for accountId={} eventType={}", accountId, eventType, exception);
        }
    }
}
