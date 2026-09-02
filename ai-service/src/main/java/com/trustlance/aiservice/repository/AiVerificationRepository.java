package com.trustlance.aiservice.repository;

import com.trustlance.aiservice.model.AiVerification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiVerificationRepository extends MongoRepository<AiVerification, String> {
    Page<AiVerification> findByUserId(UUID userId, Pageable pageable);
}
