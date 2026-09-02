package com.trustlance.disputeservice.repository;

import com.trustlance.disputeservice.model.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends MongoRepository<Dispute, String> {
    
    Optional<Dispute> findByMilestoneId(UUID milestoneId);
    
    Page<Dispute> findByInitiatorIdOrRespondentId(UUID initiatorId, UUID respondentId, Pageable pageable);
}
