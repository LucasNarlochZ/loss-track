package com.lucasnarloch.loss_track.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByTenantCnpjAndEmail(String cnpj, String email);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    List<User> findAllByTenantId(UUID tenantId);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}
