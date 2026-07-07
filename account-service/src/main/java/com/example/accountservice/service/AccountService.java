package com.example.accountservice.service;

import com.example.accountservice.dto.AccountDto;
import com.example.accountservice.dto.CreateAccountRequest;
import com.example.accountservice.dto.UpdateAccountRequest;
import com.example.accountservice.exception.AccountAlreadyExistsException;
import com.example.accountservice.exception.AccountNotFoundException;
import com.example.accountservice.exception.InsufficientFundsException;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

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
        return toDto(accountRepository.save(account));
    }

    @Transactional
    public AccountDto update(Long id, UpdateAccountRequest request) {
        Account account = getAccount(id);
        account.setName(request.name());
        account.setBirthdate(request.birthdate());
        return toDto(account);
    }

    @Transactional
    public AccountDto deposit(Long id, BigDecimal amount) {
        Account account = getAccountForUpdate(id);
        account.setBalance(account.getBalance().add(amount));
        return toDto(account);
    }

    @Transactional
    public AccountDto withdraw(Long id, BigDecimal amount) {
        Account account = getAccountForUpdate(id);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough funds on account '%s'".formatted(id));
        }
        account.setBalance(account.getBalance().subtract(amount));
        return toDto(account);
    }

    @Transactional
    public void delete(Long id) {
        Account account = getAccount(id);
        accountRepository.delete(account);
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
}
