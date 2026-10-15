package com.lucasnarloch.loss_track.domain.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequestDTO(
        @NotBlank String tenantName,
        @NotBlank String cnpj,
        @NotBlank String ownerName,
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
