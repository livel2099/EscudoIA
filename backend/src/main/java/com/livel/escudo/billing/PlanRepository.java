package com.livel.escudo.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<PlanEntity, UUID> {
    List<PlanEntity> findByActiveTrueOrderByNameAsc();
    Optional<PlanEntity> findByCodeAndActiveTrue(String code);
}

