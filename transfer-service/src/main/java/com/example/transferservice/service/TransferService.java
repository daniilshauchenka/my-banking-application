package com.example.transferservice.service;

import com.example.transferservice.client.AccountClient;
import com.example.transferservice.dto.AccountDto;
import com.example.transferservice.dto.TransferRequest;
import com.example.transferservice.dto.TransferResponse;
import com.example.transferservice.exception.TransferException;
import com.example.transferservice.model.TransferRecord;
import com.example.transferservice.model.TransferStatus;
import com.example.transferservice.repository.TransferRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class TransferService {

    private final AccountClient accountClient;
    private final TransferRecordRepository transferRecordRepository;

    public TransferService(AccountClient accountClient, TransferRecordRepository transferRecordRepository) {
        this.accountClient = accountClient;
        this.transferRecordRepository = transferRecordRepository;
    }

    public TransferResponse transfer(TransferRequest request) {
        if (request.fromLogin().equals(request.toLogin())) {
            saveRecord(request, TransferStatus.FAILED, "Sender and receiver accounts must be different");
            throw new TransferException("Sender and receiver accounts must be different");
        }

        try {
            AccountDto fromAccount = accountClient.withdraw(request.fromLogin(), request.amount());
            AccountDto toAccount;
            try {
                toAccount = accountClient.deposit(request.toLogin(), request.amount());
            } catch (RuntimeException exception) {
                accountClient.deposit(request.fromLogin(), request.amount());
                throw exception;
            }

            saveRecord(request, TransferStatus.COMPLETED, null);
            return new TransferResponse(
                    request.fromLogin(),
                    request.toLogin(),
                    request.amount(),
                    fromAccount.balance(),
                    toAccount.balance()
            );
        } catch (RuntimeException exception) {
            saveRecord(request, TransferStatus.FAILED, exception.getMessage());
            throw exception;
        }
    }

    private void saveRecord(TransferRequest request, TransferStatus status, String errorMessage) {
        transferRecordRepository.save(new TransferRecord(
                request.fromLogin(),
                request.toLogin(),
                request.amount(),
                status,
                errorMessage
        ));
    }
}
