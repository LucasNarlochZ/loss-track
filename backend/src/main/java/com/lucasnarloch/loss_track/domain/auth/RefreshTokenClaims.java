package com.lucasnarloch.loss_track.domain.auth;

import java.util.UUID;

public record RefreshTokenClaims(UUID tokenId, UUID familyId, UUID userId, UUID tenantId) {
}
