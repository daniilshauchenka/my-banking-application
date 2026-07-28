package com.example.accountservice;

import com.example.accountservice.dto.CreateAccountRequest;
import com.example.accountservice.client.NotificationClient;
import com.example.accountservice.exception.InsufficientFundsException;
import com.example.accountservice.model.Account;
import com.example.accountservice.repository.AccountRepository;
import com.example.accountservice.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AccountServiceTests {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoBean
    private NotificationClient notificationClient;

    private Long accountId;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountId = accountService.create(new CreateAccountRequest(
                "user1",
                "user 1",
                LocalDate.parse("2001-01-01"),
                new BigDecimal("100.00")
        )).id();
    }

    @Test
    void depositUsesPersistedAccountIdAndUpdatesVersion() {
        accountService.deposit(accountId, new BigDecimal("15.00"));

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getBalance()).isEqualByComparingTo("115.00");
        assertThat(account.getVersion()).isEqualTo(1L);
    }

    @Test
    void depositWithSameOperationIdIsAppliedOnce() {
        accountService.deposit(accountId, new BigDecimal("15.00"), "operation-1");
        accountService.deposit(accountId, new BigDecimal("15.00"), "operation-1");

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getBalance()).isEqualByComparingTo("115.00");
    }

    @Test
    void withdrawWithSameOperationIdIsAppliedOnce() {
        accountService.withdraw(accountId, new BigDecimal("15.00"), "operation-2");
        accountService.withdraw(accountId, new BigDecimal("15.00"), "operation-2");

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getBalance()).isEqualByComparingTo("85.00");
    }

    @Test
    void withdrawRejectsInsufficientFunds() {
        assertThatThrownBy(() -> accountService.withdraw(accountId, new BigDecimal("101.00")))
                .isInstanceOf(InsufficientFundsException.class);

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }
}
