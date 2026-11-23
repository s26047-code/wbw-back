package com.wbw.pdf;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "개인 설정")
public class FontController {

    private final FontCatalog fontCatalog;

    public FontController(FontCatalog fontCatalog) {
        this.fontCatalog = fontCatalog;
    }

    @Operation(summary = "사용 가능한 글꼴 목록", description = "family 값을 CSS font-family와 TipTap textStyle.fontFamily에 그대로 사용하세요. 로그인 불필요.")
    @GetMapping("/api/fonts")
    public List<FontCatalog.FontInfo> fonts() {
        return fontCatalog.list();
    }
}
