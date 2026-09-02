package com.trustlance.contractservice.repository;

import com.trustlance.contractservice.model.Contract;
import com.trustlance.contractservice.model.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    // List contracts where the user is either the buyer or the seller
    Page<Contract> findByBuyerIdOrSellerId(UUID buyerId, UUID sellerId, Pageable pageable);

    Page<Contract> findByBuyerId(UUID buyerId, Pageable pageable);

    Page<Contract> findBySellerId(UUID sellerId, Pageable pageable);

    long countBySellerIdAndStatus(UUID sellerId, ContractStatus status);
}
