package com.se361.financial_service.repositories;

import com.se361.financial_service.entities.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    Optional<Payout> findByGatewayPayoutId(String gatewayPayoutId);
}
