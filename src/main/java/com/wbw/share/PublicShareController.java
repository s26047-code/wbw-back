package com.wbw.share;

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
public class PublicShareController {

    private final ShareService shareService;

    public PublicShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<ShareDtos.PublicShareResponse> view(@PathVariable String token, HttpServletRequest request) {
        return noStore(shareService.view(token, request.getSession(false)));
    }

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
