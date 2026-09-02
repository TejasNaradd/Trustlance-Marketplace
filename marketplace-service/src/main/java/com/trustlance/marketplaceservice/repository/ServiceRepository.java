package com.trustlance.marketplaceservice.repository;

import com.trustlance.marketplaceservice.model.ServiceEntity;
import com.trustlance.marketplaceservice.model.ServiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID> {
    Page<ServiceEntity> findByStatus(ServiceStatus status, Pageable pageable);
    Page<ServiceEntity> findBySellerId(UUID sellerId, Pageable pageable);
    Page<ServiceEntity> findByCategoryIdAndStatus(UUID categoryId, ServiceStatus status, Pageable pageable);

    @Query("SELECT s FROM ServiceEntity s WHERE s.status = :status AND " +
           "(:categoryId IS NULL OR s.categoryId = :categoryId) AND " +
           "(:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.skills) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ServiceEntity> searchServices(@Param("keyword") String keyword,
                                       @Param("categoryId") UUID categoryId,
                                       @Param("status") ServiceStatus status,
                                       Pageable pageable);
}
