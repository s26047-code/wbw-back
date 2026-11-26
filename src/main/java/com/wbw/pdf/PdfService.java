package com.wbw.pdf;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.wbw.common.ApiException;
import com.wbw.common.ErrorCode;
import com.wbw.document.Document;
import com.wbw.document.DocumentService;
import com.wbw.settings.SettingsService;
import com.wbw.settings.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    private final DocumentService documentService;
    private final SettingsService settingsService;
    private final FontCatalog fontCatalog;

    public PdfService(DocumentService documentService, SettingsService settingsService, FontCatalog fontCatalog) {
        this.documentService = documentService;
        this.settingsService = settingsService;
        this.fontCatalog = fontCatalog;
    }

    public record PdfFile(String title, byte[] bytes) {
    }

    @Transactional
    public PdfFile export(Long userId, Long documentId, PdfOptions options) {
        PdfOptions opts = options == null ? PdfOptions.defaults() : options;
        if (!opts.hasUsableArea()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "여백이 너무 커서 본문을 배치할 수 없습니다.");
        }
        Document doc = documentService.getOwned(userId, documentId);
        UserSettings settings = settingsService.get(userId);
        String html = buildHtml(doc, settings, opts);
        return new PdfFile(doc.getTitle(), render(html));
    }

    String buildHtml(Document doc, UserSettings settings, PdfOptions opts) {
        TiptapHtmlRenderer renderer = new TiptapHtmlRenderer(this::cssFontFamily);
        String body = renderer.renderBody(documentService.contentOf(doc));
        String baseFont = Optional.ofNullable(cssFontFamily(settings.getFontFamily()))
                .orElse(cssFontFamily(UserSettings.DEFAULT_FONT));

        StringBuilder html = new StringBuilder(body.length() + 2048);
        html.append("<!DOCTYPE html><html lang=\"ko\"><head><meta charset=\"UTF-8\"/><title>")
                .append(TiptapHtmlRenderer.escapeText(doc.getTitle().isEmpty() ? "제목 없음" : doc.getTitle()))
                .append("</title><style>")
                .append("@page{size:").append(TiptapHtmlRenderer.fmt(opts.pageWidthMm())).append("mm ")
                .append(TiptapHtmlRenderer.fmt(opts.pageHeightMm())).append("mm;margin:")
                .append(TiptapHtmlRenderer.fmt(opts.top())).append("mm ")
                .append(TiptapHtmlRenderer.fmt(opts.right())).append("mm ")
                .append(TiptapHtmlRenderer.fmt(opts.bottom())).append("mm ")
                .append(TiptapHtmlRenderer.fmt(opts.left())).append("mm;}")
                .append("body{margin:0;color:#1F1B16;font-family:").append(baseFont).append(';')
                .append("font-size:").append(settings.getFontSize()).append("px;")
                .append("line-height:").append(TiptapHtmlRenderer.fmt(settings.getLineHeight())).append(';')
                .append("letter-spacing:").append(TiptapHtmlRenderer.fmt(settings.getLetterSpacing())).append("em;")
                .append("word-wrap:break-word;}")
                .append("p,h1,h2,h3{margin:0 0 ").append(TiptapHtmlRenderer.fmt(settings.getParagraphSpacing())).append("em 0;}")
                .append("h1.doc-title{font-size:1.6em;line-height:1.4;margin-bottom:1.2em;}")
                .append("h1{font-size:1.5em;}h2{font-size:1.3em;}h3{font-size:1.15em;}")
                .append("blockquote{margin:0 0 1em 0;padding-left:1em;border-left:2px solid #CFC6B8;color:#4A443C;}")
                .append("pre{white-space:pre-wrap;font-size:0.9em;background:#F4F1EA;padding:0.6em;}")
                .append("hr{border:none;border-top:1px solid #CFC6B8;margin:1.2em 0;}")
                .append("ul,ol{margin:0 0 1em 0;padding-left:1.5em;}")
                .append("div.page-break{page-break-after:always;height:0;margin:0;padding:0;}")
                .append("</style></head><body>");
        if (opts.titleIncluded() && !doc.getTitle().isEmpty()) {
            html.append("<h1 class=\"doc-title\">").append(TiptapHtmlRenderer.escapeText(doc.getTitle())).append("</h1>");
        }
        html.append(body).append("</body></html>");
        return html.toString();
    }

    byte[] render(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            for (FontCatalog.FontFace face : fontCatalog.all()) {
                fontCatalog.regularPath(face).ifPresent(p -> registerFont(builder, p, face.family(), 400));
                fontCatalog.boldPath(face).ifPresent(p -> registerFont(builder, p, face.family(), 700));
            }
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF 생성 실패", e);
            throw new ApiException(ErrorCode.PDF_GENERATION_FAILED);
        }
    }

    private static void registerFont(PdfRendererBuilder builder, Path file, String family, int weight) {
        builder.useFont(file.toFile(), family, weight, BaseRendererBuilder.FontStyle.NORMAL, true);
    }

    /**
     * 글꼴 이름을 CSS font-family 값으로 바꾼다.
     * 선택한 글꼴 뒤에 설치된 다른 한글 글꼴을 대체 글꼴로 붙여, 글자가 빠지지 않게 한다.
     */
    String cssFontFamily(String family) {
        Optional<FontCatalog.FontFace> selected = fontCatalog.find(family);
        if (selected.isEmpty()) {
            return null;
        }
        List<String> families = new ArrayList<>();
        families.add(selected.get().family());
        for (FontCatalog.FontFace face : fontCatalog.all()) {
            if (!face.family().equals(selected.get().family()) && fontCatalog.regularPath(face).isPresent()) {
                families.add(face.family());
            }
        }
        StringBuilder css = new StringBuilder();
        for (String f : families) {
            css.append('\'').append(f).append("',");
        }
        css.append(selected.get().generic());
        return css.toString();
    }
}
