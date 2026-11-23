package com.wbw.folder;

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
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping
    public List<FolderDtos.FolderResponse> list(@LoginUser Long userId) {
        return folderService.list(userId);
    }

    @GetMapping("/{folderId}")
    public FolderDtos.FolderResponse get(@LoginUser Long userId, @PathVariable Long folderId) {
        return folderService.get(userId, folderId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FolderDtos.FolderResponse create(@LoginUser Long userId,
                                            @Valid @RequestBody FolderDtos.CreateFolderRequest request) {
        return folderService.create(userId, request);
    }

    @PatchMapping("/{folderId}")
    public FolderDtos.FolderResponse update(@LoginUser Long userId, @PathVariable Long folderId,
                                            @Valid @RequestBody FolderDtos.UpdateFolderRequest request) {
        return folderService.update(userId, folderId, request);
    }

    @DeleteMapping("/{folderId}")
    public FolderDtos.DeleteFolderResponse delete(@LoginUser Long userId, @PathVariable Long folderId) {
        return folderService.delete(userId, folderId);
    }
}
