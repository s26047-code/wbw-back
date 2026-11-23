package com.wbw.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbw.common.ApiException;
import com.wbw.common.ErrorCode;
import com.wbw.config.AppProperties;
import com.wbw.folder.Folder;
import com.wbw.folder.FolderService;
import com.wbw.share.ShareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class DocumentService {

    private static final Set<String> SORT_FIELDS = Set.of("updatedAt", "createdAt", "title");

    private final DocumentRepository documentRepository;
    private final ShareRepository shareRepository;
    private final FolderService folderService;
    private final ObjectMapper objectMapper;
    private final AppProperties props;

    public DocumentService(DocumentRepository documentRepository, ShareRepository shareRepository,
                           FolderService folderService, ObjectMapper objectMapper, AppProperties props) {
        this.documentRepository = documentRepository;
        this.shareRepository = shareRepository;
        this.folderService = folderService;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    /**
     * @param folderId  지정 시 해당 폴더의 글만
     * @param unfiled   true 면 폴더가 없는 글만
     * @param q         제목 검색어
     * @param sort      updatedAt | createdAt | title
     * @param direction asc | desc
     */
    @Transactional(readOnly = true)
    public DocumentDtos.PageResponse<DocumentDtos.DocumentSummary> list(
            Long userId, Long folderId, boolean unfiled, String q, String sort, String direction, int page, int size) {
        if (folderId != null && unfiled) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "folderId 와 unfiled 는 함께 사용할 수 없습니다.");
        }
        if (folderId != null) {
            folderService.getOwned(userId, folderId);
        }
        String sortField = sort == null || sort.isBlank() ? "updatedAt" : sort;
        if (!SORT_FIELDS.contains(sortField)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "정렬 기준은 updatedAt, createdAt, title 중 하나여야 합니다.");
        }
        Sort.Direction dir;
        if (direction == null || direction.isBlank()) {
            dir = "title".equals(sortField) ? Sort.Direction.ASC : Sort.Direction.DESC;
        } else if (direction.equalsIgnoreCase("asc") || direction.equalsIgnoreCase("desc")) {
            dir = Sort.Direction.fromString(direction);
        } else {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "정렬 방향은 asc 또는 desc 여야 합니다.");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "page 는 0 이상, size 는 1~100 이어야 합니다.");
        }

        String filter = folderId != null ? "FOLDER" : (unfiled ? "UNFILED" : "ALL");
        String pattern = toLikePattern(q);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, sortField).and(Sort.by(Sort.Direction.DESC, "id")));
        Page<DocumentSummaryView> result = documentRepository.search(userId, filter, folderId, pattern, pageable);
        return new DocumentDtos.PageResponse<>(
                result.getContent().stream().map(DocumentDtos.DocumentSummary::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public DocumentDtos.DocumentResponse get(Long userId, Long documentId) {
        return toResponse(getOwned(userId, documentId));
    }

    @Transactional
    public DocumentDtos.DocumentResponse create(Long userId, DocumentDtos.CreateDocumentRequest req) {
        Folder folder = req.folderId() == null ? null : folderService.getOwned(userId, req.folderId());
        String title = normalizeTitle(req.title());
        String contentJson;
        String plainText;
        if (req.content() == null || req.content().isNull()) {
            contentJson = TiptapContent.EMPTY_DOC;
            plainText = "";
        } else {
            contentJson = TiptapContent.validateAndSerialize(req.content(), objectMapper, props.document().maxContentBytes());
            plainText = TiptapContent.extractText(req.content());
        }
        Document saved = documentRepository.saveAndFlush(new Document(userId, folder, title, contentJson, plainText));
        return toResponse(saved);
    }

    /**
     * 저장 / 자동 저장.
     * 클라이언트가 보낸 version 이 현재 버전과 다르면 덮어쓰지 않고 409를 반환한다
     * (다른 탭·기기에서 먼저 저장된 경우 데이터 유실 방지).
     */
    @Transactional
    public DocumentDtos.SaveResult update(Long userId, Long documentId, DocumentDtos.UpdateDocumentRequest req) {
        Document doc = getOwned(userId, documentId);
        if (!Objects.equals(doc.getVersion(), req.version())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("currentVersion", doc.getVersion());
            details.put("updatedAt", doc.getUpdatedAt());
            throw new ApiException(ErrorCode.DOCUMENT_VERSION_CONFLICT, details);
        }
        if (req.title() != null) {
            String title = normalizeTitle(req.title());
            if (!title.equals(doc.getTitle())) {
                doc.changeTitle(title);
            }
        }
        if (req.content() != null) {
            String json = TiptapContent.validateAndSerialize(req.content(), objectMapper, props.document().maxContentBytes());
            if (!json.equals(doc.getContent())) {
                doc.changeContent(json, TiptapContent.extractText(req.content()));
            }
        }
        Document saved = documentRepository.saveAndFlush(doc);
        return new DocumentDtos.SaveResult(saved.getId(), saved.getTitle(), saved.getCharsWithSpaces(),
                saved.getCharsWithoutSpaces(), saved.getVersion(), saved.getUpdatedAt());
    }

    @Transactional
    public DocumentDtos.DocumentResponse move(Long userId, Long documentId, DocumentDtos.MoveDocumentRequest req) {
        getOwned(userId, documentId);
        Folder folder = req.folderId() == null ? null : folderService.getOwned(userId, req.folderId());
        documentRepository.moveToFolder(documentId, userId, folder);
        return toResponse(getOwned(userId, documentId));
    }

    @Transactional
    public void delete(Long userId, Long documentId) {
        Document doc = getOwned(userId, documentId);
        shareRepository.deleteByDocumentIdBulk(doc.getId());
        documentRepository.deleteById(doc.getId());
    }

    /** 사용자가 소유한 글. 다른 사용자의 글은 존재 여부를 노출하지 않고 404로 처리한다. */
    @Transactional(readOnly = true)
    public Document getOwned(Long userId, Long documentId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    public JsonNode contentOf(Document doc) {
        return TiptapContent.parse(doc.getContent(), objectMapper);
    }

    private DocumentDtos.DocumentResponse toResponse(Document d) {
        Folder f = d.getFolder();
        return new DocumentDtos.DocumentResponse(d.getId(), d.getTitle(),
                f == null ? null : f.getId(), f == null ? null : f.getName(),
                contentOf(d), d.getCharsWithSpaces(), d.getCharsWithoutSpaces(), d.getVersion(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        String t = title.strip().replaceAll("[\\r\\n\\t]+", " ");
        if (t.length() > props.document().maxTitleLength()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "제목은 " + props.document().maxTitleLength() + "자 이하여야 합니다.");
        }
        return t;
    }

    private static String toLikePattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String escaped = q.strip().toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
