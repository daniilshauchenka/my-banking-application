package com.example.accountservice.controller;

import com.example.accountservice.dto.AccountDto;
import com.example.accountservice.dto.CreateAccountRequest;
import com.example.accountservice.dto.DepositRequest;
import com.example.accountservice.dto.UpdateAccountRequest;
import com.example.accountservice.dto.WithdrawRequest;
import com.example.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
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
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountDto> findAll(Authentication authentication) {
        if (isServiceClient(authentication)) {
            return accountService.findAll();
        }
        return List.of(accountService.findByLogin(loginFrom(authentication)));
    }

    @GetMapping("/{id}")
    public AccountDto findById(@PathVariable Long id, Authentication authentication) {
        AccountDto account = accountService.findById(id);
        assertOwnerOrService(account, authentication);
        return account;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDto create(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(request);
    }

    @PutMapping("/{id}")
    public AccountDto update(@PathVariable Long id, @Valid @RequestBody UpdateAccountRequest request, Authentication authentication) {
        assertOwnerOrService(accountService.findById(id), authentication);
        return accountService.update(id, request);
    }

    @PatchMapping("/{id}/deposit")
    public AccountDto deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest request) {
        return accountService.deposit(id, request.amount(), request.operationId());
    }

    @PatchMapping("/{id}/withdraw")
    public AccountDto withdraw(@PathVariable Long id, @Valid @RequestBody WithdrawRequest request) {
        return accountService.withdraw(id, request.amount(), request.operationId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable Long id, Authentication authentication) {
        assertOwnerOrService(accountService.findById(id), authentication);
        accountService.delete(id);
    }

    private void assertOwnerOrService(AccountDto account, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("JWT authentication is required");
        }

        if (isServiceClient(authentication)) {
            return;
        }

        if (!account.login().equals(loginFrom(authentication))) {
            throw new AccessDeniedException("Access to another account is forbidden");
        }
    }

    private boolean isServiceClient(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        String clientId = jwt.getClaimAsString("azp");
        if (clientId == null) {
            clientId = jwt.getClaimAsString("client_id");
        }
        return "cash-service".equals(clientId) || "transfer-service".equals(clientId);
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
