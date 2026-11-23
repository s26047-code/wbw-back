package com.wbw.pdf;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * PDF 내보내기 옵션. 모든 값은 선택이며 생략 시 기본값(A4 세로, 상하 25mm·좌우 20mm, 제목 포함)을 사용한다.
 * 여백 단위는 mm.
 */
public record PdfOptions(
        PageSize pageSize,
        Orientation orientation,
        @DecimalMin(value = "0", message = "여백은 0mm 이상이어야 합니다.")
        @DecimalMax(value = "60", message = "여백은 60mm 이하여야 합니다.") Double marginTop,
        @DecimalMin(value = "0", message = "여백은 0mm 이상이어야 합니다.")
        @DecimalMax(value = "60", message = "여백은 60mm 이하여야 합니다.") Double marginRight,
        @DecimalMin(value = "0", message = "여백은 0mm 이상이어야 합니다.")
        @DecimalMax(value = "60", message = "여백은 60mm 이하여야 합니다.") Double marginBottom,
        @DecimalMin(value = "0", message = "여백은 0mm 이상이어야 합니다.")
        @DecimalMax(value = "60", message = "여백은 60mm 이하여야 합니다.") Double marginLeft,
        Boolean includeTitle
) {
    public enum PageSize {
        A4(210, 297), A5(148, 210), B5(176, 250), LETTER(215.9, 279.4);

        public final double widthMm;
        public final double heightMm;

        PageSize(double widthMm, double heightMm) {
            this.widthMm = widthMm;
            this.heightMm = heightMm;
        }
    }

    public enum Orientation { PORTRAIT, LANDSCAPE }

    public static PdfOptions defaults() {
        return new PdfOptions(null, null, null, null, null, null, null);
    }

    public PageSize pageSizeOrDefault() { return pageSize == null ? PageSize.A4 : pageSize; }
    public Orientation orientationOrDefault() { return orientation == null ? Orientation.PORTRAIT : orientation; }
    public double top() { return marginTop == null ? 25 : marginTop; }
    public double right() { return marginRight == null ? 20 : marginRight; }
    public double bottom() { return marginBottom == null ? 25 : marginBottom; }
    public double left() { return marginLeft == null ? 20 : marginLeft; }
    public boolean titleIncluded() { return includeTitle == null || includeTitle; }

    public double pageWidthMm() {
        PageSize s = pageSizeOrDefault();
        return orientationOrDefault() == Orientation.PORTRAIT ? s.widthMm : s.heightMm;
    }

    public double pageHeightMm() {
        PageSize s = pageSizeOrDefault();
        return orientationOrDefault() == Orientation.PORTRAIT ? s.heightMm : s.widthMm;
    }

    /** 여백이 페이지를 다 차지하지 않는지 검사 (본문 영역 최소 40mm) */
    public boolean hasUsableArea() {
        return pageWidthMm() - left() - right() >= 40 && pageHeightMm() - top() - bottom() >= 40;
    }
}
