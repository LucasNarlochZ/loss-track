package com.lucasnarloch.loss_track.domain.store;

import com.lucasnarloch.loss_track.domain.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    Optional<Store> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Store> findAllByTenantId(UUID tenantId);

    UUID tenant(Tenant tenant);
}
