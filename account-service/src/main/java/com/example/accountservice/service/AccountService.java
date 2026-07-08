package com.example.accountservice.service;

 import com.example.accountservice.client.NotificationClient;
import com.example.accountservice.dto.AccountDto;
import com.example.accountservice.dto.CreateAccountRequest;
import com.example.accountservice.dto.UpdateAccountRequest;
import com.example.accountservice.exception.AccountAlreadyExistsException;
import com.example.accountservice.exception.AccountNotFoundException;
import com.example.accountservice.exception.InsufficientFundsException;
import com.example.accountservice.model.Account;
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
        Account account = getAccountForUpdate(id);
        account.setBalance(account.getBalance().add(amount));
        notifyAccount(account.getId(), "ACCOUNT_DEPOSIT", amount,
                "Account %s balance increased by %s".formatted(account.getId(), amount));
        return toDto(account);
    }

    @Transactional
    public AccountDto withdraw(Long id, BigDecimal amount) {
        Account account = getAccountForUpdate(id);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough funds on account '%s'".formatted(id));
        }
        account.setBalance(account.getBalance().subtract(amount));
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

    private void notifyAccount(Long accountId, String eventType, BigDecimal amount, String message) {
        try {
            notificationClient.notify(accountId, eventType, amount, message);
        } catch (RuntimeException exception) {
            log.warn("Account notification failed for accountId={} eventType={}", accountId, eventType, exception);
        }
    }
}
