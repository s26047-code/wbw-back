package com.wbw.document;

import com.wbw.common.BaseTimeEntity;
import com.wbw.folder.Folder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "documents")
public class Document extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Column(nullable = false, length = 200)
    private String title;

    /** TipTap JSON (서식 포함 원본) */
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    /** 검색·글자 수 계산용 순수 텍스트 */
    @Column(name = "plain_text", nullable = false, columnDefinition = "LONGTEXT")
    private String plainText;

    @Column(nullable = false, length = 200)
    private String preview;

    @Column(name = "chars_with_spaces", nullable = false)
    private int charsWithSpaces;

    @Column(name = "chars_without_spaces", nullable = false)
    private int charsWithoutSpaces;

    /** 자동 저장 충돌 감지용 낙관적 락 버전 */
    @Version
    @Column(nullable = false)
    private Long version;

    protected Document() {
    }

    public Document(Long userId, Folder folder, String title, String content, String plainText) {
        this.userId = userId;
        this.folder = folder;
        this.title = title;
        changeContent(content, plainText);
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeContent(String content, String plainText) {
        this.content = content;
        this.plainText = plainText;
        this.preview = TiptapContent.preview(plainText);
        CharacterCounter.Counts counts = CharacterCounter.count(plainText);
        this.charsWithSpaces = counts.withSpaces();
        this.charsWithoutSpaces = counts.withoutSpaces();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Folder getFolder() { return folder; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getPlainText() { return plainText; }
    public String getPreview() { return preview; }
    public int getCharsWithSpaces() { return charsWithSpaces; }
    public int getCharsWithoutSpaces() { return charsWithoutSpaces; }
    public Long getVersion() { return version; }
}
