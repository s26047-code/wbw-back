package com.wbw.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    public static final String DEFAULT_FONT = "Noto Serif KR";

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Theme theme;

    @Column(name = "background_color", length = 7)
    private String backgroundColor;

    @Column(name = "text_color", length = 7)
    private String textColor;

    @Column(name = "font_family", nullable = false, length = 50)
    private String fontFamily;

    @Column(name = "font_size", nullable = false)
    private int fontSize;

    @Column(name = "letter_spacing", nullable = false)
    private double letterSpacing;

    @Column(name = "line_height", nullable = false)
    private double lineHeight;

    @Column(name = "paragraph_spacing", nullable = false)
    private double paragraphSpacing;

    @Column(name = "editor_width", nullable = false)
    private int editorWidth;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserSettings() {
    }

    /** 처음 사용하는 사용자에게 적용되는 기본 글쓰기 환경 */
    public static UserSettings defaults(Long userId) {
        UserSettings s = new UserSettings();
        s.userId = userId;
        s.theme = Theme.LIGHT;
        s.backgroundColor = null;
        s.textColor = null;
        s.fontFamily = DEFAULT_FONT;
        s.fontSize = 17;
        s.letterSpacing = 0.0;
        s.lineHeight = 1.8;
        s.paragraphSpacing = 0.8;
        s.editorWidth = 720;
        return s;
    }

    public void update(Theme theme, String backgroundColor, String textColor, String fontFamily, int fontSize,
                       double letterSpacing, double lineHeight, double paragraphSpacing, int editorWidth) {
        this.theme = theme;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.letterSpacing = letterSpacing;
        this.lineHeight = lineHeight;
        this.paragraphSpacing = paragraphSpacing;
        this.editorWidth = editorWidth;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getUserId() { return userId; }
    public Theme getTheme() { return theme; }
    public String getBackgroundColor() { return backgroundColor; }
    public String getTextColor() { return textColor; }
    public String getFontFamily() { return fontFamily; }
    public int getFontSize() { return fontSize; }
    public double getLetterSpacing() { return letterSpacing; }
    public double getLineHeight() { return lineHeight; }
    public double getParagraphSpacing() { return paragraphSpacing; }
    public int getEditorWidth() { return editorWidth; }
    public Instant getUpdatedAt() { return updatedAt; }
}
