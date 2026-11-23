package com.wbw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        Cors cors,
        Document document,
        Share share,
        Pdf pdf
) {
    public record Cors(List<String> allowedOrigins) {
    }

    public record Document(int maxContentBytes, int maxTitleLength) {
    }

    public record Share(int maxPasswordAttempts, int lockMinutes) {
    }

    public record Pdf(String fontDir) {
    }

    /** 끝의 슬래시를 제거한 프론트엔드 주소 */
    public String frontendBaseUrl() {
        return frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }
}
