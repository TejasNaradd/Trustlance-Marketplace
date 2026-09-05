package com.trustlance.reviewservice.service;

import com.trustlance.reviewservice.dto.CreateReviewRequest;
import com.trustlance.reviewservice.dto.ReviewResponse;
import com.trustlance.reviewservice.event.ReviewEvent;
import com.trustlance.reviewservice.exception.AppException;
import com.trustlance.reviewservice.model.Review;
import com.trustlance.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String REVIEW_EVENTS_TOPIC = "review-events";

    public ReviewResponse createReview(UUID reviewerId, CreateReviewRequest request) {
        if (reviewerId.equals(request.getTargetId())) {
            throw new AppException("You cannot review yourself", HttpStatus.BAD_REQUEST, "INVALID_TARGET");
        }

        if (reviewRepository.existsByReviewerIdAndTargetIdAndContractId(reviewerId, request.getTargetId(), request.getContractId())) {
            throw new AppException("You have already reviewed this user for this contract", HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS");
        }

        // TODO: Validate contract eligibility — call contract-service to check contract is COMPLETED
        // For now, we trust the caller. In production, use WebClient to verify.

        Review review = Review.builder()
                .reviewerId(reviewerId)
                .contractId(request.getContractId())
                .targetId(request.getTargetId())
                .targetRole(request.getTargetRole())
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Review saved = reviewRepository.save(review);

        // Publish REVIEW_CREATED event
        ReviewEvent event = ReviewEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("REVIEW_CREATED")
                .producer("review-service")
                .occurredAt(Instant.now())
                .reviewId(saved.getId())
                .contractId(saved.getContractId())
                .reviewerId(saved.getReviewerId())
                .targetId(saved.getTargetId())
                .rating(saved.getRating())
                .build();
        kafkaTemplate.send(REVIEW_EVENTS_TOPIC, saved.getContractId().toString(), event);

        return toResponse(saved);
    }

    public Page<ReviewResponse> getReviewsForUser(UUID userId, Pageable pageable) {
        return reviewRepository.findByTargetId(userId, pageable)
                .map(this::toResponse);
    }

    public Page<ReviewResponse> getReviewsByMe(UUID userId, Pageable pageable) {
        return reviewRepository.findByReviewerId(userId, pageable)
                .map(this::toResponse);
    }

    public Page<ReviewResponse> getReviewsByContract(UUID contractId, Pageable pageable) {
        return reviewRepository.findByContractId(contractId, pageable)
                .map(this::toResponse);
    }

    public void deleteReview(String reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new AppException("Review not found", HttpStatus.NOT_FOUND, "NOT_FOUND");
        }
        reviewRepository.deleteById(reviewId);
        log.info("Review {} deleted by ADMIN moderation", reviewId);
    }

    private ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .contractId(r.getContractId())
                .reviewerId(r.getReviewerId())
                .targetId(r.getTargetId())
                .targetRole(r.getTargetRole())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
