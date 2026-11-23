package com.wbw.folder;

import com.wbw.common.ApiException;
import com.wbw.common.ErrorCode;
import com.wbw.document.DocumentRepository;
import com.wbw.share.ShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final ShareRepository shareRepository;

    public FolderService(FolderRepository folderRepository, DocumentRepository documentRepository,
                         ShareRepository shareRepository) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.shareRepository = shareRepository;
    }

    @Transactional(readOnly = true)
    public List<FolderDtos.FolderResponse> list(Long userId) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : folderRepository.countDocumentsByFolder(userId)) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return folderRepository.findByUserIdOrderBySortOrderAscIdAsc(userId).stream()
                .map(f -> toResponse(f, counts.getOrDefault(f.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public FolderDtos.FolderResponse get(Long userId, Long folderId) {
        Folder folder = getOwned(userId, folderId);
        return toResponse(folder, documentRepository.countByFolderId(folderId));
    }

    @Transactional
    public FolderDtos.FolderResponse create(Long userId, FolderDtos.CreateFolderRequest req) {
        String name = req.name().strip();
        validateName(name);
        if (folderRepository.existsByUserIdAndName(userId, name)) {
            throw new ApiException(ErrorCode.FOLDER_NAME_DUPLICATED);
        }
        int order = folderRepository.findMaxSortOrder(userId) + 1;
        Folder saved = folderRepository.saveAndFlush(new Folder(userId, name, order));
        return toResponse(saved, 0);
    }

    @Transactional
    public FolderDtos.FolderResponse update(Long userId, Long folderId, FolderDtos.UpdateFolderRequest req) {
        Folder folder = getOwned(userId, folderId);
        if (req.name() != null) {
            String name = req.name().strip();
            validateName(name);
            if (folderRepository.existsByUserIdAndNameAndIdNot(userId, name, folderId)) {
                throw new ApiException(ErrorCode.FOLDER_NAME_DUPLICATED);
            }
            folder.rename(name);
        }
        if (req.sortOrder() != null) {
            folder.changeSortOrder(req.sortOrder());
        }
        folderRepository.saveAndFlush(folder);
        return toResponse(folder, documentRepository.countByFolderId(folderId));
    }

    /**
     * 폴더와 그 안의 모든 글(및 글의 공유 링크)을 함께 삭제한다.
     * 프론트엔드는 삭제 전 documentCount 로 안내 문구를 보여준다.
     */
    @Transactional
    public FolderDtos.DeleteFolderResponse delete(Long userId, Long folderId) {
        Folder folder = getOwned(userId, folderId);
        shareRepository.deleteByFolderIdBulk(folder.getId());
        int deletedDocs = documentRepository.deleteByFolderIdBulk(folder.getId());
        folderRepository.deleteById(folder.getId());
        return new FolderDtos.DeleteFolderResponse(folderId, deletedDocs);
    }

    /** 사용자가 소유한 폴더를 조회한다. 다른 사용자의 폴더는 존재 여부를 노출하지 않고 404로 처리한다. */
    @Transactional(readOnly = true)
    public Folder getOwned(Long userId, Long folderId) {
        return folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.FOLDER_NOT_FOUND));
    }

    private static void validateName(String name) {
        if (name.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "폴더 이름을 입력해 주세요.");
        }
        if (name.length() > 50) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "폴더 이름은 50자 이하여야 합니다.");
        }
    }

    private static FolderDtos.FolderResponse toResponse(Folder f, long count) {
        return new FolderDtos.FolderResponse(f.getId(), f.getName(), f.getSortOrder(), count,
                f.getCreatedAt(), f.getUpdatedAt());
    }
}
