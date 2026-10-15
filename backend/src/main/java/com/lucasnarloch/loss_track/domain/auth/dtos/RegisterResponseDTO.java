package com.lucasnarloch.loss_track.domain.auth.dtos;

import com.lucasnarloch.loss_track.domain.tenant.dtos.TenantResponseDTO;
import com.lucasnarloch.loss_track.domain.user.dtos.UserResponseDTO;

public record RegisterResponseDTO(TenantResponseDTO tenant, UserResponseDTO owner) {
}
