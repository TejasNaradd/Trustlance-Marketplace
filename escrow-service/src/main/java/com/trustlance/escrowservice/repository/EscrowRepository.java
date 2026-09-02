package com.trustlance.escrowservice.repository;

import com.trustlance.escrowservice.model.Escrow;
import com.trustlance.escrowservice.model.EscrowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscrowRepository extends JpaRepository<Escrow, UUID> {

    Optional<Escrow> findByMilestoneId(UUID milestoneId);

    List<Escrow> findByContractId(UUID contractId);

    Page<Escrow> findByBuyerIdOrSellerId(UUID buyerId, UUID sellerId, Pageable pageable);

    List<Escrow> findByContractIdAndStatus(UUID contractId, EscrowStatus status);
}
