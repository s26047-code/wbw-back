package com.wbw.document;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "글", description = "글 작성·조회·자동 저장")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "글 목록", description = "folderId(폴더별) 또는 unfiled=true(폴더 없음) 필터, q 제목 검색, sort=updatedAt|createdAt|title")
    @GetMapping
    public DocumentDtos.PageResponse<DocumentDtos.DocumentSummary> list(
            @LoginUser Long userId,
            @RequestParam(required = false) Long folderId,
            @RequestParam(defaultValue = "false") boolean unfiled,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(required = false) String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return documentService.list(userId, folderId, unfiled, q, sort, direction, page, size);
    }

    @Operation(summary = "글 조회", description = "본문(TipTap JSON)과 version 포함")
    @GetMapping("/{documentId}")
    public DocumentDtos.DocumentResponse get(@LoginUser Long userId, @PathVariable Long documentId) {
        return documentService.get(userId, documentId);
    }

    @Operation(summary = "새 글", description = "모든 필드 선택. {} 이면 빈 글 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDtos.DocumentResponse create(@LoginUser Long userId,
                                                @Valid @RequestBody DocumentDtos.CreateDocumentRequest request) {
        return documentService.create(userId, request);
    }

    @Operation(summary = "저장 / 자동 저장", description = "version 필수. 서버 버전과 다르면 409 DOCUMENT_VERSION_CONFLICT (덮어쓰지 않음). 응답의 version을 다음 저장에 사용하세요.")
    @PutMapping("/{documentId}")
    public DocumentDtos.SaveResult update(@LoginUser Long userId, @PathVariable Long documentId,
                                          @Valid @RequestBody DocumentDtos.UpdateDocumentRequest request) {
        return documentService.update(userId, documentId, request);
    }

    @Operation(summary = "폴더 이동", description = "folderId=null 이면 폴더 없음. version이 바뀌지 않아 편집 중 자동 저장과 충돌하지 않습니다.")
    @PatchMapping("/{documentId}/folder")
    public DocumentDtos.DocumentResponse move(@LoginUser Long userId, @PathVariable Long documentId,
                                              @RequestBody DocumentDtos.MoveDocumentRequest request) {
        return documentService.move(userId, documentId, request);
    }

    @Operation(summary = "글 삭제", description = "공유 링크도 함께 삭제")
    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginUser Long userId, @PathVariable Long documentId) {
        documentService.delete(userId, documentId);
    }
}
