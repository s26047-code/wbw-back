package com.wbw.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@MappedSuperclass
public abstract class BaseTimeEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = now();
    }

    private static Instant now() {
        // DB 컬럼 정밀도(DATETIME(6))에 맞춰 마이크로초 단위로 자른다.
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
