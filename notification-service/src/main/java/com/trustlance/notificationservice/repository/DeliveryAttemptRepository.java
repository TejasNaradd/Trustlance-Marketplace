package com.trustlance.notificationservice.repository;

import com.trustlance.notificationservice.model.DeliveryAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryAttemptRepository extends MongoRepository<DeliveryAttempt, String> {
    List<DeliveryAttempt> findByNotificationId(String notificationId);
}
