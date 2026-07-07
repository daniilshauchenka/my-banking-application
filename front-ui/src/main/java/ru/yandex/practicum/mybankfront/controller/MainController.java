package ru.yandex.practicum.mybankfront.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
public class MainController {

    private final BankClient bankClient;

    public MainController(BankClient bankClient) {
        this.bankClient = bankClient;
    }

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
            bankClient.updateAccount(currentLogin, new UpdateAccountRequest(name, birthdate));
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
            BigDecimal amount = BigDecimal.valueOf(value);
            if (action == CashAction.GET) {
                bankClient.withdraw(currentLogin, amount);
                fillModel(model, currentLogin, null, "Cash withdrawn");
            } else {
                bankClient.deposit(currentLogin, amount);
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
            @RequestParam("login") String login,
            Principal principal
    ) {
        try {
            String currentLogin = principal.getName();
            bankClient.transfer(new TransferRequest(currentLogin, login, BigDecimal.valueOf(value)));
            fillModel(model, currentLogin, null, "Transfer completed");
        } catch (RuntimeException exception) {
            fillModel(model, principal.getName(), List.of(exception.getMessage()), null);
        }

        return "main";
    }

    private void fillModel(Model model, String currentLogin, List<String> errors, String info) {
        AccountDto currentAccount = bankClient.getAccount(currentLogin);
        List<AccountDto> accounts = bankClient.getAccounts().stream()
                .filter(account -> !account.login().equals(currentLogin))
                .toList();

        model.addAttribute("name", currentAccount.name());
        model.addAttribute("birthdate", currentAccount.birthdate().format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("sum", currentAccount.balance().intValue());
        model.addAttribute("accounts", accounts);
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
    }
}
