package com.livel.escudo.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PlanPriceRepository extends JpaRepository<PlanPriceEntity, UUID> {
    @Query("select p from PlanPriceEntity p where p.planId=:planId and p.activeFrom<=:now and (p.activeTo is null or p.activeTo>:now) order by p.activeFrom desc limit 1")
    Optional<PlanPriceEntity> current(UUID planId, Instant now);
}

