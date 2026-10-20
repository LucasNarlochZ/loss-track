package com.lucasnarloch.loss_track.domain.user.dtos;

import com.lucasnarloch.loss_track.domain.store.Store;
import com.lucasnarloch.loss_track.domain.user.User;
import com.lucasnarloch.loss_track.domain.user.UserStatus;
import com.lucasnarloch.loss_track.domain.user.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        UUID tenantId,
        String name,
        String email,
        UserStatus status,
        UserRole role,
        Store assignedStore,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public UserResponseDTO(User user) {
        this(
                user.getId(),
                user.getTenant().getId(),
                user.getName(),
                user.getEmail(),
                user.getStatus(),
                user.getRole(),
                user.getStore(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
