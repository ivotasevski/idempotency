package com.ivotasevski.idempotency.repository;

import com.ivotasevski.idempotency.domain.IdempotentRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface IdempotentRequestRepository extends JpaRepository<IdempotentRequestEntity, Long> {

    @Transactional
    @Query(value = "SELECT * FROM gtw_idemp " +
            "WHERE x_request_id=:xRequestId " +
            "FOR UPDATE", nativeQuery = true)
    Optional<IdempotentRequestEntity> findByxRequestIdAndLockForUpdate(@Param("xRequestId") String xRequestId);

}
