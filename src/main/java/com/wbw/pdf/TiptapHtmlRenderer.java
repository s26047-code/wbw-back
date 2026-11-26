package com.wbw.pdf;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TipTap JSON → XHTML 변환기 (PDF 생성용).
 * <p>
 * 허용된 노드/마크/속성만 출력하고 모든 텍스트와 속성 값을 이스케이프/검증한다.
 * 알 수 없는 노드는 태그 없이 자식만 출력한다.
 * <p>
 * 지원 서식 (프론트엔드 TipTap 속성 이름과 일치해야 함):
 * <ul>
 *   <li>노드: paragraph, heading(level 1~3), blockquote, bulletList, orderedList, listItem, codeBlock, horizontalRule, hardBreak</li>
 *   <li>문단 속성: textAlign(left|center|right|justify), lineHeight(1.6 배수 / "160%" / "28px"),
 *       paragraphSpacing(0.8 = em, 문단 아래 간격 / "12px"),
 *       pageBreak(true면 이 문단 다음에서 새 쪽)</li>
 *   <li>마크: bold, italic, underline, strike, textStyle(fontFamily, fontSize, letterSpacing, color)</li>
 * </ul>
 */
public class TiptapHtmlRenderer {

    private static final Pattern NUMBER_UNIT = Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*(px|pt|em|rem)?\\s*$");
    private static final Pattern HEX_COLOR = Pattern.compile("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

    private final Function<String, String> fontFamilyResolver;

    /**
     * @param fontFamilyResolver 글꼴 이름 → CSS font-family 값(대체 글꼴 포함). 지원하지 않는 글꼴이면 null.
     */
    public TiptapHtmlRenderer(Function<String, String> fontFamilyResolver) {
        this.fontFamilyResolver = fontFamilyResolver;
    }

    public String renderBody(JsonNode doc) {
        StringBuilder sb = new StringBuilder();
        renderChildren(doc, sb);
        return sb.toString();
    }

    private void renderChildren(JsonNode node, StringBuilder sb) {
        JsonNode children = node.get("content");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                render(child, sb);
            }
        }
    }

    private void render(JsonNode node, StringBuilder sb) {
        if (node == null || !node.isObject()) {
            return;
        }
        String type = node.path("type").asText("");
        JsonNode attrs = node.path("attrs");
        switch (type) {
            case "text" -> renderText(node, sb);
            case "hardBreak" -> sb.append("<br/>");
            case "paragraph" -> {
                if (isPageBreak(attrs) && !hasContent(node)) {
                    // 내용 없는 쪽 나눔 문단: 빈 줄을 남기지 않고 다음 쪽으로 넘긴다
                    sb.append("<div class=\"page-break\"></div>");
                } else {
                    block("p", paragraphStyle(attrs), node, sb, true);
                }
            }
            case "heading" -> {
                int level = Math.max(1, Math.min(3, attrs.path("level").asInt(1)));
                block("h" + level, paragraphStyle(attrs), node, sb, true);
            }
            case "blockquote" -> block("blockquote", "", node, sb, false);
            case "bulletList" -> block("ul", "", node, sb, false);
            case "orderedList" -> {
                int start = Math.max(1, attrs.path("start").asInt(1));
                sb.append("<ol start=\"").append(start).append("\">");
                renderChildren(node, sb);
                sb.append("</ol>");
            }
            case "listItem" -> block("li", "", node, sb, false);
            case "codeBlock" -> block("pre", "", node, sb, false);
            case "horizontalRule" -> sb.append("<hr/>");
            default -> renderChildren(node, sb);
        }
    }

    private void block(String tag, String style, JsonNode node, StringBuilder sb, boolean keepEmptyLine) {
        sb.append('<').append(tag);
        if (!style.isEmpty()) {
            sb.append(" style=\"").append(escapeAttr(style)).append('"');
        }
        sb.append('>');
        int before = sb.length();
        renderChildren(node, sb);
        if (keepEmptyLine && sb.length() == before) {
            sb.append("&#160;"); // 빈 문단도 한 줄 높이를 유지
        }
        sb.append("</").append(tag).append('>');
    }

    private String paragraphStyle(JsonNode attrs) {
        StringBuilder style = new StringBuilder();
        String align = attrs.path("textAlign").asText("");
        if (align.equals("left") || align.equals("center") || align.equals("right") || align.equals("justify")) {
            style.append("text-align:").append(align).append(';');
        }
        String lineHeight = lineHeightCss(attrs.get("lineHeight"));
        if (lineHeight != null) {
            style.append("line-height:").append(lineHeight).append(';');
        }
        String spacing = paragraphSpacingCss(attrs.get("paragraphSpacing"));
        if (spacing != null) {
            style.append("margin-bottom:").append(spacing).append(';');
        }
        if (isPageBreak(attrs)) {
            style.append("page-break-after:always;");
        }
        return style.toString();
    }

    /**
     * 줄 간격: 1.6 / "1.6" (배수, 권장) · "160%" (→ 1.6) · "28px" · "1.6em" 을 받는다.
     * 범위를 벗어나거나 해석할 수 없으면 null (사용자 설정 기본값 적용).
     */
    static String lineHeightCss(JsonNode value) {
        Length len = parseLength(value);
        if (len == null) {
            return null;
        }
        return switch (len.unit()) {
            case "", "em" -> len.value() >= 0.8 && len.value() <= 4.0 ? fmt(len.value()) : null;
            case "%" -> len.value() >= 80 && len.value() <= 400 ? fmt(len.value() / 100.0) : null;
            case "px" -> len.value() >= 6 && len.value() <= 200 ? fmt(len.value()) + "px" : null;
            case "pt" -> len.value() >= 5 && len.value() <= 150 ? fmt(len.value()) + "pt" : null;
            default -> null;
        };
    }

    /**
     * 문단 간격(문단 아래 여백): 0.8 (em, 권장) · "0.8em" · "12px" · "9pt" 를 받는다.
     * 단위 없는 숫자는 5 이하면 em, 5보다 크면 px 로 해석한다.
     */
    static String paragraphSpacingCss(JsonNode value) {
        Length len = parseLength(value);
        if (len == null) {
            return null;
        }
        String unit = len.unit();
        if (unit.isEmpty()) {
            unit = len.value() <= 5 ? "em" : "px";
        }
        return switch (unit) {
            case "em" -> len.value() >= 0 && len.value() <= 5 ? fmt(len.value()) + "em" : null;
            case "px" -> len.value() >= 0 && len.value() <= 200 ? fmt(len.value()) + "px" : null;
            case "pt" -> len.value() >= 0 && len.value() <= 150 ? fmt(len.value()) + "pt" : null;
            default -> null;
        };
    }

    record Length(double value, String unit) {
    }

    private static final Pattern LENGTH = Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*(px|pt|em|rem|%)?\\s*$");

    static Length parseLength(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return new Length(value.asDouble(), "");
        }
        Matcher m = LENGTH.matcher(value.asText("").toLowerCase(Locale.ROOT));
        if (!m.matches()) {
            return null;
        }
        String unit = m.group(2) == null ? "" : m.group(2);
        return new Length(Double.parseDouble(m.group(1)), unit.equals("rem") ? "em" : unit);
    }

    /** attrs.pageBreak 가 true(또는 "true")이면 이 문단 다음에서 새 쪽으로 넘긴다. */
    static boolean isPageBreak(JsonNode attrs) {
        JsonNode v = attrs == null ? null : attrs.get("pageBreak");
        return v != null && (v.asBoolean(false) || "true".equalsIgnoreCase(v.asText()));
    }

    private static boolean hasContent(JsonNode node) {
        JsonNode children = node.get("content");
        return children != null && children.isArray() && !children.isEmpty();
    }

    private void renderText(JsonNode node, StringBuilder sb) {
        String text = node.path("text").asText("");
        if (text.isEmpty()) {
            return;
        }
        StringBuilder open = new StringBuilder();
        StringBuilder close = new StringBuilder();
        JsonNode marks = node.get("marks");
        if (marks != null && marks.isArray()) {
            for (JsonNode mark : marks) {
                String markType = mark.path("type").asText("");
                String tag = switch (markType) {
                    case "bold" -> "strong";
                    case "italic" -> "em";
                    case "underline" -> "u";
                    case "strike" -> "s";
                    default -> null;
                };
                if (tag != null) {
                    open.append('<').append(tag).append('>');
                    close.insert(0, "</" + tag + ">");
                } else if (markType.equals("textStyle")) {
                    String style = textStyle(mark.path("attrs"));
                    if (!style.isEmpty()) {
                        open.append("<span style=\"").append(escapeAttr(style)).append("\">");
                        close.insert(0, "</span>");
                    }
                }
            }
        }
        sb.append(open).append(escapeText(text)).append(close);
    }

    private String textStyle(JsonNode attrs) {
        StringBuilder style = new StringBuilder();
        String family = attrs.path("fontFamily").asText(null);
        if (family != null) {
            String css = fontFamilyResolver.apply(family);
            if (css != null) {
                style.append("font-family:").append(css).append(';');
            }
        }
        String fontSize = cssLength(attrs.get("fontSize"), "px", 6, 96, 6, 72, 0.3, 6);
        if (fontSize != null) {
            style.append("font-size:").append(fontSize).append(';');
        }
        String spacing = cssLength(attrs.get("letterSpacing"), "em", -10, 30, -8, 24, -0.5, 1.5);
        if (spacing != null) {
            style.append("letter-spacing:").append(spacing).append(';');
        }
        String color = attrs.path("color").asText("");
        if (HEX_COLOR.matcher(color).matches()) {
            style.append("color:").append(color).append(';');
        }
        return style.toString();
    }

    /**
     * "17px", "12pt", "0.05em", 17 등의 값을 검증하여 CSS 길이로 만든다. 단위가 없으면 defaultUnit.
     * 단위별 허용 범위를 벗어나면 null.
     */
    static String cssLength(JsonNode value, String defaultUnit,
                            double pxMin, double pxMax, double ptMin, double ptMax, double emMin, double emMax) {
        if (value == null || value.isNull()) {
            return null;
        }
        String raw = value.isNumber() ? value.asText() : value.asText("");
        Matcher m = NUMBER_UNIT.matcher(raw.toLowerCase(Locale.ROOT));
        if (!m.matches()) {
            return null;
        }
        double n = Double.parseDouble(m.group(1));
        String unit = m.group(2) == null ? defaultUnit : m.group(2);
        if (unit.equals("rem")) {
            unit = "em";
        }
        boolean ok = switch (unit) {
            case "px" -> n >= pxMin && n <= pxMax;
            case "pt" -> n >= ptMin && n <= ptMax;
            case "em" -> n >= emMin && n <= emMax;
            default -> false;
        };
        return ok ? fmt(n) + unit : null;
    }

    static Double number(JsonNode value, double min, double max) {
        if (value == null || value.isNull()) {
            return null;
        }
        double n;
        if (value.isNumber()) {
            n = value.asDouble();
        } else {
            Matcher m = NUMBER_UNIT.matcher(value.asText(""));
            if (!m.matches() || (m.group(2) != null && !m.group(2).equals("em"))) {
                return null;
            }
            n = Double.parseDouble(m.group(1));
        }
        return n >= min && n <= max ? n : null;
    }

    static String fmt(double n) {
        if (n == Math.rint(n)) {
            return String.valueOf((long) n);
        }
        return String.format(Locale.ROOT, "%.3f", n).replaceAll("0+$", "");
    }

    public static String escapeText(String s) {
        StringBuilder out = new StringBuilder(s.length());
        s.codePoints().forEach(cp -> {
            switch (cp) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '\t' -> out.append("&#160;&#160;&#160;&#160;");
                case '\n' -> out.append("<br/>");
                default -> {
                    // XML 1.0에서 허용되지 않는 제어 문자 제거
                    boolean validXml = cp >= 0x20 && !(cp >= 0xD800 && cp <= 0xDFFF) && cp != 0xFFFE && cp != 0xFFFF;
                    if (validXml) {
                        out.appendCodePoint(cp);
                    }
                }
            }
        });
        return out.toString();
    }

    public static String escapeAttr(String s) {
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
