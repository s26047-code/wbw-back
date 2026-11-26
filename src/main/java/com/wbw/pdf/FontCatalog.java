package com.wbw.pdf;

import com.wbw.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 서비스에서 제공하는 글꼴 목록.
 * <p>
 * family 값은 프론트엔드 CSS font-family 및 TipTap textStyle.fontFamily 에 그대로 사용한다.
 * <p>
 * PDF에는 고정 굵기(static) TTF 파일을 직접 임베드한다. (WOFF2·가변 글꼴·CFF 기반 OTF/TTC는 PDF 엔진에서 한글이 깨질 수 있어 사용하지 않음)
 * 글꼴 파일은 아래 순서로 찾는다.
 * <ol>
 *   <li>app.pdf.font-dir (기본 ./fonts) — Google Fonts에서 받은 파일</li>
 *   <li>/usr/share/fonts/truetype/nanum — 리눅스 서버의 fonts-nanum 패키지 (sudo apt install fonts-nanum)</li>
 *   <li>Windows 글꼴 폴더 — 로컬 개발 PC에 나눔 글꼴이 설치된 경우</li>
 * </ol>
 */
@Component
public class FontCatalog {

    private static final Logger log = LoggerFactory.getLogger(FontCatalog.class);

    /** regularFiles / boldFiles: 같은 글꼴의 여러 배포판 파일명 후보 (앞쪽 우선) */
    public record FontFace(String family, String label, String generic, List<String> regularFiles, List<String> boldFiles) {
    }

    public record FontInfo(String family, String label, String generic, boolean pdfAvailable) {
    }

    private static final List<FontFace> FONTS = List.of(
            new FontFace("Noto Serif KR", "본명조 (Noto Serif KR)", "serif",
                    List.of("NotoSerifKR-Regular.ttf"), List.of("NotoSerifKR-Bold.ttf")),
            new FontFace("Noto Sans KR", "본고딕 (Noto Sans KR)", "sans-serif",
                    List.of("NotoSansKR-Regular.ttf"), List.of("NotoSansKR-Bold.ttf")),
            new FontFace("Nanum Myeongjo", "나눔명조", "serif",
                    List.of("NanumMyeongjo-Regular.ttf", "NanumMyeongjo.ttf"),
                    List.of("NanumMyeongjo-Bold.ttf", "NanumMyeongjoBold.ttf")),
            new FontFace("Nanum Gothic", "나눔고딕", "sans-serif",
                    List.of("NanumGothic-Regular.ttf", "NanumGothic.ttf"),
                    List.of("NanumGothic-Bold.ttf", "NanumGothicBold.ttf")),
            new FontFace("Gowun Batang", "고운바탕", "serif",
                    List.of("GowunBatang-Regular.ttf"), List.of("GowunBatang-Bold.ttf")),
            new FontFace("Gowun Dodum", "고운돋움", "sans-serif",
                    List.of("GowunDodum-Regular.ttf"), List.of())
    );

    private final List<Path> searchDirs;

    public FontCatalog(AppProperties props) {
        String dir = props.pdf() != null && props.pdf().fontDir() != null ? props.pdf().fontDir() : "./fonts";
        List<Path> dirs = new ArrayList<>();
        dirs.add(Path.of(dir).toAbsolutePath().normalize());
        dirs.add(Path.of("/usr/share/fonts/truetype/nanum"));
        String windir = System.getenv("WINDIR");
        if (windir != null) {
            dirs.add(Path.of(windir, "Fonts"));
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                dirs.add(Path.of(localAppData, "Microsoft", "Windows", "Fonts"));
            }
        }
        this.searchDirs = List.copyOf(dirs);

        List<String> available = FONTS.stream().filter(f -> regularPath(f).isPresent()).map(FontFace::family).toList();
        if (available.isEmpty()) {
            log.warn("PDF용 한글 글꼴(TTF)을 찾지 못했습니다. PDF의 한글이 표시되지 않습니다. "
                    + "서버라면 'sudo apt install fonts-nanum', 또는 {} 에 TTF를 넣으세요.", searchDirs.get(0));
        } else {
            log.info("PDF 글꼴 {}/{}개 사용 가능: {}", available.size(), FONTS.size(), available);
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
        return resolve(face.regularFiles());
    }

    public Optional<Path> boldPath(FontFace face) {
        return resolve(face.boldFiles());
    }

    private Optional<Path> resolve(List<String> candidates) {
        for (Path dir : searchDirs) {
            for (String name : candidates) {
                Path p = dir.resolve(name);
                if (Files.isRegularFile(p)) {
                    return Optional.of(p);
                }
            }
        }
        return Optional.empty();
    }
}
