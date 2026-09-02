package com.trustlance.notificationservice.repository;

import com.trustlance.notificationservice.model.ProcessedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, String> {
    boolean existsByEventId(String eventId);
}
