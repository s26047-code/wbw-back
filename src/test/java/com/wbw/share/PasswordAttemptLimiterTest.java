package com.wbw.share;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordAttemptLimiterTest {

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void locksAfterMaxFailuresAndUnlocksAfterDuration() {
        MutableClock clock = new MutableClock();
        PasswordAttemptLimiter limiter = new PasswordAttemptLimiter(3, Duration.ofMinutes(10), clock);

        limiter.recordFailure("k");
        limiter.recordFailure("k");
        assertThat(limiter.isLocked("k")).isFalse();
        limiter.recordFailure("k");
        assertThat(limiter.isLocked("k")).isTrue();
        assertThat(limiter.isLocked("other")).isFalse();

        clock.now = clock.now.plus(Duration.ofMinutes(11));
        assertThat(limiter.isLocked("k")).isFalse();
        limiter.recordFailure("k");
        assertThat(limiter.isLocked("k")).isFalse(); // 잠금 해제 후 횟수 초기화
    }

    @Test
    void successResetsFailures() {
        MutableClock clock = new MutableClock();
        PasswordAttemptLimiter limiter = new PasswordAttemptLimiter(2, Duration.ofMinutes(10), clock);
        limiter.recordFailure("k");
        limiter.recordSuccess("k");
        limiter.recordFailure("k");
        assertThat(limiter.isLocked("k")).isFalse();
    }
}
