package com.example.transferservice.service;

import com.example.transferservice.client.AccountClient;
import com.example.transferservice.dto.AccountDto;
import com.example.transferservice.exception.TransferException;
import com.example.transferservice.model.TransferRecord;
import com.example.transferservice.model.TransferStatus;
import com.example.transferservice.repository.TransferRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class TransferSagaProcessor {

    private final AccountClient accountClient;
    private final TransferRecordRepository transferRecordRepository;

    @Transactional(noRollbackFor = RuntimeException.class)
    public TransferRecord process(Long transferId) {
        TransferRecord transferRecord = transferRecordRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new TransferException("Transfer '%s' was not found".formatted(transferId)));

        if (transferRecord.getStatus() == TransferStatus.PENDING) {
            withdraw(transferRecord);
        }

        if (transferRecord.getStatus() == TransferStatus.WITHDRAWN || transferRecord.getStatus() == TransferStatus.DEPOSIT_PENDING) {
            deposit(transferRecord);
        }

        if (transferRecord.getStatus() == TransferStatus.COMPENSATION_PENDING) {
            compensate(transferRecord);
        }

        return transferRecord;
    }

    private void withdraw(TransferRecord record) {
        try {
            AccountDto fromAccount = accountClient.withdraw(
                    record.getFromAccountId(),
                    record.getAmount(),
                    operationId(record, "withdraw")
            );
            record.setFromBalance(fromAccount.balance());
            record.markStatus(TransferStatus.WITHDRAWN, null);
        } catch (TransferException exception) {
            record.markStatus(TransferStatus.FAILED, exception.getMessage());
            throw exception;
        }
    }

    private void deposit(TransferRecord record) {
        record.markStatus(TransferStatus.DEPOSIT_PENDING, null);
        try {
            AccountDto toAccount = accountClient.deposit(
                    record.getToAccountId(),
                    record.getAmount(),
                    operationId(record, "deposit")
            );
            record.setToBalance(toAccount.balance());
            record.markStatus(TransferStatus.COMPLETED, null);
        } catch (TransferException exception) {
            record.markStatus(TransferStatus.COMPENSATION_PENDING, exception.getMessage());
            throw new TransferException("Receiver deposit failed; compensation is scheduled");
        } catch (RestClientException exception) {
            record.markStatus(TransferStatus.DEPOSIT_PENDING, exception.getMessage());
            throw exception;
        }
    }

    private void compensate(TransferRecord record) {
        AccountDto fromAccount = accountClient.deposit(
                record.getFromAccountId(),
                record.getAmount(),
                operationId(record, "compensate")
        );
        record.setFromBalance(fromAccount.balance());
        record.markStatus(TransferStatus.COMPENSATED, null);
    }

    private String operationId(TransferRecord record, String step) {
        return "transfer-%s-%s".formatted(record.getId(), step);
    }
}
