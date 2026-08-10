package com.example.cashservice.controller;

import com.example.cashservice.dto.AccountDto;
import com.example.cashservice.dto.CashRequest;
import com.example.cashservice.service.CashService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PatchMapping("/{accountId}/deposit")
    public AccountDto deposit(@PathVariable Long accountId, @Valid @RequestBody CashRequest request, Authentication authentication) {
        return cashService.deposit(accountId, request.amount(), authentication);
    }

    @PatchMapping("/{accountId}/withdraw")
    public AccountDto withdraw(@PathVariable Long accountId, @Valid @RequestBody CashRequest request, Authentication authentication) {
        return cashService.withdraw(accountId, request.amount(), authentication);
    }
}
