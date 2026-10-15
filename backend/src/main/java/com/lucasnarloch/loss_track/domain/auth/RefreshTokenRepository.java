package com.lucasnarloch.loss_track.domain.auth;

import java.time.Duration;
import java.util.UUID;

public interface RefreshTokenRepository {
    void save(UUID tokenId, UUID userId, UUID familyId, Duration expiration);

    boolean rotate(UUID oldTokenId, UUID newTokenId, UUID userId, UUID familyId, Duration expiration);

    void revokeFamily(UUID familyId, Duration expiration);
}
