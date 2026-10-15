package com.lucasnarloch.loss_track.domain.store.dtos;

import jakarta.validation.constraints.NotBlank;
public record StoreRequestDTO(
        @NotBlank String name,
        @NotBlank String code,
        @NotBlank String cnpj,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String state
) {
}
