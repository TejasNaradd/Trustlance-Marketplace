package com.trustlance.contractservice.repository;

import com.trustlance.contractservice.model.Milestone;
import com.trustlance.contractservice.model.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    List<Milestone> findByContractId(UUID contractId);

    long countByContractIdAndStatusNot(UUID contractId, MilestoneStatus status);
}
