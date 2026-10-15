package com.lucasnarloch.loss_track.domain.user.dtos;

import com.lucasnarloch.loss_track.domain.user.UserStatus;
import com.lucasnarloch.loss_track.domain.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequestDTO(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull UserStatus status,
        @NotNull UserRole role
) {
}
