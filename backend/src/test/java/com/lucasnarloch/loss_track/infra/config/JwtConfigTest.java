package com.lucasnarloch.loss_track.infra.config;

import com.lucasnarloch.loss_track.domain.auth.JwtService;
import com.lucasnarloch.loss_track.domain.auth.exceptions.InvalidRefreshToken;
import com.lucasnarloch.loss_track.domain.tenant.Tenant;
import com.lucasnarloch.loss_track.domain.tenant.TenantPlan;
import com.lucasnarloch.loss_track.domain.user.User;
import com.lucasnarloch.loss_track.domain.user.UserRole;
import com.lucasnarloch.loss_track.domain.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTest {

    @Test
    void tokenDecodersShouldAcceptOnlyTheirTokenType() {
        JwtProperties properties = new JwtProperties(
                Base64.getEncoder().encodeToString(new byte[32]),
                Duration.ofMinutes(15),
                Duration.ofDays(30)
        );
        JwtConfig config = new JwtConfig();
        var key = config.jwtSecretKey(properties);
        var accessDecoder = config.accessJwtDecoder(key);
        var refreshDecoder = config.refreshJwtDecoder(key);
        JwtService service = new JwtService(config.jwtEncoder(key), refreshDecoder, properties);

        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        Tenant tenant = new Tenant("Empresa", "12345678000190", TenantPlan.BASIC);
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        User user = new User(tenant, "Owner", "owner@example.com", "hash", UserStatus.ACTIVE, UserRole.OWNER);
        ReflectionTestUtils.setField(user, "id", userId);

        String accessToken = service.generateAccessToken(user);
        var refreshToken = service.generateRefreshToken(user, familyId);

        var accessJwt = accessDecoder.decode(accessToken);
        assertEquals(userId.toString(), accessJwt.getSubject());
        assertEquals(tenantId.toString(), accessJwt.getClaimAsString("tenant_id"));
        assertEquals("OWNER", accessJwt.getClaimAsString("role"));
        assertEquals(userId, service.decodeRefreshToken(refreshToken.value()).userId());
        assertThrows(JwtException.class, () -> accessDecoder.decode(refreshToken.value()));
        assertThrows(InvalidRefreshToken.class, () -> service.decodeRefreshToken(accessToken));
    }
}
