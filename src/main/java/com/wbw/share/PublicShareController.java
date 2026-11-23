package com.wbw.share;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 없이 접근하는 공유 글 열람 API */
@RestController
@RequestMapping("/api/public/shares")
@Tag(name = "공유 (열람)", description = "로그인 없이 공유 글 열람")
public class PublicShareController {

    private final ShareService shareService;

    public PublicShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @Operation(summary = "공유 글 열람", description = "비밀번호 공유이고 인증 전이면 401 SHARE_PASSWORD_REQUIRED")
    @GetMapping("/{token}")
    public ResponseEntity<ShareDtos.PublicShareResponse> view(@PathVariable String token, HttpServletRequest request) {
        return noStore(shareService.view(token, request.getSession(false)));
    }

    @Operation(summary = "공유 비밀번호 확인", description = "성공 시 글 내용 반환, 같은 세션에서는 다시 묻지 않음. 5회 실패 시 10분 잠금(429)")
    @PostMapping("/{token}/verify")
    public ResponseEntity<ShareDtos.PublicShareResponse> verify(@PathVariable String token,
                                                                @Valid @RequestBody ShareDtos.VerifyPasswordRequest body,
                                                                HttpServletRequest request) {
        return noStore(shareService.verify(token, body.password(), request.getSession(true), request.getRemoteAddr()));
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(body);
    }
}
