package com.wbw.folder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wbw.auth.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@Tag(name = "폴더", description = "폴더 관리")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @Operation(summary = "폴더 목록", description = "폴더별 글 개수(documentCount) 포함")
    @GetMapping
    public List<FolderDtos.FolderResponse> list(@LoginUser Long userId) {
        return folderService.list(userId);
    }

    @Operation(summary = "폴더 조회")
    @GetMapping("/{folderId}")
    public FolderDtos.FolderResponse get(@LoginUser Long userId, @PathVariable Long folderId) {
        return folderService.get(userId, folderId);
    }

    @Operation(summary = "폴더 생성", description = "이름 1~50자, 사용자별로 중복 불가")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FolderDtos.FolderResponse create(@LoginUser Long userId,
                                            @Valid @RequestBody FolderDtos.CreateFolderRequest request) {
        return folderService.create(userId, request);
    }

    @Operation(summary = "폴더 이름·순서 변경", description = "보낸 값만 변경")
    @PatchMapping("/{folderId}")
    public FolderDtos.FolderResponse update(@LoginUser Long userId, @PathVariable Long folderId,
                                            @Valid @RequestBody FolderDtos.UpdateFolderRequest request) {
        return folderService.update(userId, folderId, request);
    }

    @Operation(summary = "폴더 삭제", description = "⚠️ 폴더 안의 글과 그 글의 공유 링크까지 영구 삭제됩니다. 삭제 전 documentCount로 안내 문구를 보여주세요.")
    @DeleteMapping("/{folderId}")
    public FolderDtos.DeleteFolderResponse delete(@LoginUser Long userId, @PathVariable Long folderId) {
        return folderService.delete(userId, folderId);
    }
}
