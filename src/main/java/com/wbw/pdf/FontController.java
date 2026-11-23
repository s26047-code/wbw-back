package com.wbw.pdf;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FontController {

    private final FontCatalog fontCatalog;

    public FontController(FontCatalog fontCatalog) {
        this.fontCatalog = fontCatalog;
    }

    @GetMapping("/api/fonts")
    public List<FontCatalog.FontInfo> fonts() {
        return fontCatalog.list();
    }
}
