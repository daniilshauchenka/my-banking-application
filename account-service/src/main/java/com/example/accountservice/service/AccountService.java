package com.example.accountservice.service;

import com.example.accountservice.dto.AccountDto;
import com.example.accountservice.dto.CreateAccountRequest;
import com.example.accountservice.dto.UpdateAccountRequest;
import com.example.accountservice.exception.AccountAlreadyExistsException;
import com.example.accountservice.exception.AccountNotFoundException;
import com.example.accountservice.exception.InsufficientFundsException;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountDto> findAll() {
        return accountRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountDto findByLogin(String login) {
        return toDto(getAccount(login));
    }

    @Transactional
    public AccountDto create(CreateAccountRequest request) {
        if (accountRepository.existsById(request.login())) {
            throw new AccountAlreadyExistsException("Account with login '%s' already exists".formatted(request.login()));
        }

        Account account = new Account(request.login(), request.name(), request.birthdate(), request.balance());
        return toDto(accountRepository.save(account));
    }

    @Transactional
    public AccountDto update(String login, UpdateAccountRequest request) {
        Account account = getAccount(login);
        account.setName(request.name());
        account.setBirthdate(request.birthdate());
        return toDto(account);
    }

    @Transactional
    public AccountDto deposit(String login, BigDecimal amount) {
        Account account = getAccount(login);
        account.setBalance(account.getBalance().add(amount));
        return toDto(account);
    }

    @Transactional
    public AccountDto withdraw(String login, BigDecimal amount) {
        Account account = getAccount(login);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough funds on account '%s'".formatted(login));
        }
        account.setBalance(account.getBalance().subtract(amount));
        return toDto(account);
    }

    @Transactional
    public void delete(String login) {
        Account account = getAccount(login);
        accountRepository.delete(account);
    }

    private Account getAccount(String login) {
        return accountRepository.findById(login)
                .orElseThrow(() -> new AccountNotFoundException("Account with login '%s' was not found".formatted(login)));
    }

    private AccountDto toDto(Account account) {
        return new AccountDto(account.getLogin(), account.getName(), account.getBirthdate(), account.getBalance());
    }
}
