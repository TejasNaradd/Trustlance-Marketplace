package com.trustlance.notificationservice.repository;

import com.trustlance.notificationservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    Page<Notification> findByRecipientId(UUID recipientId, Pageable pageable);
    
    Page<Notification> findByRecipientIdAndReadFalse(UUID recipientId, Pageable pageable);
    
    long countByRecipientIdAndReadFalse(UUID recipientId);
}
