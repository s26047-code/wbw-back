package com.wbw.pdf;

import com.wbw.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 서비스에서 제공하는 글꼴 목록.
 * <p>
 * family 값은 프론트엔드 CSS font-family 및 TipTap textStyle.fontFamily 에 그대로 사용한다.
 * PDF 생성에는 app.pdf.font-dir 경로의 TTF 파일이 필요하다 (모두 SIL OFL 라이선스, README 참고).
 */
@Component
public class FontCatalog {

    private static final Logger log = LoggerFactory.getLogger(FontCatalog.class);

    public record FontFace(String family, String label, String generic, String regularFile, String boldFile) {
    }

    public record FontInfo(String family, String label, String generic, boolean pdfAvailable) {
    }

    private static final List<FontFace> FONTS = List.of(
            new FontFace("Noto Serif KR", "본명조 (Noto Serif KR)", "serif", "NotoSerifKR-Regular.ttf", "NotoSerifKR-Bold.ttf"),
            new FontFace("Noto Sans KR", "본고딕 (Noto Sans KR)", "sans-serif", "NotoSansKR-Regular.ttf", "NotoSansKR-Bold.ttf"),
            new FontFace("Nanum Myeongjo", "나눔명조", "serif", "NanumMyeongjo-Regular.ttf", "NanumMyeongjo-Bold.ttf"),
            new FontFace("Nanum Gothic", "나눔고딕", "sans-serif", "NanumGothic-Regular.ttf", "NanumGothic-Bold.ttf"),
            new FontFace("Gowun Batang", "고운바탕", "serif", "GowunBatang-Regular.ttf", "GowunBatang-Bold.ttf"),
            new FontFace("Gowun Dodum", "고운돋움", "sans-serif", "GowunDodum-Regular.ttf", null)
    );

    private final Path fontDir;

    public FontCatalog(AppProperties props) {
        String dir = props.pdf() != null && props.pdf().fontDir() != null ? props.pdf().fontDir() : "./fonts";
        this.fontDir = Path.of(dir).toAbsolutePath().normalize();
        long available = FONTS.stream().filter(f -> regularPath(f).isPresent()).count();
        if (available == 0) {
            log.warn("PDF용 한글 글꼴(TTF)이 {} 에 없습니다. PDF의 한글이 표시되지 않을 수 있습니다. README의 '글꼴 설치'를 참고하세요.", fontDir);
        } else {
            log.info("PDF 글꼴 {}/{}개 사용 가능 ({})", available, FONTS.size(), fontDir);
        }
    }

    public List<FontInfo> list() {
        return FONTS.stream()
                .map(f -> new FontInfo(f.family(), f.label(), f.generic(), regularPath(f).isPresent()))
                .toList();
    }

    public boolean isKnown(String family) {
        return find(family).isPresent();
    }

    public Optional<FontFace> find(String family) {
        if (family == null) {
            return Optional.empty();
        }
        String normalized = family.replace("\"", "").replace("'", "").trim();
        return FONTS.stream().filter(f -> f.family().equalsIgnoreCase(normalized)).findFirst();
    }

    public List<FontFace> all() {
        return FONTS;
    }

    public Optional<Path> regularPath(FontFace face) {
        return resolve(face.regularFile());
    }

    public Optional<Path> boldPath(FontFace face) {
        return face.boldFile() == null ? Optional.empty() : resolve(face.boldFile());
    }

    private Optional<Path> resolve(String fileName) {
        Path p = fontDir.resolve(fileName);
        return Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
    }
}
