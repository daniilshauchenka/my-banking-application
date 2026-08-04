package com.example.transferservice.service;

import com.example.transferservice.dto.TransferRequest;
import com.example.transferservice.dto.TransferResponse;
import com.example.transferservice.exception.TransferException;
import com.example.transferservice.client.NotificationClient;
import com.example.transferservice.model.TransferRecord;
import com.example.transferservice.model.TransferStatus;
import com.example.transferservice.repository.TransferRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransferRecordRepository transferRecordRepository;
    private final TransferSagaProcessor transferSagaProcessor;
    private final NotificationClient notificationClient;

    public TransferResponse transfer(TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            saveRecord(request, TransferStatus.FAILED, "Sender and receiver accounts must be different");
            notifyTransfer(request.fromAccountId(), "TRANSFER_FAILED", request.amount(),
                    "Transfer from account %s to account %s failed: sender and receiver accounts must be different"
                            .formatted(request.fromAccountId(), request.toAccountId()));
            throw new TransferException("Sender and receiver accounts must be different");
        }

        TransferRecord record = transferRecordRepository.save(TransferRecord.builder()
                .fromAccountId(request.fromAccountId())
                .toAccountId(request.toAccountId())
                .amount(request.amount())
                .status(TransferStatus.PENDING)
                .build());

        try {
            record = transferSagaProcessor.process(record.getId());
            if (record.getStatus() == TransferStatus.COMPLETED) {
                notifyTransfer(record.getFromAccountId(), "TRANSFER_COMPLETED", record.getAmount(),
                        "Transfer from account %s to account %s completed"
                                .formatted(record.getFromAccountId(), record.getToAccountId()));
                return new TransferResponse(
                        record.getFromAccountId(),
                        record.getToAccountId(),
                        record.getAmount(),
                        record.getFromBalance(),
                        record.getToBalance()
                );
            }

            throw new TransferException("Transfer '%s' is not completed: %s".formatted(record.getId(), record.getStatus()));
        } catch (TransferException exception) {
            notifyFailedIfFinal(record.getId());
            throw exception;
        } catch (RuntimeException exception) {
            notifyFailedIfFinal(record.getId());
            throw new TransferException("Transfer '%s' failed: %s".formatted(record.getId(), exception.getMessage()));
        }
    }

    private void saveRecord(TransferRequest request, TransferStatus status, String errorMessage) {
        transferRecordRepository.save(TransferRecord.builder()
                .fromAccountId(request.fromAccountId())
                .toAccountId(request.toAccountId())
                .amount(request.amount())
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }

    @Scheduled(fixedDelayString = "${app.transfers.scheduler-delay:5000}")
    public void processPendingTransfers() {
        List<TransferStatus> statuses = List.of(
                TransferStatus.PENDING,
                TransferStatus.WITHDRAWN,
                TransferStatus.DEPOSIT_PENDING,
                TransferStatus.COMPENSATION_PENDING
        );
        transferRecordRepository.findProcessableIds(statuses)
                .forEach(transferId -> {
                    try {
                        TransferRecord record = transferSagaProcessor.process(transferId);
                        notifyIfFinal(record);
                    } catch (RuntimeException exception) {
                        log.warn("Transfer saga step failed for transferId={}", transferId, exception);
                    }
                });
    }

    private void notifyFailedIfFinal(Long transferId) {
        transferRecordRepository.findById(transferId)
                .filter(record -> record.getStatus() == TransferStatus.FAILED)
                .ifPresent(this::notifyIfFinal);
    }

    private void notifyIfFinal(TransferRecord record) {
        if (record.getStatus() == TransferStatus.COMPLETED) {
            notifyTransfer(record.getFromAccountId(), "TRANSFER_COMPLETED", record.getAmount(),
                    "Transfer from account %s to account %s completed"
                            .formatted(record.getFromAccountId(), record.getToAccountId()));
        } else if (record.getStatus() == TransferStatus.FAILED) {
            notifyTransfer(record.getFromAccountId(), "TRANSFER_FAILED", record.getAmount(),
                    "Transfer from account %s to account %s failed: %s"
                            .formatted(record.getFromAccountId(), record.getToAccountId(), record.getErrorMessage()));
        } else if (record.getStatus() == TransferStatus.COMPENSATED) {
            notifyTransfer(record.getFromAccountId(), "TRANSFER_COMPENSATED", record.getAmount(),
                    "Transfer from account %s to account %s was compensated"
                            .formatted(record.getFromAccountId(), record.getToAccountId()));
        }
    }

    private void notifyTransfer(Long accountId, String eventType, java.math.BigDecimal amount, String message) {
        try {
            notificationClient.notify(accountId, eventType, amount, message);
        } catch (RuntimeException exception) {
            log.warn("Transfer notification failed for accountId={} eventType={}", accountId, eventType, exception);
        }
    }
}
