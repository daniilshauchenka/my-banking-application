package ru.yandex.practicum.mybankfront.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.mybankfront.client.BankClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;
import ru.yandex.practicum.mybankfront.controller.dto.TransferRequest;
import ru.yandex.practicum.mybankfront.controller.dto.UpdateAccountRequest;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final BankClient bankClient;

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model, Principal principal) {
        fillModel(model, principal.getName(), null, null);
        return "main";
    }

    @PostMapping("/account")
    public String editAccount(
            Model model,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate,
            Principal principal
    ) {
        try {
            String currentLogin = principal.getName();
            AccountDto currentAccount = findCurrentAccount(currentLogin);
            bankClient.updateAccount(currentAccount.id(), new UpdateAccountRequest(name, birthdate));
            fillModel(model, currentLogin, null, "Account updated");
        } catch (RuntimeException exception) {
            fillModel(model, principal.getName(), List.of(exception.getMessage()), null);
        }

        return "main";
    }

    @PostMapping("/cash")
    public String editCash(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("action") CashAction action,
            Principal principal
    ) {
        try {
            String currentLogin = principal.getName();
            AccountDto currentAccount = findCurrentAccount(currentLogin);
            BigDecimal amount = BigDecimal.valueOf(value);
            if (action == CashAction.GET) {
                bankClient.withdraw(currentAccount.id(), amount);
                fillModel(model, currentLogin, null, "Cash withdrawn");
            } else {
                bankClient.deposit(currentAccount.id(), amount);
                fillModel(model, currentLogin, null, "Cash deposited");
            }
        } catch (RuntimeException exception) {
            fillModel(model, principal.getName(), List.of(exception.getMessage()), null);
        }

        return "main";
    }

    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("accountId") Long accountId,
            Principal principal
    ) {
        try {
            String currentLogin = principal.getName();
            AccountDto currentAccount = findCurrentAccount(currentLogin);
            bankClient.transfer(new TransferRequest(currentAccount.id(), accountId, BigDecimal.valueOf(value)));
            fillModel(model, currentLogin, null, "Transfer completed");
        } catch (RuntimeException exception) {
            fillModel(model, principal.getName(), List.of(exception.getMessage()), null);
        }

        return "main";
    }

    private void fillModel(Model model, String currentLogin, List<String> errors, String info) {
        List<AccountDto> allAccounts = bankClient.getAccounts();
        AccountDto currentAccount = allAccounts.stream()
                .filter(account -> account.login().equals(currentLogin))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Current account was not found"));
        List<AccountDto> accounts = allAccounts.stream()
                .filter(account -> !account.login().equals(currentLogin))
                .toList();

        model.addAttribute("name", currentAccount.name());
        model.addAttribute("birthdate", currentAccount.birthdate().format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("sum", currentAccount.balance().intValue());
        model.addAttribute("accounts", accounts);
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
    }

    private AccountDto findCurrentAccount(String currentLogin) {
        return bankClient.getAccounts().stream()
                .filter(account -> account.login().equals(currentLogin))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Current account was not found"));
    }
}
