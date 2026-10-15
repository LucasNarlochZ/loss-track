package com.lucasnarloch.loss_track.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Product> findAllByTenantId(UUID tenantId);
}
