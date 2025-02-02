package org.example.gateway.support;

import java.util.Collections;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private static final int REQUEST_LIMIT_PER_MINUTE = 100;

    private final RedisScript<Long> incrRequestCountScript;
    private final RedisTemplate<String, Object> redisTemplate;

    public long checkRequestCount(String key, Long currentTime, Long currentTimeWithoutSeconds) {
        Long requestCount = redisTemplate.execute(incrRequestCountScript,
            Collections.singletonList(key), String.valueOf(REQUEST_LIMIT_PER_MINUTE),
            String.valueOf(currentTime), String.valueOf(currentTimeWithoutSeconds));

        if (Objects.isNull(requestCount)) {
            // redis 연결 문제
            log.error("redis 조회 실패");
            throw new RuntimeException("redis 조회 실패");
        }

        if (requestCount.intValue() == 0) {
            // zadd 실패
            log.error("ZADD 실패");
            throw new RuntimeException("ZADD 실패");
        }

        return requestCount;
    }
}
