package com.wbw.share;

import com.wbw.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 공유 비밀번호 무차별 대입 방지.
 * (공유 토큰 + 접속 IP) 기준으로 연속 실패가 일정 횟수를 넘으면 일정 시간 동안 시도를 막는다.
 * 단일 서버 기준 메모리 저장소이며, 서버를 여러 대로 늘리면 Redis 등 공용 저장소로 바꿔야 한다.
 */
@Component
public class PasswordAttemptLimiter {

    private record Attempt(int failures, Instant lockedUntil, Instant lastFailure) {
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock clock;

    @Autowired
    public PasswordAttemptLimiter(AppProperties props) {
        this(props.share().maxPasswordAttempts(), Duration.ofMinutes(props.share().lockMinutes()), Clock.systemUTC());
    }

    PasswordAttemptLimiter(int maxAttempts, Duration lockDuration, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public boolean isLocked(String key) {
        Attempt a = attempts.get(key);
        return a != null && a.lockedUntil() != null && a.lockedUntil().isAfter(clock.instant());
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        attempts.compute(key, (k, prev) -> {
            // 잠금이 끝났거나 마지막 실패 후 잠금 시간만큼 지났으면 횟수를 초기화한다.
            boolean stale = prev == null
                    || (prev.lockedUntil() != null && !prev.lockedUntil().isAfter(now))
                    || prev.lastFailure().plus(lockDuration).isBefore(now);
            int failures = stale ? 1 : prev.failures() + 1;
            Instant lockedUntil = failures >= maxAttempts ? now.plus(lockDuration) : null;
            return new Attempt(failures, lockedUntil, now);
        });
        if (attempts.size() > 10_000) {
            cleanup(now);
        }
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private void cleanup(Instant now) {
        attempts.entrySet().removeIf(e -> e.getValue().lastFailure().plus(lockDuration).isBefore(now)
                && (e.getValue().lockedUntil() == null || e.getValue().lockedUntil().isBefore(now)));
    }
}
