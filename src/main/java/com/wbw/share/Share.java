package com.wbw.share;

import com.wbw.common.BaseTimeEntity;
import com.wbw.document.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "shares")
public class Share extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    /** null 이면 공개 공유 */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected Share() {
    }

    public Share(Document document, String token, String passwordHash, Instant expiresAt) {
        this.document = document;
        this.token = token;
        this.passwordHash = passwordHash;
        this.expiresAt = expiresAt;
        this.enabled = true;
    }

    public boolean isPasswordProtected() {
        return passwordHash != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void changeExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public String getToken() { return token; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
    public Instant getExpiresAt() { return expiresAt; }
}
