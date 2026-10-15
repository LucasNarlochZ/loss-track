package com.lucasnarloch.loss_track.infra.config;

import com.lucasnarloch.loss_track.domain.auth.TokenType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
    public static final String ISSUER = "loss-track";

    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] secret = Base64.getDecoder().decode(properties.secret());
        if (secret.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 256 bits");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey).build();
    }

    @Bean("accessJwtDecoder")
    JwtDecoder accessJwtDecoder(SecretKey secretKey) {
        return decoder(secretKey, TokenType.ACCESS);
    }

    @Bean("refreshJwtDecoder")
    JwtDecoder refreshJwtDecoder(SecretKey secretKey) {
        return decoder(secretKey, TokenType.REFRESH);
    }

    private JwtDecoder decoder(SecretKey secretKey, TokenType tokenType) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(ISSUER);
        OAuth2TokenValidator<Jwt> type = new JwtClaimValidator<>(
                "token_type",
                value -> tokenType.value().equals(value)
        );
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, type));
        return decoder;
    }
}
