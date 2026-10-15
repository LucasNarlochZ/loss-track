package com.lucasnarloch.loss_track.auth;

import com.lucasnarloch.loss_track.domain.auth.AuthService;
import com.lucasnarloch.loss_track.domain.auth.IssuedRefreshToken;
import com.lucasnarloch.loss_track.domain.auth.JwtService;
import com.lucasnarloch.loss_track.domain.auth.RefreshTokenClaims;
import com.lucasnarloch.loss_track.domain.auth.RefreshTokenRepository;
import com.lucasnarloch.loss_track.domain.auth.TokenPair;
import com.lucasnarloch.loss_track.domain.auth.dtos.LoginRequestDTO;
import com.lucasnarloch.loss_track.domain.auth.exceptions.InactiveAccount;
import com.lucasnarloch.loss_track.domain.auth.exceptions.InvalidCredentials;
import com.lucasnarloch.loss_track.domain.auth.exceptions.InvalidRefreshToken;
import com.lucasnarloch.loss_track.domain.tenant.Tenant;
import com.lucasnarloch.loss_track.domain.tenant.TenantPlan;
import com.lucasnarloch.loss_track.domain.tenant.TenantStatus;
import com.lucasnarloch.loss_track.domain.user.User;
import com.lucasnarloch.loss_track.domain.user.UserRepository;
import com.lucasnarloch.loss_track.domain.user.UserRole;
import com.lucasnarloch.loss_track.domain.user.UserStatus;
import com.lucasnarloch.loss_track.infra.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;
    private User user;
    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenRepository,
                new JwtProperties("unused", Duration.ofMinutes(15), Duration.ofDays(30))
        );

        Tenant tenant = new Tenant("Empresa", "12345678000190", TenantPlan.BASIC, TenantStatus.ACTIVE);
        tenantId = UUID.randomUUID();
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        user = new User(tenant, "Owner", "owner@example.com", "password-hash", UserStatus.ACTIVE, UserRole.OWNER);
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
    }

    @Test
    void loginShouldIssueAndStoreTokensForTenantUser() {
        LoginRequestDTO request = new LoginRequestDTO("12345678000190", "owner@example.com", "secret");
        UUID familyId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        IssuedRefreshToken refresh = new IssuedRefreshToken("refresh-token", tokenId, familyId);

        when(userRepository.findByTenantCnpjAndEmail(request.cnpj(), request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user, familyId)).thenReturn(refresh);

        TokenPair tokens = authService.login(request, familyId);

        assertEquals("access-token", tokens.accessToken());
        assertEquals("refresh-token", tokens.refreshToken());
        verify(refreshTokenRepository).save(tokenId, userId, familyId, Duration.ofDays(30));
    }

    @Test
    void loginShouldRejectInvalidPassword() {
        LoginRequestDTO request = new LoginRequestDTO("12345678000190", "owner@example.com", "wrong");
        when(userRepository.findByTenantCnpjAndEmail(request.cnpj(), request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentials.class, () -> authService.login(request, UUID.randomUUID()));
        verify(jwtService, never()).generateAccessToken(user);
    }

    @Test
    void loginShouldRejectInactiveTenant() {
        Tenant tenant = new Tenant("Empresa", "12345678000190", TenantPlan.BASIC, TenantStatus.SUSPENDED);
        User suspendedTenantUser = new User(tenant, "Owner", "owner@example.com", "password-hash", UserStatus.ACTIVE, UserRole.OWNER);
        LoginRequestDTO request = new LoginRequestDTO("12345678000190", "owner@example.com", "secret");
        when(userRepository.findByTenantCnpjAndEmail(request.cnpj(), request.email())).thenReturn(Optional.of(suspendedTenantUser));
        when(passwordEncoder.matches(request.password(), suspendedTenantUser.getPasswordHash())).thenReturn(true);

        assertThrows(InactiveAccount.class, () -> authService.login(request, UUID.randomUUID()));
    }

    @Test
    void refreshShouldRotateTokenAndReloadActiveUser() {
        UUID oldTokenId = UUID.randomUUID();
        UUID newTokenId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        RefreshTokenClaims claims = new RefreshTokenClaims(oldTokenId, familyId, userId, tenantId);
        IssuedRefreshToken replacement = new IssuedRefreshToken("new-refresh", newTokenId, familyId);

        when(jwtService.decodeRefreshToken("old-refresh")).thenReturn(claims);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user, familyId)).thenReturn(replacement);
        when(refreshTokenRepository.rotate(oldTokenId, newTokenId, userId, familyId, Duration.ofDays(30))).thenReturn(true);

        TokenPair tokens = authService.refresh("old-refresh");

        assertEquals("new-access", tokens.accessToken());
        assertEquals("new-refresh", tokens.refreshToken());
    }

    @Test
    void refreshShouldRevokeFamilyWhenUsedTokenIsReplayed() {
        UUID oldTokenId = UUID.randomUUID();
        UUID newTokenId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        RefreshTokenClaims claims = new RefreshTokenClaims(oldTokenId, familyId, userId, tenantId);
        IssuedRefreshToken replacement = new IssuedRefreshToken("new-refresh", newTokenId, familyId);

        when(jwtService.decodeRefreshToken("old-refresh")).thenReturn(claims);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user, familyId)).thenReturn(replacement);
        when(refreshTokenRepository.rotate(oldTokenId, newTokenId, userId, familyId, Duration.ofDays(30))).thenReturn(false);

        assertThrows(InvalidRefreshToken.class, () -> authService.refresh("old-refresh"));
        verify(refreshTokenRepository).revokeFamily(familyId, Duration.ofDays(30));
    }
}
