package com.wbw.share;

import com.wbw.auth.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping("/api/documents/{documentId}/shares")
    public List<ShareDtos.ShareResponse> list(@LoginUser Long userId, @PathVariable Long documentId) {
        return shareService.list(userId, documentId);
    }

    @PostMapping("/api/documents/{documentId}/shares")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareDtos.ShareResponse create(@LoginUser Long userId, @PathVariable Long documentId,
                                          @Valid @RequestBody ShareDtos.CreateShareRequest request) {
        return shareService.create(userId, documentId, request);
    }

    @PatchMapping("/api/shares/{shareId}")
    public ShareDtos.ShareResponse update(@LoginUser Long userId, @PathVariable Long shareId,
                                          @Valid @RequestBody ShareDtos.UpdateShareRequest request) {
        return shareService.update(userId, shareId, request);
    }

    @DeleteMapping("/api/shares/{shareId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginUser Long userId, @PathVariable Long shareId) {
        shareService.delete(userId, shareId);
    }
}
