package com.wbw.pdf;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wbw.auth.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@Tag(name = "글")
public class PdfController {

    private final PdfService pdfService;

    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @Operation(summary = "PDF 내보내기", description = "모든 옵션 선택. 응답은 application/pdf 파일")
    @PostMapping("/api/documents/{documentId}/pdf")
    public ResponseEntity<byte[]> export(@LoginUser Long userId, @PathVariable Long documentId,
                                         @Valid @RequestBody(required = false) PdfOptions options) {
        PdfService.PdfFile file = pdfService.export(userId, documentId, options);
        String filename = safeFileName(file.title()) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(file.bytes());
    }

    @Operation(summary = "PDF용 HTML 보기 (레이아웃 확인용)",
            description = "PDF를 만들 때 쓰는 HTML/CSS를 그대로 돌려줍니다. 줄 간격·글꼴이 화면과 다를 때 실제 적용된 스타일을 확인하는 용도. 요청 본문은 PDF 내보내기와 같습니다.")
    @PostMapping(value = "/api/documents/{documentId}/pdf/html", produces = "text/html;charset=UTF-8")
    public String previewHtml(@LoginUser Long userId, @PathVariable Long documentId,
                              @Valid @RequestBody(required = false) PdfOptions options) {
        return pdfService.previewHtml(userId, documentId, options);
    }

    static String safeFileName(String title) {
        String name = title == null ? "" : title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").strip();
        if (name.isEmpty()) {
            name = "제목 없음";
        }
        return name.length() > 80 ? name.substring(0, 80) : name;
    }
}
