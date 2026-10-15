package com.lucasnarloch.loss_track.domain.auth;

import com.lucasnarloch.loss_track.domain.auth.dtos.LoginRequestDTO;
import com.lucasnarloch.loss_track.domain.auth.exceptions.InactiveAccount;
import com.lucasnarloch.loss_track.domain.auth.exceptions.InvalidCredentials;
import com.lucasnarloch.loss_track.domain.auth.exceptions.InvalidRefreshToken;
import com.lucasnarloch.loss_track.domain.tenant.TenantStatus;
import com.lucasnarloch.loss_track.domain.user.User;
import com.lucasnarloch.loss_track.domain.user.UserRepository;
import com.lucasnarloch.loss_track.domain.user.UserStatus;
import com.lucasnarloch.loss_track.infra.config.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public TokenPair login(LoginRequestDTO request, UUID familyId) {
        User user = userRepository.findByTenantCnpjAndEmail(request.cnpj(), request.email())
                .orElseThrow(InvalidCredentials::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentials();
        }
        validateActive(user);

        String accessToken = jwtService.generateAccessToken(user);
        IssuedRefreshToken refreshToken = jwtService.generateRefreshToken(user, familyId);
        refreshTokenRepository.save(
                refreshToken.tokenId(),
                user.getId(),
                refreshToken.familyId(),
                jwtProperties.refreshTokenExpiration()
        );
        return new TokenPair(accessToken, refreshToken.value());
    }

    public TokenPair refresh(String refreshTokenValue) {
        RefreshTokenClaims claims = jwtService.decodeRefreshToken(refreshTokenValue);
        User user = userRepository.findByIdAndTenantId(claims.userId(), claims.tenantId())
                .orElseThrow(InvalidRefreshToken::new);
        validateActive(user);

        String accessToken = jwtService.generateAccessToken(user);
        IssuedRefreshToken replacement = jwtService.generateRefreshToken(user, claims.familyId());
        boolean rotated = refreshTokenRepository.rotate(
                claims.tokenId(),
                replacement.tokenId(),
                user.getId(),
                claims.familyId(),
                jwtProperties.refreshTokenExpiration()
        );
        if (!rotated) {
            refreshTokenRepository.revokeFamily(claims.familyId(), jwtProperties.refreshTokenExpiration());
            throw new InvalidRefreshToken();
        }
        return new TokenPair(accessToken, replacement.value());
    }

    public void logout(String refreshTokenValue) {
        RefreshTokenClaims claims = jwtService.decodeRefreshToken(refreshTokenValue);
        refreshTokenRepository.revokeFamily(claims.familyId(), jwtProperties.refreshTokenExpiration());
    }

    private void validateActive(User user) {
        TenantStatus tenantStatus = user.getTenant().getStatus();
        boolean tenantActive = tenantStatus == TenantStatus.ACTIVE || tenantStatus == TenantStatus.TRIAL;
        if (user.getStatus() != UserStatus.ACTIVE || !tenantActive) {
            throw new InactiveAccount();
        }
    }
}
