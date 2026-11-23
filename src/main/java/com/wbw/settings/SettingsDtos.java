package com.wbw.settings;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public final class SettingsDtos {

    private SettingsDtos() {
    }

    private static final String HEX = "^#[0-9A-Fa-f]{6}$";

    public record SettingsRequest(
            @NotNull(message = "테마를 선택해 주세요.") Theme theme,
            @Pattern(regexp = HEX, message = "색상은 #RRGGBB 형식이어야 합니다.") String backgroundColor,
            @Pattern(regexp = HEX, message = "색상은 #RRGGBB 형식이어야 합니다.") String textColor,
            @NotBlank(message = "글꼴을 선택해 주세요.") String fontFamily,
            @NotNull @Min(value = 10, message = "글자 크기는 10px 이상이어야 합니다.")
            @Max(value = 40, message = "글자 크기는 40px 이하여야 합니다.") Integer fontSize,
            @NotNull @DecimalMin(value = "-0.1", message = "자간은 -0.1em 이상이어야 합니다.")
            @DecimalMax(value = "0.5", message = "자간은 0.5em 이하여야 합니다.") Double letterSpacing,
            @NotNull @DecimalMin(value = "1.0", message = "줄 간격은 1.0 이상이어야 합니다.")
            @DecimalMax(value = "3.0", message = "줄 간격은 3.0 이하여야 합니다.") Double lineHeight,
            @NotNull @DecimalMin(value = "0.0", message = "문단 간격은 0em 이상이어야 합니다.")
            @DecimalMax(value = "3.0", message = "문단 간격은 3em 이하여야 합니다.") Double paragraphSpacing,
            @NotNull @Min(value = 480, message = "글쓰기 영역 너비는 480px 이상이어야 합니다.")
            @Max(value = 1200, message = "글쓰기 영역 너비는 1200px 이하여야 합니다.") Integer editorWidth
    ) {
        @AssertTrue(message = "사용자 지정 테마는 배경색과 글자색이 모두 필요합니다.")
        public boolean isCustomColorsPresent() {
            return theme != Theme.CUSTOM || (backgroundColor != null && textColor != null);
        }
    }

    public record SettingsResponse(
            Theme theme,
            String backgroundColor,
            String textColor,
            String fontFamily,
            int fontSize,
            double letterSpacing,
            double lineHeight,
            double paragraphSpacing,
            int editorWidth,
            Instant updatedAt
    ) {
        public static SettingsResponse from(UserSettings s) {
            return new SettingsResponse(s.getTheme(), s.getBackgroundColor(), s.getTextColor(), s.getFontFamily(),
                    s.getFontSize(), s.getLetterSpacing(), s.getLineHeight(), s.getParagraphSpacing(),
                    s.getEditorWidth(), s.getUpdatedAt());
        }
    }
}
