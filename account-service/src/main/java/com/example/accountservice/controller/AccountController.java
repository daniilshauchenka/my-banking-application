package com.example.accountservice.controller;

import com.example.accountservice.dto.AccountDto;
import com.example.accountservice.dto.CreateAccountRequest;
import com.example.accountservice.dto.DepositRequest;
import com.example.accountservice.dto.UpdateAccountRequest;
import com.example.accountservice.dto.WithdrawRequest;
import com.example.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountDto> findAll() {
        return accountService.findAll();
    }

    @GetMapping("/{login}")
    public AccountDto findByLogin(@PathVariable String login) {
        return accountService.findByLogin(login);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDto create(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(request);
    }

    @PutMapping("/{login}")
    public AccountDto update(@PathVariable String login, @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.update(login, request);
    }

    @PatchMapping("/{login}/deposit")
    public AccountDto deposit(@PathVariable String login, @Valid @RequestBody DepositRequest request) {
        return accountService.deposit(login, request.amount());
    }

    @PatchMapping("/{login}/withdraw")
    public AccountDto withdraw(@PathVariable String login, @Valid @RequestBody WithdrawRequest request) {
        return accountService.withdraw(login, request.amount());
    }

    @DeleteMapping("/{login}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String login) {
        accountService.delete(login);
    }
}
