package com.lucasnarloch.loss_track.domain.product.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
public record ProductRequestDTO(
        @NotBlank String name,
        @NotBlank String sku,
        @NotBlank String barcode,
        @NotBlank String category,
        @NotBlank String unit,
        @NotNull BigDecimal costPrice
) {
}
