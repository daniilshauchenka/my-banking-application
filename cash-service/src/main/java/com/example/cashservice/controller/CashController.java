package com.example.cashservice.controller;

import com.example.cashservice.dto.AccountDto;
import com.example.cashservice.dto.CashRequest;
import com.example.cashservice.service.CashService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cash")
public class CashController {

    private final CashService cashService;

    public CashController(CashService cashService) {
        this.cashService = cashService;
    }

    @PatchMapping("/{login}/deposit")
    public AccountDto deposit(@PathVariable String login, @Valid @RequestBody CashRequest request) {
        return cashService.deposit(login, request.amount());
    }

    @PatchMapping("/{login}/withdraw")
    public AccountDto withdraw(@PathVariable String login, @Valid @RequestBody CashRequest request) {
        return cashService.withdraw(login, request.amount());
    }
}
