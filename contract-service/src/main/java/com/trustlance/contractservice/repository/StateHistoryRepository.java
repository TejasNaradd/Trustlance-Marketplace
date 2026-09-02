package com.trustlance.contractservice.repository;

import com.trustlance.contractservice.model.StateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StateHistoryRepository extends JpaRepository<StateHistory, UUID> {

    List<StateHistory> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, UUID entityId);
}
