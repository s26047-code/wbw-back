package com.wbw.share;

import com.wbw.common.ApiException;
import com.wbw.common.ErrorCode;
import com.wbw.config.AppProperties;
import com.wbw.document.Document;
import com.wbw.document.DocumentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShareService {

    static final String SESSION_VERIFIED_SHARES = "WBW_VERIFIED_SHARES";

    private final ShareRepository shareRepository;
    private final DocumentService documentService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordAttemptLimiter attemptLimiter;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock = Clock.systemUTC();

    public ShareService(ShareRepository shareRepository, DocumentService documentService,
                        PasswordEncoder passwordEncoder, PasswordAttemptLimiter attemptLimiter, AppProperties props) {
        this.shareRepository = shareRepository;
        this.documentService = documentService;
        this.passwordEncoder = passwordEncoder;
        this.attemptLimiter = attemptLimiter;
        this.props = props;
    }

    // ---------- 작성자용 ----------

    @Transactional
    public ShareDtos.ShareResponse create(Long userId, Long documentId, ShareDtos.CreateShareRequest req) {
        Document doc = documentService.getOwned(userId, documentId);
        validateFuture(req.expiresAt());
        String hash = req.password() == null ? null : passwordEncoder.encode(req.password());
        Share share = shareRepository.saveAndFlush(new Share(doc, newToken(), hash, req.expiresAt()));
        return toResponse(share);
    }

    @Transactional(readOnly = true)
    public List<ShareDtos.ShareResponse> list(Long userId, Long documentId) {
        documentService.getOwned(userId, documentId);
        return shareRepository.findByDocumentIdOrderByCreatedAtDesc(documentId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShareDtos.ShareResponse update(Long userId, Long shareId, ShareDtos.UpdateShareRequest req) {
        Share share = getOwned(userId, shareId);
        if (Boolean.TRUE.equals(req.removePassword()) && req.password() != null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "password 와 removePassword 는 함께 사용할 수 없습니다.");
        }
        if (Boolean.TRUE.equals(req.clearExpiresAt()) && req.expiresAt() != null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "expiresAt 과 clearExpiresAt 은 함께 사용할 수 없습니다.");
        }
        if (req.enabled() != null) {
            share.changeEnabled(req.enabled());
        }
        if (Boolean.TRUE.equals(req.removePassword())) {
            share.changePasswordHash(null);
        } else if (req.password() != null) {
            share.changePasswordHash(passwordEncoder.encode(req.password()));
        }
        if (Boolean.TRUE.equals(req.clearExpiresAt())) {
            share.changeExpiresAt(null);
        } else if (req.expiresAt() != null) {
            validateFuture(req.expiresAt());
            share.changeExpiresAt(req.expiresAt());
        }
        return toResponse(shareRepository.saveAndFlush(share));
    }

    @Transactional
    public void delete(Long userId, Long shareId) {
        shareRepository.delete(getOwned(userId, shareId));
    }

    // ---------- 공개 열람 ----------

    @Transactional(readOnly = true)
    public ShareDtos.PublicShareResponse view(String token, HttpSession session) {
        Share share = findAccessible(token);
        if (share.isPasswordProtected() && !isVerified(share, session)) {
            throw new ApiException(ErrorCode.SHARE_PASSWORD_REQUIRED);
        }
        return toPublic(share);
    }

    /** 비밀번호가 맞으면 세션에 인증 사실을 기록하고 글을 반환한다. */
    @Transactional(readOnly = true)
    public ShareDtos.PublicShareResponse verify(String token, String password, HttpSession session, String clientIp) {
        Share share = findAccessible(token);
        if (!share.isPasswordProtected()) {
            return toPublic(share);
        }
        String key = share.getId() + "|" + clientIp;
        if (attemptLimiter.isLocked(key)) {
            throw new ApiException(ErrorCode.SHARE_TOO_MANY_ATTEMPTS);
        }
        if (!passwordEncoder.matches(password, share.getPasswordHash())) {
            attemptLimiter.recordFailure(key);
            if (attemptLimiter.isLocked(key)) {
                throw new ApiException(ErrorCode.SHARE_TOO_MANY_ATTEMPTS);
            }
            throw new ApiException(ErrorCode.SHARE_PASSWORD_MISMATCH);
        }
        attemptLimiter.recordSuccess(key);
        markVerified(share, session);
        return toPublic(share);
    }

    private Share findAccessible(String token) {
        if (token == null || token.length() > 64) {
            throw new ApiException(ErrorCode.SHARE_NOT_FOUND);
        }
        Share share = shareRepository.findByToken(token)
                .filter(Share::isEnabled)
                .orElseThrow(() -> new ApiException(ErrorCode.SHARE_NOT_FOUND));
        if (share.isExpired(clock.instant())) {
            throw new ApiException(ErrorCode.SHARE_EXPIRED);
        }
        return share;
    }

    /** 비밀번호가 바뀌면 기존 인증이 무효가 되도록 해시 값까지 비교한다. */
    @SuppressWarnings("unchecked")
    private boolean isVerified(Share share, HttpSession session) {
        if (session == null) {
            return false;
        }
        Object attr = session.getAttribute(SESSION_VERIFIED_SHARES);
        if (!(attr instanceof Map<?, ?> map)) {
            return false;
        }
        return share.getPasswordHash().equals(((Map<Long, String>) map).get(share.getId()));
    }

    @SuppressWarnings("unchecked")
    private void markVerified(Share share, HttpSession session) {
        Object attr = session.getAttribute(SESSION_VERIFIED_SHARES);
        HashMap<Long, String> map = attr instanceof Map<?, ?> m ? new HashMap<>((Map<Long, String>) m) : new HashMap<>();
        map.put(share.getId(), share.getPasswordHash());
        session.setAttribute(SESSION_VERIFIED_SHARES, map);
    }

    private ShareDtos.PublicShareResponse toPublic(Share share) {
        Document doc = share.getDocument();
        return new ShareDtos.PublicShareResponse(doc.getTitle(), documentService.contentOf(doc),
                doc.getCharsWithSpaces(), doc.getCharsWithoutSpaces(), doc.getUpdatedAt());
    }

    private Share getOwned(Long userId, Long shareId) {
        return shareRepository.findByIdAndOwner(shareId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.SHARE_NOT_FOUND));
    }

    private void validateFuture(Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(clock.instant())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "만료 시각은 현재보다 이후여야 합니다.");
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ShareDtos.ShareResponse toResponse(Share s) {
        return new ShareDtos.ShareResponse(s.getId(), s.getDocument().getId(), s.getToken(),
                props.frontendBaseUrl() + "/s/" + s.getToken(), s.isPasswordProtected(), s.isEnabled(),
                s.getExpiresAt(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
