package com.lucasnarloch.loss_track.infra.redis;

import com.lucasnarloch.loss_track.domain.auth.RefreshTokenRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Repository
public class RedisRefreshTokenRepository implements RefreshTokenRepository {
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[3]) == 1 then
                return 0
            end
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then
                redis.call('SET', KEYS[3], 'revoked', 'PX', ARGV[2])
                return 0
            end
            redis.call('DEL', KEYS[1])
            redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisRefreshTokenRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(UUID tokenId, UUID userId, UUID familyId, Duration expiration) {
        redis.opsForValue().set(tokenKey(tokenId), value(userId, familyId), expiration);
    }

    @Override
    public boolean rotate(UUID oldTokenId, UUID newTokenId, UUID userId, UUID familyId, Duration expiration) {
        Long result = redis.execute(
                ROTATE_SCRIPT,
                List.of(tokenKey(oldTokenId), tokenKey(newTokenId), familyKey(familyId)),
                value(userId, familyId),
                Long.toString(expiration.toMillis())
        );
        return Long.valueOf(1).equals(result);
    }

    @Override
    public void revokeFamily(UUID familyId, Duration expiration) {
        redis.opsForValue().set(familyKey(familyId), "revoked", expiration);
    }

    private String tokenKey(UUID tokenId) {
        return "refresh-token:" + tokenId;
    }

    private String familyKey(UUID familyId) {
        return "refresh-family:" + familyId + ":revoked";
    }

    private String value(UUID userId, UUID familyId) {
        return userId + ":" + familyId;
    }
}
