package com.lucasnarloch.loss_track.domain.auth;

import com.lucasnarloch.loss_track.domain.auth.dtos.LoginRequestDTO;
import com.lucasnarloch.loss_track.domain.auth.dtos.LoginResponseDTO;
import com.lucasnarloch.loss_track.domain.auth.dtos.RegisterRequestDTO;
import com.lucasnarloch.loss_track.domain.auth.dtos.RegisterResponseDTO;
import com.lucasnarloch.loss_track.infra.config.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final RegistrationService registrationService;
    private final JwtProperties jwtProperties;
    private final boolean secureCookies;

    public AuthController(
            AuthService authService,
            RegistrationService registrationService,
            JwtProperties jwtProperties,
            @Value("${security.cookie.secure:false}") boolean secureCookies
    ) {
        this.authService = authService;
        this.registrationService = registrationService;
        this.jwtProperties = jwtProperties;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
        return registrationService.register(request);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request, HttpServletResponse response) {
        TokenPair tokens = authService.login(request, UUID.randomUUID());
        setRefreshCookie(response, tokens.refreshToken(), jwtProperties.refreshTokenExpiration());
        return new LoginResponseDTO(tokens.accessToken());
    }

    @PostMapping("/refresh")
    public LoginResponseDTO refresh(
            @CookieValue("refresh_token") String refreshToken,
            HttpServletResponse response
    ) {
        TokenPair tokens = authService.refresh(refreshToken);
        setRefreshCookie(response, tokens.refreshToken(), jwtProperties.refreshTokenExpiration());
        return new LoginResponseDTO(tokens.accessToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue("refresh_token") String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        setRefreshCookie(response, "", Duration.ZERO);
    }

    private void setRefreshCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Strict")
                .path("/auth")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
