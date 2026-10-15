package com.lucasnarloch.loss_track.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = "tenant")
    Optional<User> findByTenantCnpjAndEmail(String cnpj, String email);

    @EntityGraph(attributePaths = "tenant")
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    List<User> findAllByTenantId(UUID tenantId);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}
