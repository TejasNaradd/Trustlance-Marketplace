package com.trustlance.escrowservice.repository;

import com.trustlance.escrowservice.model.EscrowTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, UUID> {

    List<EscrowTransaction> findByEscrowIdOrderByCreatedAtAsc(UUID escrowId);

    Optional<EscrowTransaction> findByIdempotencyKey(String idempotencyKey);
}
