package com.lucasnarloch.loss_track.domain.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}
