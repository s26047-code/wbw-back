package com.wbw.pdf;

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
public class PdfController {

    private final PdfService pdfService;

    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

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

    static String safeFileName(String title) {
        String name = title == null ? "" : title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").strip();
        if (name.isEmpty()) {
            name = "제목 없음";
        }
        return name.length() > 80 ? name.substring(0, 80) : name;
    }
}
