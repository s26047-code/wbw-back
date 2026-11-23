package com.wbw.document;

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
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

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

    @GetMapping("/{documentId}")
    public DocumentDtos.DocumentResponse get(@LoginUser Long userId, @PathVariable Long documentId) {
        return documentService.get(userId, documentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDtos.DocumentResponse create(@LoginUser Long userId,
                                                @Valid @RequestBody DocumentDtos.CreateDocumentRequest request) {
        return documentService.create(userId, request);
    }

    @PutMapping("/{documentId}")
    public DocumentDtos.SaveResult update(@LoginUser Long userId, @PathVariable Long documentId,
                                          @Valid @RequestBody DocumentDtos.UpdateDocumentRequest request) {
        return documentService.update(userId, documentId, request);
    }

    @PatchMapping("/{documentId}/folder")
    public DocumentDtos.DocumentResponse move(@LoginUser Long userId, @PathVariable Long documentId,
                                              @RequestBody DocumentDtos.MoveDocumentRequest request) {
        return documentService.move(userId, documentId, request);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginUser Long userId, @PathVariable Long documentId) {
        documentService.delete(userId, documentId);
    }
}
