package com.lucasnarloch.loss_track.domain.auth;

import com.lucasnarloch.loss_track.domain.auth.exceptions.InvalidRefreshToken;
import com.lucasnarloch.loss_track.domain.user.User;
import com.lucasnarloch.loss_track.infra.config.JwtConfig;
import com.lucasnarloch.loss_track.infra.config.JwtProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder refreshJwtDecoder;
    private final JwtProperties jwtProperties;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder,
            JwtProperties jwtProperties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.refreshJwtDecoder = refreshJwtDecoder;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(User user) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = baseClaims(user, issuedAt)
                .expiresAt(issuedAt.plus(jwtProperties.accessTokenExpiration()))
                .claim("token_type", TokenType.ACCESS.value())
                .build();
        return encode(claims);
    }

    public IssuedRefreshToken generateRefreshToken(User user, UUID familyId) {
        Instant issuedAt = Instant.now();
        UUID tokenId = UUID.randomUUID();
        JwtClaimsSet claims = baseClaims(user, issuedAt)
                .expiresAt(issuedAt.plus(jwtProperties.refreshTokenExpiration()))
                .id(tokenId.toString())
                .claim("family_id", familyId.toString())
                .claim("token_type", TokenType.REFRESH.value())
                .build();
        return new IssuedRefreshToken(encode(claims), tokenId, familyId);
    }

    public RefreshTokenClaims decodeRefreshToken(String token) {
        try {
            var jwt = refreshJwtDecoder.decode(token);
            return new RefreshTokenClaims(
                    UUID.fromString(jwt.getId()),
                    UUID.fromString(jwt.getClaimAsString("family_id")),
                    UUID.fromString(jwt.getSubject()),
                    UUID.fromString(jwt.getClaimAsString("tenant_id"))
            );
        } catch (JwtException | IllegalArgumentException | NullPointerException exception) {
            throw new InvalidRefreshToken();
        }
    }

    private JwtClaimsSet.Builder baseClaims(User user, Instant issuedAt) {
        return JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .issuedAt(issuedAt)
                .subject(user.getId().toString())
                .claim("tenant_id", user.getTenant().getId().toString())
                .claim("role", user.getRole().name());
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
