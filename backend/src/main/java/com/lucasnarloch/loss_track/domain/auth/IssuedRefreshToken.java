package com.lucasnarloch.loss_track.domain.auth;

import java.util.UUID;

public record IssuedRefreshToken(String value, UUID tokenId, UUID familyId) {
}
