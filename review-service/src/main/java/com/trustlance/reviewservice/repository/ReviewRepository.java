package com.trustlance.reviewservice.repository;

import com.trustlance.reviewservice.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    
    Page<Review> findByTargetId(UUID targetId, Pageable pageable);
    
    Page<Review> findByReviewerId(UUID reviewerId, Pageable pageable);
    
    boolean existsByReviewerIdAndTargetIdAndContractId(UUID reviewerId, UUID targetId, UUID contractId);

    Page<Review> findByContractId(UUID contractId, Pageable pageable);
}
