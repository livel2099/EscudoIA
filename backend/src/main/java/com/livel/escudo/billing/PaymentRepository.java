package com.livel.escudo.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    boolean existsByProviderEventId(String providerEventId);
    Optional<PaymentEntity> findByProviderId(String providerId);
}

