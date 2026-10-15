package com.lucasnarloch.loss_track.integration;

import com.lucasnarloch.loss_track.domain.auth.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenConcurrencyIT extends AbstractIntegrationIT {
    @Autowired
    private RefreshTokenRepository tokens;

    @Test
    void onlyOneConcurrentRotationSucceedsAndReplayRevokesTheFamily() throws Exception {
        UUID oldToken = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        UUID family = UUID.randomUUID();
        UUID firstReplacement = UUID.randomUUID();
        UUID secondReplacement = UUID.randomUUID();
        Duration expiration = Duration.ofMinutes(5);
        tokens.save(oldToken, user, family, expiration);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> first = () -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return tokens.rotate(oldToken, firstReplacement, user, family, expiration);
            };
            Callable<Boolean> second = () -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return tokens.rotate(oldToken, secondReplacement, user, family, expiration);
            };
            var firstResult = executor.submit(first);
            var secondResult = executor.submit(second);
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            assertThat(List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(redis.hasKey("refresh-token:" + oldToken)).isFalse();
        assertThat(redis.opsForValue().get("refresh-family:" + family + ":revoked")).isEqualTo("revoked");
        assertThat(redis.getExpire("refresh-family:" + family + ":revoked")).isPositive();
        assertThat(tokens.rotate(firstReplacement, UUID.randomUUID(), user, family, expiration)).isFalse();
        assertThat(tokens.rotate(secondReplacement, UUID.randomUUID(), user, family, expiration)).isFalse();
    }
}
