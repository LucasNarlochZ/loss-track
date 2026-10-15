package com.lucasnarloch.loss_track.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationIT extends AbstractIntegrationIT {
    @Autowired
    private PasswordEncoder passwords;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    @Qualifier("accessJwtDecoder")
    private JwtDecoder accessDecoder;

    @Autowired
    @Qualifier("refreshJwtDecoder")
    private JwtDecoder refreshDecoder;

    @Test
    void registersTenantAndOwnerWithHashedPassword() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("tenantName", "Shop", "cnpj", CNPJ,
                                "ownerName", "Owner", "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner.role").value("OWNER"))
                .andExpect(jsonPath("$.owner.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.owner.password").doesNotExist());

        var owner = users.findByTenantCnpjAndEmail(CNPJ, EMAIL).orElseThrow();
        assertThat(owner.getPasswordHash()).startsWith("$argon2").isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, owner.getPasswordHash())).isTrue();
        assertThat(jdbc.sql("SELECT count(*) FROM tenants").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateTenantRegistrationWithoutCreatingAnotherUser() throws Exception {
        register(CNPJ);
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("tenantName", "Duplicate", "cnpj", CNPJ,
                                "ownerName", "Other owner", "email", "other@example.com", "password", PASSWORD))))
                .andExpect(status().isConflict());
        assertThat(users.count()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM tenants").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void loginIssuesTenantScopedAccessTokenAndSecureRefreshCookie() throws Exception {
        var owner = register(CNPJ);
        var result = login(CNPJ, EMAIL, PASSWORD);
        var token = accessDecoder.decode(body(result).path("accessToken").asString());
        assertThat(token.getSubject()).isEqualTo(owner.getId().toString());
        assertThat(token.getClaimAsString("tenant_id")).isEqualTo(owner.getTenant().getId().toString());
        assertThat(token.getClaimAsString("role")).isEqualTo("OWNER");
        assertThat(token.getClaimAsString("token_type")).isEqualTo("access");
        assertThat(body(result).has("refreshToken")).isFalse();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/auth");
        var refresh = refreshDecoder.decode(refreshCookie(result).getValue());
        assertThat(redis.opsForValue().get("refresh-token:" + refresh.getId()))
                .isEqualTo(owner.getId() + ":" + refresh.getClaimAsString("family_id"));
        assertThat(redis.getExpire("refresh-token:" + refresh.getId())).isPositive();
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-password", "wrong-email", "wrong-tenant"})
    void rejectsInvalidCredentials(String scenario) throws Exception {
        register(CNPJ);
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "cnpj", scenario.equals("wrong-tenant") ? OTHER_CNPJ : CNPJ,
                                "email", scenario.equals("wrong-email") ? "missing@example.com" : EMAIL,
                                "password", scenario.equals("wrong-password") ? "wrong" : PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "INACTIVE", "SUSPENDED"})
    void rejectsInactiveAccountsAtLoginAndRefresh(String account) throws Exception {
        var owner = register(CNPJ);
        var cookie = refreshCookie(login(CNPJ, EMAIL, PASSWORD));
        if (account.equals("user")) {
            jdbc.sql("UPDATE users SET status = 'INACTIVE' WHERE id = :id").param("id", owner.getId()).update();
        } else {
            jdbc.sql("UPDATE tenants SET status = :status WHERE id = :id")
                    .param("status", account).param("id", owner.getTenant().getId()).update();
        }
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("cnpj", CNPJ, "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/auth/refresh").cookie(cookie)).andExpect(status().isForbidden());
    }

    @Test
    void rotatesRefreshTokenAndRevokesFamilyOnReplay() throws Exception {
        register(CNPJ);
        var oldCookie = refreshCookie(login(CNPJ, EMAIL, PASSWORD));
        var oldClaims = refreshDecoder.decode(oldCookie.getValue());
        var rotation = mockMvc.perform(post("/auth/refresh").cookie(oldCookie))
                .andExpect(status().isOk()).andReturn();
        var newCookie = refreshCookie(rotation);
        var newClaims = refreshDecoder.decode(newCookie.getValue());
        assertThat(newClaims.getId()).isNotEqualTo(oldClaims.getId());
        assertThat(newClaims.getClaimAsString("family_id")).isEqualTo(oldClaims.getClaimAsString("family_id"));
        assertThat(redis.hasKey("refresh-token:" + oldClaims.getId())).isFalse();
        assertThat(redis.hasKey("refresh-token:" + newClaims.getId())).isTrue();
        mockMvc.perform(get("/products").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + body(rotation).path("accessToken").asString())).andExpect(status().isOk());
        mockMvc.perform(post("/auth/refresh").cookie(oldCookie)).andExpect(status().isUnauthorized());
        assertThat(redis.hasKey("refresh-family:" + oldClaims.getClaimAsString("family_id") + ":revoked")).isTrue();
        mockMvc.perform(post("/auth/refresh").cookie(newCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesOnlyItsSessionFamilyAndClearsCookie() throws Exception {
        register(CNPJ);
        var first = refreshCookie(login(CNPJ, EMAIL, PASSWORD));
        var second = refreshCookie(login(CNPJ, EMAIL, PASSWORD));
        mockMvc.perform(post("/auth/logout").cookie(first))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
        mockMvc.perform(post("/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/refresh").cookie(second)).andExpect(status().isOk());
    }

    @Test
    void rejectsMalformedTokensAndTokenTypeConfusion() throws Exception {
        register(CNPJ);
        var result = login(CNPJ, EMAIL, PASSWORD);
        var access = body(result).path("accessToken").asString();
        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", access)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", "invalid")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/products").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + refreshCookie(result).getValue())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/products").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    private Cookie refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "TRIAL"})
    void allowsActiveAndTrialTenants(String tenantStatus) throws Exception {
        var owner = register(CNPJ);
        jdbc.sql("UPDATE tenants SET status = :status WHERE id = :id")
                .param("status", tenantStatus).param("id", owner.getTenant().getId()).update();
        var cookie = refreshCookie(login(CNPJ, EMAIL, PASSWORD));
        mockMvc.perform(post("/auth/refresh").cookie(cookie)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "wrong-issuer", "tampered"})
    void rejectsExpiredWrongIssuerAndTamperedAccessTokens(String scenario) throws Exception {
        var owner = register(CNPJ);
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(scenario.equals("wrong-issuer") ? "untrusted" : "loss-track")
                .issuedAt(now.minusSeconds(600))
                .expiresAt(scenario.equals("expired") ? now.minusSeconds(120) : now.plusSeconds(300))
                .subject(owner.getId().toString())
                .claim("tenant_id", owner.getTenant().getId().toString())
                .claim("role", "OWNER")
                .claim("token_type", "access")
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        if (scenario.equals("tampered")) {
            int signature = token.lastIndexOf('.') + 1;
            char replacement = token.charAt(signature) == 'A' ? 'B' : 'A';
            token = token.substring(0, signature) + replacement + token.substring(signature + 1);
        }
        mockMvc.perform(get("/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
