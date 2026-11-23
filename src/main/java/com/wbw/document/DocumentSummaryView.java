package com.wbw.document;

import java.time.Instant;

/** 목록 조회용 프로젝션 (본문은 읽지 않는다) */
public interface DocumentSummaryView {
    Long getId();
    String getTitle();
    Long getFolderId();
    String getFolderName();
    String getPreview();
    int getCharsWithSpaces();
    int getCharsWithoutSpaces();
    Instant getCreatedAt();
    Instant getUpdatedAt();
}
