package com.lucasnarloch.loss_track.domain.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank String cnpj,
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
