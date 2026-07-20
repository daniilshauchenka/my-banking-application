package com.example.transferservice.repository;

import com.example.transferservice.model.TransferRecord;
import com.example.transferservice.model.TransferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransferRecord t where t.id = :id")
    Optional<TransferRecord> findByIdForUpdate(Long id);

    @Query("select t.id from TransferRecord t where t.status in :statuses order by t.createdAt")
    List<Long> findProcessableIds(Collection<TransferStatus> statuses);
}
