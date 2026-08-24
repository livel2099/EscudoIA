package com.livel.escudo.scan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ScanRepository extends JpaRepository<ScanEntity, UUID> {
    Page<ScanEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByUserIdAndCreatedAtAfter(UUID userId, Instant since);
    @EntityGraph(attributePaths = "indicators") Optional<ScanEntity> findDetailedById(UUID id);
}

